package com.vogmod;

import com.vogmod.commands.VogCommand;
import com.vogmod.audio.AudioPlayer;
import com.vogmod.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Vog extends JavaPlugin implements Listener {

    private static Vog instance;
    private ConfigManager configManager;
    private AudioPlayer audioPlayer;
    
    // Track players who have pending resource pack downloads
    private final java.util.Map<String, Boolean> packAccepted = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        audioPlayer = new AudioPlayer(this);
        
        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
        
        var cmd = getCommand("vog");
        if (cmd != null) {
            cmd.setExecutor(new VogCommand(this));
        }
        
        getLogger().info("Vog (Voice of God) enabled!");
    }

    @Override
    public void onDisable() {
        if (audioPlayer != null) {
            audioPlayer.shutdown();
        }
        getLogger().info("Vog disabled.");
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        getLogger().info("Resource pack status for " + playerName + ": " + event.getStatus());
        
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED:
                getLogger().info("Resource pack loaded for " + playerName + "!");
                packAccepted.put(playerName, true);
                // Trigger sound play
                audioPlayer.onPackLoaded(player);
                break;
            case DECLINED:
                getLogger().warning(playerName + " declined the resource pack!");
                packAccepted.remove(playerName);
                break;
            case FAILED_DOWNLOAD:
                getLogger().warning(playerName + " failed to download resource pack!");
                packAccepted.remove(playerName);
                break;
            case ACCEPTED:
                getLogger().info(playerName + " accepted resource pack, downloading...");
                break;
            default:
                break;
        }
    }
    
    public boolean hasPackAccepted(String playerName) {
        return packAccepted.getOrDefault(playerName, false);
    }

    public static Vog getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }
}
