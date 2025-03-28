package me.ryanhamshire.GPFlags.gui;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class FlagGUI implements Listener {
    private static FlagGUI instance;
    private final GPFlags plugin;
    private final NamespacedKey MENU_TYPE_KEY;
    private final NamespacedKey FLAG_NAME_KEY;
    private final NamespacedKey CLAIM_ID_KEY;
    private final NamespacedKey FLAG_ENABLED_KEY;
    private final NamespacedKey PAGE_KEY;
    
    private final Map<UUID, PlayerFlagSession> activeSessions = new HashMap<>();
    
    // Menu types
    public static final String MENU_MAIN = "main";
    public static final String MENU_FLAGS = "flags";
    public static final String MENU_PARAMETERS = "parameters";
    
    public FlagGUI(GPFlags plugin) {
        this.plugin = plugin;
        MENU_TYPE_KEY = new NamespacedKey(plugin, "menu_type");
        FLAG_NAME_KEY = new NamespacedKey(plugin, "flag_name");
        CLAIM_ID_KEY = new NamespacedKey(plugin, "claim_id");
        FLAG_ENABLED_KEY = new NamespacedKey(plugin, "flag_enabled");
        PAGE_KEY = new NamespacedKey(plugin, "page");
        
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public static FlagGUI getInstance() {
        return instance;
    }
    
    /**
     * Open the main menu for a player
     * @param player The player to open the menu for
     */
    public void openMainMenu(Player player) {
        // Use a title that works with Minecraft's color codes
        Inventory inventory = Bukkit.createInventory(null, 27, "§2GPFlags Menu");
        
        // Fill with black glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        // Option to manage claim flags where the player is standing - set in a position that is clearly visible
        ItemStack standingClaimItem = createItem(
                Material.GRASS_BLOCK, 
                "§aManage Current Claim Flags", 
                Arrays.asList("§7Manage flags for the claim", "§7where you are currently standing")
        );
        inventory.setItem(11, standingClaimItem);
        
        // Option to list all owned claims - place in a more visible location
        ItemStack ownedClaimsItem = createItem(
                Material.GOLDEN_SHOVEL, 
                "§eManage Your Claims", 
                Arrays.asList("§7View and manage flags for", "§7all your claims")
        );
        inventory.setItem(15, ownedClaimsItem);
        
        // Add header item
        ItemStack headerItem = createItem(
                Material.OAK_SIGN,
                "§6§lGPFlags Menu",
                Arrays.asList("§7Select an option below")
        );
        inventory.setItem(4, headerItem);
        
        // Add metadata to inventory items
        ItemMeta meta = standingClaimItem.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(MENU_TYPE_KEY, PersistentDataType.STRING, MENU_MAIN);
        standingClaimItem.setItemMeta(meta);
        
        meta = ownedClaimsItem.getItemMeta();
        data = meta.getPersistentDataContainer();
        data.set(MENU_TYPE_KEY, PersistentDataType.STRING, MENU_MAIN);
        ownedClaimsItem.setItemMeta(meta);
        
        player.openInventory(inventory);
    }
    
    /**
     * Open the flags menu for a specific claim
     * @param player The player viewing the menu
     * @param claim The claim to display flags for
     * @param page The page number (starting from 0)
     */
    public void openClaimFlagsMenu(Player player, Claim claim, int page) {
        if (claim == null) {
            MessagingUtil.sendMessage(player, "Claim not found. Stand in a claim to manage flags.");
            return;
        }
        
        // Store the session data
        PlayerFlagSession session = new PlayerFlagSession(player.getUniqueId(), claim);
        activeSessions.put(player.getUniqueId(), session);
        
        Inventory inventory = Bukkit.createInventory(null, 54, "§2Flags for Claim: " + getClaimName(claim));
        
        // Fill with black glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        FlagManager flagManager = GPFlags.getInstance().getFlagManager();
        Collection<FlagDefinition> flagDefinitions = flagManager.getFlagDefinitions();
        List<FlagDefinition> sortedDefinitions = new ArrayList<>(flagDefinitions);
        sortedDefinitions.sort(Comparator.comparing(FlagDefinition::getName));
        
        // Calculate pagination
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, sortedDefinitions.size());
        
        if (startIndex >= sortedDefinitions.size()) {
            page = 0;
            startIndex = 0;
            endIndex = Math.min(45, sortedDefinitions.size());
        }
        
        // Add flag items
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            FlagDefinition def = sortedDefinitions.get(i);
            
            // Check if player has permission for this flag
            if (!player.hasPermission("gpflags.flag." + def.getName().toLowerCase()) && 
                !player.hasPermission("gpflags.admin")) {
                continue;
            }
            
            // Check if the flag is already set
            boolean isSet = false;
            Flag flag = flagManager.getRawClaimFlag(claim, def.getName().toLowerCase());
            if (flag != null && flag.getSet()) {
                isSet = true;
            }
            
            // Create flag item
            Material material = isSet ? Material.LIME_WOOL : Material.RED_WOOL;
            String status = isSet ? "§aEnabled" : "§cDisabled";
            
            ItemStack flagItem = createItem(
                    material,
                    "§b" + def.getName(),
                    Arrays.asList(
                            "§7Status: " + status,
                            "",
                            "§7" + def.getDescription(),
                            "",
                            "§e" + "Click to " + (isSet ? "disable" : "configure")
                    )
            );
            
            // Add metadata
            ItemMeta meta = flagItem.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(MENU_TYPE_KEY, PersistentDataType.STRING, MENU_FLAGS);
            data.set(FLAG_NAME_KEY, PersistentDataType.STRING, def.getName());
            data.set(CLAIM_ID_KEY, PersistentDataType.STRING, claim.getID().toString());
            data.set(FLAG_ENABLED_KEY, PersistentDataType.INTEGER, isSet ? 1 : 0);
            flagItem.setItemMeta(meta);
            
            // Use inventory positions 10-16, 19-25, 28-34, 37-43 (4 rows of 7 slots each)
            int row = slot / 7;
            int col = slot % 7;
            int invSlot = 10 + col + (row * 9);
            
            if (invSlot < 44) {
                inventory.setItem(invSlot, flagItem);
                slot++;
            }
        }
        
        // Add page title
        ItemStack titleItem = createItem(
                Material.OAK_SIGN,
                "§6§lClaim Flags",
                Arrays.asList("§7Page " + (page + 1))
        );
        inventory.setItem(4, titleItem);
        
        // Add navigation buttons
        if (page > 0) {
            ItemStack prevPage = createItem(Material.ARROW, "§ePrevious Page", null);
            ItemMeta meta = prevPage.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(MENU_TYPE_KEY, PersistentDataType.STRING, MENU_FLAGS);
            data.set(PAGE_KEY, PersistentDataType.INTEGER, page - 1);
            data.set(CLAIM_ID_KEY, PersistentDataType.STRING, claim.getID().toString());
            prevPage.setItemMeta(meta);
            inventory.setItem(45, prevPage);
        }
        
        if (endIndex < sortedDefinitions.size()) {
            ItemStack nextPage = createItem(Material.ARROW, "§eNext Page", null);
            ItemMeta meta = nextPage.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(MENU_TYPE_KEY, PersistentDataType.STRING, MENU_FLAGS);
            data.set(PAGE_KEY, PersistentDataType.INTEGER, page + 1);
            data.set(CLAIM_ID_KEY, PersistentDataType.STRING, claim.getID().toString());
            nextPage.setItemMeta(meta);
            inventory.setItem(53, nextPage);
        }
        
        // Back button
        ItemStack backButton = createItem(Material.BARRIER, "§cBack", null);
        ItemMeta backMeta = backButton.getItemMeta();
        PersistentDataContainer backData = backMeta.getPersistentDataContainer();
        backData.set(MENU_TYPE_KEY, PersistentDataType.STRING, "back");
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);
        
        player.openInventory(inventory);
    }
    
    /**
     * Open the parameter input menu for a flag
     * @param player The player
     * @param claim The claim
     * @param flagDef The flag definition
     */
    public void openParameterMenu(Player player, Claim claim, FlagDefinition flagDef) {
        // Create a simple menu for simple flags that don't need parameters
        Inventory inventory = Bukkit.createInventory(null, 27, "§2Configure Flag: " + flagDef.getName());
        
        // Fill with black glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        // Add info about the flag
        ItemStack infoItem = createItem(
                Material.BOOK, 
                "§b" + flagDef.getName(), 
                Arrays.asList(
                        "§7" + flagDef.getDescription(),
                        "",
                        "§7Parameter: " + (flagDef.requiresParameters() ? "Required" : "Not Required"),
                        "",
                        flagDef.requiresParameters() ? 
                                "§eType the parameters in chat after clicking the button below" : 
                                "§eClick the button below to enable this flag"
                )
        );
        inventory.setItem(4, infoItem);
        
        // Add enable button
        ItemStack enableButton = createItem(
                Material.LIME_CONCRETE, 
                "§aEnable", 
                flagDef.requiresParameters() ? 
                        Arrays.asList("§eClick to enter parameters") : 
                        Arrays.asList("§eClick to enable without parameters")
        );
        ItemMeta meta = enableButton.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(MENU_TYPE_KEY, PersistentDataType.STRING, MENU_PARAMETERS);
        data.set(FLAG_NAME_KEY, PersistentDataType.STRING, flagDef.getName());
        data.set(CLAIM_ID_KEY, PersistentDataType.STRING, claim.getID().toString());
        enableButton.setItemMeta(meta);
        inventory.setItem(13, enableButton);
        
        // Back button
        ItemStack backButton = createItem(Material.BARRIER, "§cBack", null);
        ItemMeta backMeta = backButton.getItemMeta();
        PersistentDataContainer backData = backMeta.getPersistentDataContainer();
        backData.set(MENU_TYPE_KEY, PersistentDataType.STRING, "back");
        backButton.setItemMeta(backMeta);
        inventory.setItem(22, backButton);
        
        player.openInventory(inventory);
    }
    
    /**
     * Create a new ItemStack with the given material, name, and lore
     * @param material The material
     * @param name The name
     * @param lore The lore (can be null)
     * @return The created ItemStack
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get a user-friendly name for a claim
     * @param claim The claim
     * @return A user-friendly name
     */
    private String getClaimName(Claim claim) {
        if (claim.getOwnerName() == null || claim.getOwnerName().isEmpty()) {
            return "Administrative Claim";
        } else {
            return claim.getOwnerName() + "'s Claim";
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer data = meta.getPersistentDataContainer();
        
        // Check if this is one of our menus
        if (!data.has(MENU_TYPE_KEY, PersistentDataType.STRING)) {
            return;
        }
        
        event.setCancelled(true);
        
        String menuType = data.get(MENU_TYPE_KEY, PersistentDataType.STRING);
        
        switch (menuType) {
            case MENU_MAIN:
                handleMainMenuClick(player, clickedItem, event.getSlot());
                break;
            case MENU_FLAGS:
                handleFlagsMenuClick(player, data);
                break;
            case MENU_PARAMETERS:
                handleParameterMenuClick(player, data);
                break;
            case "back":
                player.closeInventory();
                if (activeSessions.containsKey(player.getUniqueId())) {
                    PlayerFlagSession session = activeSessions.get(player.getUniqueId());
                    openClaimFlagsMenu(player, session.getClaim(), 0);
                } else {
                    openMainMenu(player);
                }
                break;
        }
    }
    
    private void handleMainMenuClick(Player player, ItemStack clickedItem, int slot) {
        switch (slot) {
            case 11: // Current claim
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
                if (claim == null) {
                    player.closeInventory();
                    MessagingUtil.sendMessage(player, "You are not standing in a claim. Go to a claim to manage flags.");
                    return;
                }
                
                // Check permission
                if (!claim.getOwnerID().equals(player.getUniqueId()) && 
                        !player.hasPermission("gpflags.admin")) {
                    player.closeInventory();
                    MessagingUtil.sendMessage(player, "You don't have permission to manage flags in this claim.");
                    return;
                }
                
                openClaimFlagsMenu(player, claim, 0);
                break;
            case 15: // All owned claims
                List<Claim> claims = new ArrayList<>();
                for (Claim c : GriefPrevention.instance.dataStore.getClaims()) {
                    if (c.getOwnerID().equals(player.getUniqueId())) {
                        claims.add(c);
                    }
                }
                
                if (claims.isEmpty()) {
                    player.closeInventory();
                    MessagingUtil.sendMessage(player, "You don't have any claims.");
                    return;
                }
                
                // For now, just open the first claim
                // In a more complex implementation, you could create a menu to select from multiple claims
                openClaimFlagsMenu(player, claims.get(0), 0);
                break;
        }
    }
    
    private void handleFlagsMenuClick(Player player, PersistentDataContainer data) {
        if (data.has(PAGE_KEY, PersistentDataType.INTEGER)) {
            int page = data.get(PAGE_KEY, PersistentDataType.INTEGER);
            String claimIdStr = data.get(CLAIM_ID_KEY, PersistentDataType.STRING);
            Claim claim = GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimIdStr));
            
            openClaimFlagsMenu(player, claim, page);
            return;
        }
        
        String flagName = data.get(FLAG_NAME_KEY, PersistentDataType.STRING);
        String claimIdStr = data.get(CLAIM_ID_KEY, PersistentDataType.STRING);
        int isEnabled = data.get(FLAG_ENABLED_KEY, PersistentDataType.INTEGER);
        
        Claim claim = GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimIdStr));
        FlagManager flagManager = GPFlags.getInstance().getFlagManager();
        FlagDefinition flagDef = flagManager.getFlagDefinitionByName(flagName);
        
        if (isEnabled == 1) {
            // Disable the flag
            SetFlagResult result = flagManager.unSetFlag(claim, flagDef);
            player.closeInventory();
            
            if (result.success) {
                MessagingUtil.sendMessage(player, "Flag " + flagName + " disabled for this claim.");
            } else {
                MessagingUtil.sendMessage(player, "Error disabling flag: " + result.getMessage().getMessageID());
            }
            
            // Reopen the menu after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> openClaimFlagsMenu(player, claim, 0), 2L);
        } else {
            // Open parameter menu for the flag
            openParameterMenu(player, claim, flagDef);
        }
    }
    
    private void handleParameterMenuClick(Player player, PersistentDataContainer data) {
        String flagName = data.get(FLAG_NAME_KEY, PersistentDataType.STRING);
        String claimIdStr = data.get(CLAIM_ID_KEY, PersistentDataType.STRING);
        
        Claim claim = GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimIdStr));
        FlagManager flagManager = GPFlags.getInstance().getFlagManager();
        FlagDefinition flagDef = flagManager.getFlagDefinitionByName(flagName);
        
        player.closeInventory();
        
        if (flagDef.requiresParameters()) {
            // Start a conversation for parameter input
            PlayerFlagSession session = new PlayerFlagSession(
                    player.getUniqueId(), 
                    claim,
                    flagDef
            );
            activeSessions.put(player.getUniqueId(), session);
            
            MessagingUtil.sendMessage(player, "Type the parameters for the flag " + flagName + " in chat (type 'cancel' to cancel):");
        } else {
            // Set the flag without parameters
            SetFlagResult result = flagManager.setFlag(claim.getID().toString(), flagDef, true, player);
            
            if (result.success) {
                MessagingUtil.sendMessage(player, "Flag " + flagName + " enabled for this claim.");
            } else {
                MessagingUtil.sendMessage(player, "Error enabling flag: " + result.getMessage().getMessageID());
            }
            
            // Reopen the menu after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> openClaimFlagsMenu(player, claim, 0), 2L);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // If player closes parameter menu, they cancel the operation
        if (activeSessions.containsKey(player.getUniqueId())) {
            PlayerFlagSession session = activeSessions.get(player.getUniqueId());
            if (session.isAwaitingInput()) {
                activeSessions.remove(player.getUniqueId());
            }
        }
    }
    
    /**
     * Handle chat input for flag parameters
     * @param player The player
     * @param message The message they typed
     * @return True if the message was handled, false otherwise
     */
    public boolean handleChatInput(Player player, String message) {
        PlayerFlagSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isAwaitingInput()) {
            return false;
        }
        
        activeSessions.remove(player.getUniqueId());
        
        if (message.equalsIgnoreCase("cancel")) {
            MessagingUtil.sendMessage(player, "Flag setting cancelled.");
            openClaimFlagsMenu(player, session.getClaim(), 0);
            return true;
        }
        
        FlagManager flagManager = GPFlags.getInstance().getFlagManager();
        SetFlagResult result = flagManager.setFlag(
                session.getClaim().getID().toString(), 
                session.getFlagDefinition(), 
                true, 
                player, 
                message.split(" ")
        );
        
        if (result.success) {
            MessagingUtil.sendMessage(player, "Flag " + session.getFlagDefinition().getName() + " enabled for this claim.");
        } else {
            MessagingUtil.sendMessage(player, "Error enabling flag: " + result.getMessage().getMessageID());
        }
        
        // Reopen the menu after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> openClaimFlagsMenu(player, session.getClaim(), 0), 2L);
        return true;
    }
    
    /**
     * Class to track player flag sessions when they're setting up flags
     */
    private static class PlayerFlagSession {
        private final UUID playerId;
        private final Claim claim;
        private FlagDefinition flagDefinition;
        
        public PlayerFlagSession(UUID playerId, Claim claim) {
            this.playerId = playerId;
            this.claim = claim;
        }
        
        public PlayerFlagSession(UUID playerId, Claim claim, FlagDefinition flagDefinition) {
            this.playerId = playerId;
            this.claim = claim;
            this.flagDefinition = flagDefinition;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public Claim getClaim() {
            return claim;
        }
        
        public FlagDefinition getFlagDefinition() {
            return flagDefinition;
        }
        
        public boolean isAwaitingInput() {
            return flagDefinition != null;
        }
    }
} 