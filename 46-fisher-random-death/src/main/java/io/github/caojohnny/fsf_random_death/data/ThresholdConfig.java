package io.github.caojohnny.fsf_random_death.data;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class ThresholdConfig {
    private static final String THRESHOLD_CONFIG_NAME = "threshold.yaml";

    private static final String THRESHOLD_KEY = "threshold";

    private final Path configPath;
    private final YamlConfiguration yaml;

    private double threshold;

    public ThresholdConfig(JavaPlugin plugin) throws IOException, InvalidConfigurationException {
        this.configPath = getConfigPath(plugin, THRESHOLD_CONFIG_NAME);
        this.yaml = loadYaml(plugin, this.configPath);

        this.threshold = this.yaml.getDouble(THRESHOLD_KEY);
    }

    @SuppressWarnings("SameParameterValue")
    private static Path getConfigPath(JavaPlugin plugin, String configName) {
        return plugin.getDataFolder().toPath().resolve(configName);
    }

    private static void saveDefaultConfig(JavaPlugin plugin, Path configPath) throws IOException {
        Path dataFolderPath = plugin.getDataFolder().toPath();
        if (!Files.exists(dataFolderPath)) {
            Files.createDirectories(dataFolderPath);
        }

        String fileName = configPath.getFileName().toString();
        Files.copy(requireNonNull(plugin.getResource(fileName)), configPath);
    }

    private static YamlConfiguration loadYaml(JavaPlugin plugin, Path configPath) throws IOException, InvalidConfigurationException {
        if (!Files.exists(configPath)) {
            saveDefaultConfig(plugin, configPath);
        }

        String configString = Files.readString(configPath);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(configString);

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
