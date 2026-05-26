package com.vogmod.config;

import com.vogmod.Vog;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {

    private final Vog plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ConfigManager(Vog plugin) {
        this.plugin = plugin;
    }

    public int getCooldownSeconds() {
        return plugin.getConfig().getInt("cooldown-seconds", 60);
    }

    public int getMaxMessageLength() {
        return plugin.getConfig().getInt("max-message-length", 200);
    }

    public String getVoice() {
        return plugin.getConfig().getString("voice", "en-US-JennyNeural");
    }

    public double getVolume() {
        return plugin.getConfig().getDouble("volume", 1.0);
    }

    public int getEchoCount() {
        return plugin.getConfig().getInt("echo-count", 3);
    }

    public int getEchoDelayMs() {
        return plugin.getConfig().getInt("echo-delay-ms", 400);
    }

    public double getEchoVolumeDecay() {
        return plugin.getConfig().getDouble("echo-volume-decay", 0.6);
    }

    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    public boolean shouldTestVanillaSound() {
        return plugin.getConfig().getBoolean("test-vanilla-sound", false);
    }

    public boolean isOnCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }
        long cooldownEnd = cooldowns.get(playerId);
        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return 0;
        }
        long remaining = cooldowns.get(playerId) - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis() + (getCooldownSeconds() * 1000L));
    }

    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
