package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlagDef_PlayerWeather extends PlayerMovementFlagDefinition implements Listener {

    public FlagDef_PlayerWeather(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(Player player, Location from, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        // Reset the weather if moving from enabled to disabled
        if (flagTo == null && flagFrom != null) {
            player.resetPlayerWeather();
            return;
        }

        // Set weather to new flag if exists
        if (flagTo == null) return;
        setPlayerWeather(player, flagTo);
    }

    public void setPlayerWeather(Player player, @NotNull Flag flag) {
        String weather = flag.parameters;
        if (weather.equalsIgnoreCase("sun")) {
            player.setPlayerWeather(WeatherType.CLEAR);
        } else if (weather.equalsIgnoreCase("rain")) {
            player.setPlayerWeather(WeatherType.DOWNFALL);
        }
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.PlayerWeatherRequired));
        }
        if (!parameters.equalsIgnoreCase("sun") && !parameters.equalsIgnoreCase("rain")) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.PlayerWeatherRequired));
        }
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public String getName() {
        return "PlayerWeather";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.PlayerWeatherSet, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.PlayerWeatherUnSet);
    }

}
