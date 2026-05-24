package io.github.caojohnny.fsf_random_death.data;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThresholdConfig {
    private static final String THRESHOLD_CONFIG_NAME = "threshold.yaml";

    private static final String THRESHOLD_KEY = "threshold";

    private final Path configPath;
    private final YamlConfiguration yaml;

    private double threshold;

    public ThresholdConfig(JavaPlugin plugin) throws IOException, InvalidConfigurationException {
        this.configPath = getConfigPath(plugin);
        this.yaml = loadYaml(this.configPath);

        this.threshold = this.yaml.getDouble(THRESHOLD_KEY);
    }

    private static Path getConfigPath(JavaPlugin plugin) {
        return plugin.getDataFolder().toPath().resolve(THRESHOLD_CONFIG_NAME);
    }

    private static YamlConfiguration loadYaml(Path configPath) throws IOException, InvalidConfigurationException {
        String configString = Files.readString(configPath);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(configString);

        return yaml;
    }

    public double getThreshold() {
        return this.threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void save() throws IOException {
        this.yaml.set(THRESHOLD_KEY, this.threshold);

        this.yaml.save(this.configPath.toFile());
    }
}
