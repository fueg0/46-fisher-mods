package io.github.caojohnny.fsf_random_death;

import io.github.caojohnny.fsf_random_death.cmd.RandomDeathCmd;
import io.github.caojohnny.fsf_random_death.data.ThresholdConfig;
import io.github.caojohnny.fsf_random_death.listener.PunchListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unused")
public class FsfRandomDeath extends JavaPlugin {
    private ThresholdConfig thresholdConfig;

    @Override
    public void onEnable() {
        try {
            this.thresholdConfig = new ThresholdConfig(this);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PunchListener(this.thresholdConfig), this);

        requireNonNull(this.getCommand("randomdeath")).setExecutor(new RandomDeathCmd(this.thresholdConfig));
    }

    @Override
    public void onDisable() {
        try {
            this.thresholdConfig.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
