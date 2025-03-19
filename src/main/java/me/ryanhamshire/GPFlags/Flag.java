package me.ryanhamshire.GPFlags;

import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Main flag object
 */
public class Flag {

    FlagDefinition flagDefinition;
    public String parameters;
    private String[] parametersArray;
    private boolean set = true;

    Flag(FlagDefinition definition, String parameters) {
        this.flagDefinition = definition;
        this.parameters = parameters;
        this.parameters = this.parameters.replace('&', (char) 0x00A7);
    }

    /**
     * Get parameters for this flag
     *
     * @return Parameters for this flag
     */
    public String[] getParametersArray() {
        if (this.parametersArray != null) return this.parametersArray;
        this.parametersArray = this.parameters.split(" ");
        return this.parametersArray;
    }

    public String getFriendlyParameters() {
        StringBuilder builder = new StringBuilder();
        if (flagDefinition.getName().equals("NoEnterPlayer")) {
            for (String idOrName : getParametersArray()) {
                if (idOrName.length() > 30) {
                    // if long, try parsing it as a uuid
                    try {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(idOrName));
                        if (offlinePlayer.getName() != null) {
                            idOrName = offlinePlayer.getName();
                        }
                    } catch (Exception ignored) {
                    }
                }
                builder.append(idOrName).append(" ");
            }
            return builder.toString().trim();
        }
        return parameters;
    }

    public String getParameters() {
        return parameters;
    }

    /**
     * Gets if this flag is currently active
     *
     * @return Activity of flag
     */
    public boolean getSet() {
        return this.set;
    }

    /**
     * Sets if this flag is currently active
     *
     * @param value if the flag is active
     */
    public void setSet(boolean value) {
        this.set = value;
    }

    /**
     * Get the flag definition of this flag
     *
     * @return Flag definition of this flag
     */
    public FlagDefinition getFlagDefinition() {
        return flagDefinition;
    }

}
