package io.github.caojohnny.fsf_random_death.cmd;

import io.github.caojohnny.fsf_random_death.data.ThresholdConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class RandomDeathCmd implements CommandExecutor {
    private final ThresholdConfig thresholdConfig;

    public RandomDeathCmd(ThresholdConfig thresholdConfig) {
        this.thresholdConfig = thresholdConfig;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender,
                             @NonNull Command command,
                             @NonNull String label,
                             @NonNull String[] args) {
        if (args.length != 2) {
            return false;
        }

        String subcommand = args[0];
        if (subcommand.equals("threshold")) {
            try {
                double chance = Double.parseDouble(args[1]);

                this.thresholdConfig.setThreshold(chance);
                sender.sendMessage("Set random death threshold to: " + chance);
            } catch (NumberFormatException e) {
                sender.sendMessage("Should be a valid decimal! (" + args[1] + ")");
                return true;
            }
        }

        return false;
    }
}
