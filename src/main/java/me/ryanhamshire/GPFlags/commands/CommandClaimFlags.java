package me.ryanhamshire.GPFlags.commands;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.flags.FlagDescriptions;
import me.ryanhamshire.GPFlags.flags.FlagParameterType;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Native dialog editor. Mutations delegate to the commands so their validation stays authoritative. */
public class CommandClaimFlags implements TabExecutor {
    private static final int PAGE_SIZE = 12;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gpflags.command.setclaimflag")) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }
        if (!(sender instanceof Player)) {
            MessagingUtil.sendMessage(sender, TextMode.Warn, Messages.PlayerOnlyCommand, command.toString());
            return true;
        }
        Player player = (Player) sender;
        Claim claim = currentClaim(player);
        if (claim == null) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.StandInAClaim);
        } else if (claim.getID() == null || claim.getID() == -1) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.UpdateGPForSubdivisionFlags);
        } else {
            menu(player, claim.getID(), 0, "");
        }
        return true;
    }

    private static Claim currentClaim(Player player) {
        return GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false,
                GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).lastClaim);
    }

    private static Flag localFlag(long claimId, FlagDefinition def) {
        return GPFlags.getInstance().getFlagManager().getFlags(Long.toString(claimId)).stream()
                .filter(flag -> flag.getFlagDefinition().getName().equalsIgnoreCase(def.getName()))
                .findFirst().orElse(null);
    }

    private void menu(Player player, long claimId, int requestedPage, String query) {
        Claim claim = currentClaim(player);
        List<FlagDefinition> flags = GPFlags.getInstance().getFlagManager().getFlagDefinitions().stream()
                .filter(def -> def.getFlagType().contains(FlagDefinition.FlagType.CLAIM)
                        && player.hasPermission("gpflags.flag." + def.getName())
                        && Util.canConfigureClaimFlags(player, claim, def))
                .sorted(Comparator.comparing(FlagDefinition::getName)).collect(Collectors.toList());
        if (flags.isEmpty()) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
            return;
        }
        String search = query.trim().toLowerCase(Locale.ROOT);
        flags = flags.stream().filter(def -> def.getName().toLowerCase(Locale.ROOT).contains(search)
                || FlagDescriptions.get(def.getName()).toLowerCase(Locale.ROOT).contains(search))
                .collect(Collectors.toList());
        int pages = Math.max(1, (flags.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button(player, claimId, iconLabel("Search", "item/spyglass"), "Search flag names and descriptions.", view -> {
            String value = view.getText("search");
            menu(player, claimId, 0, value == null ? "" : value);
        }));
        buttons.add(button(player, claimId, iconLabel("Clear search", "item/barrier"), "Show all available flags.", view -> menu(player, claimId, 0, "")));
        for (FlagDefinition def : flags.subList(page * PAGE_SIZE, Math.min(flags.size(), (page + 1) * PAGE_SIZE))) {
            Flag flag = localFlag(claimId, def);
            String state = flag == null ? "Inherited / unset" : flag.getSet() ? "Set" : "Disabled";
            Component label = statusSprite(flag).append(Component.text(" " + def.getName()));
            buttons.add(button(player, claimId, label, state + ". " + FlagDescriptions.get(def.getName())
                            + (def.getParameterType() == FlagParameterType.BOOLEAN ? " Click to cycle: on -> off -> unset -> on." : " Click to edit."),
                    view -> {
                        if (def.getParameterType() == FlagParameterType.BOOLEAN) {
                            toggle(player, claimId, def);
                            menu(player, claimId, page, query);
                        } else edit(player, claimId, def, page, query);
                    }));
        }
        if (page > 0) buttons.add(button(player, claimId, iconLabel("Previous", "widget/page_backward"), "Go to the previous page.", view -> menu(player, claimId, page - 1, query)));
        if (page + 1 < pages) buttons.add(button(player, claimId, iconLabel("Next", "widget/page_forward"), "Go to the next page.", view -> menu(player, claimId, page + 1, query)));
        show(player, "Claim Flags · " + (page + 1) + "/" + pages,
                "Claim #" + claimId + ". " + (flags.isEmpty() ? "No matching flags. " : "Select a flag to edit. ")
                        + "Search names or descriptions, then click Search. Hover a flag for its status and description. Unset flags may inherit defaults or flags from a parent claim, world or server.",
                Collections.singletonList(DialogInput.text("search", Component.text("Search flags"))
                        .initial(query).maxLength(100).width(300).build()), buttons, close());
    }

    private void toggle(Player player, long claimId, FlagDefinition def) {
        Claim claim = currentClaim(player);
        if (!player.hasPermission("gpflags.flag." + def.getName()) || !Util.canConfigureClaimFlags(player, claim, def)) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
            return;
        }
        Flag local = localFlag(claimId, def);
        if (local != null) {
            // An explicit off value must be removed to restore inheritance.
            SetFlagResult result = local.getSet()
                    ? GPFlags.getInstance().getFlagManager().setFlag(Long.toString(claimId), def, false, player)
                    : GPFlags.getInstance().getFlagManager().unSetFlag(Long.toString(claimId), def);
            MessagingUtil.sendMessage(player, result.isSuccess() ? TextMode.Success : TextMode.Err,
                    result.getMessage().getMessageID(), result.getMessage().getMessageParams());
            if (result.isSuccess()) GPFlags.getInstance().getFlagManager().save();
        } else {
            new CommandSetClaimFlag().onCommand(player, Objects.requireNonNull(Bukkit.getPluginCommand("setclaimflag")),
                    "setclaimflag", new String[]{def.getName()});
        }
    }

    private static DialogInput parameterInput(FlagDefinition def, String initial) {
        FlagParameterType type = def.getParameterType();
        if (!type.getChoices().isEmpty()) {
            List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
            boolean known = type.getChoices().stream().anyMatch(value -> value.equalsIgnoreCase(initial));
            for (int i = 0; i < type.getChoices().size(); i++) {
                String value = type.getChoices().get(i);
                options.add(SingleOptionDialogInput.OptionEntry.create(value, Component.text(value),
                        value.equalsIgnoreCase(initial) || !known && i == 0));
            }
            return DialogInput.singleOption("parameters", Component.text(type.getLabel()), options).build();
        }
        return DialogInput.text("parameters", Component.text(type.getLabel()))
                .initial(initial).maxLength(Math.max(4096, initial.length())).build();
    }

    private void edit(Player player, long claimId, FlagDefinition def, int page, String query) {
        if (!player.hasPermission("gpflags.flag." + def.getName())
                || !Util.canConfigureClaimFlags(player, currentClaim(player), def)) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
            return;
        }
        Flag flag = localFlag(claimId, def);
        String initial = flag == null ? "" : flag.getFriendlyParameters();
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button(player, claimId, "Save", view -> {
            String value = view.getText("parameters");
            List<String> args = new ArrayList<>();
            args.add(def.getName());
            if (value != null && !value.trim().isEmpty()) args.addAll(Arrays.asList(value.trim().split(" +")));
            new CommandSetClaimFlag().onCommand(player, Objects.requireNonNull(Bukkit.getPluginCommand("setclaimflag")),
                    "setclaimflag", args.toArray(new String[0]));
        }));
        if (flag != null) buttons.add(button(player, claimId, "Remove", view -> {
            new CommandUnsetClaimFlag().onCommand(player, Objects.requireNonNull(Bukkit.getPluginCommand("unsetclaimflag")),
                    "unsetclaimflag", new String[]{def.getName()});
            menu(player, claimId, page, query);
        }));
        show(player, def.getName(), "Enter the same parameters you would use with /setclaimflag. Leave empty for flags without parameters. Remove restores inherited behavior.",
                Collections.singletonList(parameterInput(def, initial)), buttons,
                button(player, claimId, "Back", view -> menu(player, claimId, page, query)));
    }

    private ActionButton button(Player player, long claimId, String label, Consumer<DialogResponseView> action) {
        return button(player, claimId, label, null, action);
    }

    private ActionButton button(Player player, long claimId, String label, String lore, Consumer<DialogResponseView> action) {
        return button(player, claimId, Component.text(label), lore, action);
    }

    private ActionButton button(Player player, long claimId, Component label, String lore, Consumer<DialogResponseView> action) {
        return ActionButton.create(label, lore == null ? null : tooltip(lore), 150, DialogAction.customClick((view, audience) -> {
            if (!player.equals(audience)) return;
            Bukkit.getScheduler().runTask(GPFlags.getInstance(), () -> {
                if (!player.isOnline()) return;
                Claim claim = currentClaim(player);
                if (!player.hasPermission("gpflags.command.setclaimflag") || claim == null
                        || !Objects.equals(claim.getID(), claimId)) {
                    player.sendMessage(Component.text("This dialog has expired. Open /claimflags in the claim again."));
                    return;
                }
                action.accept(view);
            });
        }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build()));
    }

    private static Component statusSprite(Flag flag) {
        String item = flag == null ? "gray_dye" : flag.getSet() ? "lime_dye" : "red_dye";
        return itemSprite(item);
    }

    private static Component iconLabel(String label, String sprite) {
        Component icon = sprite.startsWith("item/") ? itemSprite(sprite.substring(5))
                : Component.object(ObjectContents.sprite(Key.key("minecraft:gui"), Key.key("minecraft:" + sprite)));
        return icon.append(Component.text(" " + label));
    }

    private static Component itemSprite(String item) {
        // Item sprites moved to their own atlas in Minecraft 1.21.11.
        String[] version = Bukkit.getMinecraftVersion().split("\\.");
        int major = Integer.parseInt(version[0]);
        int minor = Integer.parseInt(version[1]);
        int patch = version.length > 2 ? Integer.parseInt(version[2]) : 0;
        boolean itemsAtlas = major > 1 || minor > 21 || minor == 21 && patch >= 11;
        return Component.object(ObjectContents.sprite(Key.key(itemsAtlas ? "minecraft:items" : "minecraft:blocks"),
                Key.key("minecraft:item/" + item)));
    }

    /** Dialog tooltips do not wrap automatically. Keep long descriptions readable on screen. */
    private static Component tooltip(String description) {
        StringBuilder wrapped = new StringBuilder();
        int width = 0;
        for (String word : description.split(" +")) {
            if (width > 0) {
                if (width + 1 + word.length() > 55) {
                    wrapped.append('\n');
                    width = 0;
                } else {
                    wrapped.append(' ');
                    width++;
                }
            }
            wrapped.append(word);
            width += word.length();
        }
        return Component.text(wrapped.toString());
    }

    private static ActionButton close() {
        return ActionButton.create(Component.text("Close"), null, 150, null);
    }

    private static void show(Player player, String title, String description, List<DialogInput> inputs,
                             List<ActionButton> buttons, ActionButton exit) {
        player.showDialog(Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text(title)).pause(false).canCloseWithEscape(true)
                        .body(Collections.singletonList(DialogBody.plainMessage(Component.text(description), 350)))
                        .inputs(inputs).build())
                .type(DialogType.multiAction(buttons, exit, 2))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
