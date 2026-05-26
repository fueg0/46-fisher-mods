package com.vogmod.commands;

import com.vogmod.Vog;
import com.vogmod.audio.AudioPlayer;
import com.vogmod.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VogCommand implements CommandExecutor {

    private final Vog plugin;
    private final ConfigManager config;
    private final AudioPlayer audioPlayer;

    public VogCommand(Vog plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.audioPlayer = plugin.getAudioPlayer();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /vog <message>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Speak to the heavens...", NamedTextColor.GRAY));
            return true;
        }

        String message = String.join(" ", args);
        
        if (message.length() > config.getMaxMessageLength()) {
            sender.sendMessage(Component.text("Message too long! Max: " + 
                config.getMaxMessageLength() + " characters.", NamedTextColor.RED));
            return true;
        }

        // Check cooldown for players
        if (sender instanceof Player player) {
            if (!player.hasPermission("vog.bypass.cooldown") && config.isOnCooldown(player.getUniqueId())) {
                long remaining = config.getRemainingCooldown(player.getUniqueId());
                sender.sendMessage(Component.text("Cooldown! Wait " + remaining + " seconds.", NamedTextColor.RED));
                return true;
            }
            config.setCooldown(player.getUniqueId());
        }

        sender.sendMessage(Component.text("The Voice of God speaks...", NamedTextColor.LIGHT_PURPLE));
        
        // Generate and play TTS via dynamic resource pack
        audioPlayer.playTts(message, () -> {
            plugin.getServer().broadcast(
                Component.text("The Voice of God has spoken!", NamedTextColor.GOLD)
            );
        });

        return true;
    }
}
