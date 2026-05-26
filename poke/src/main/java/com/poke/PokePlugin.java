package com.poke;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PokePlugin extends JavaPlugin implements Listener {

    // Color progression: brown -> gray -> ROYGBIV -> light purple -> lilac -> white
    // Using hex colors for smooth rainbow transitions not covered by NamedTextColor
    private static final TextColor[] COLORS = {
        TextColor.fromHexString("#8B4513"), // 0: saddle brown
        TextColor.fromHexString("#808080"), // 1: gray
        TextColor.fromHexString("#FF0000"), // 2: red
        TextColor.fromHexString("#FF7F00"), // 3: orange
        TextColor.fromHexString("#FFFF00"), // 4: yellow
        TextColor.fromHexString("#00FF00"), // 5: green
        TextColor.fromHexString("#007FFF"), // 6: azure/blue
        TextColor.fromHexString("#4B0082"), // 7: indigo
        TextColor.fromHexString("#9400D3"), // 8: violet
        TextColor.fromHexString("#DDA0DD"), // 9: plum / light purple
        TextColor.fromHexString("#E6E6FA"), // 10: lavender
        TextColor.fromHexString("#FFFFFF"), // 11: white
    };

    // Map of player UUID -> poke count (LinkedHashMap for deterministic save order)
    private final Map<UUID, Integer> pokeCounts = new LinkedHashMap<>();

    // File to persist data
    private final File dataFile = new File(getDataFolder(), "pokes.dat");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadData();

        // Update tab list for all online players every second to reflect score changes
        // Using LOW priority so this runs after other plugins that may change player names
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateTabDisplay(player);
            }
        }, 20L, 20L);

        getLogger().info("Poke plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        pokeCounts.clear(); // Reset to avoid accumulating entries on reload
        try (FileReader reader = new FileReader(dataFile)) {
            StringBuilder content = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) content.append((char) c);
            String[] lines = content.toString().split("\n");
            for (String line : lines) {
                if (line.isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length != 2) continue; // Skip malformed lines
                try {
                    UUID uuid = UUID.fromString(parts[0]);
                    int count = Integer.parseInt(parts[1].trim());
                    pokeCounts.put(uuid, count);
                } catch (IllegalArgumentException e) {
                    // Skip lines with invalid UUID or count
                }
            }
        } catch (IOException e) {
            getLogger().warning("Failed to load poke data: " + e.getMessage());
        }
    }

    private void saveData() {
        try {
            dataFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(dataFile)) {
                for (Map.Entry<UUID, Integer> entry : pokeCounts.entrySet()) {
                    writer.write(entry.getKey().toString() + ":" + entry.getValue() + "\n");
                }
            }
        } catch (IOException e) {
            getLogger().warning("Failed to save poke data: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerHit(PlayerInteractEntityEvent event) {
        // Only care about right-clicking a player (to "poke" them)
        if (!(event.getRightClicked() instanceof Player victim)) return;
        if (event.getPlayer().equals(victim)) return; // Can't poke yourself

        int count = pokeCounts.getOrDefault(victim.getUniqueId(), 0) + 1;
        pokeCounts.put(victim.getUniqueId(), count);

        // Broadcast the poke
        TextColor msgColor = getColorForCount(count);
        Component msg = Component.text(victim.getName())
                .color(msgColor)
                .append(Component.text(" got poked by "))
                .append(Component.text(event.getPlayer().getName()).color(getColorForCount(pokeCounts.getOrDefault(event.getPlayer().getUniqueId(), 0))))
                .append(Component.text("! (Total: " + count + ")").color(msgColor));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(msg);
        }

        // Update tab list for all players immediately
        updateTabDisplay(victim);
        updateTabDisplay(event.getPlayer());

        saveData();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateTabDisplay(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveData();
    }

    /**
     * Set the player's tab list name to include their poke count.
     * Format: PlayerName (count)
     */
    private void updateTabDisplay(Player player) {
        int count = pokeCounts.getOrDefault(player.getUniqueId(), 0);
        TextColor color = getColorForCount(count);

        Component name = Component.text(player.getName())
                .color(color)
                .append(Component.text(" (" + count + ")").color(color));

        player.playerListName(name);
    }

    /**
     * Get a color that progresses through the 12 color brackets on a bottom-weighted curve.
     * Uses a gentler power curve for count 1-7 (gray-red) to ensure all 12 colors are reachable.
     * Formula (piecewise):
     *   count <= 0: brown (COLORS[0])
     *   count 1-7:  index = pow(count+1, 0.5) / pow(8, 0.5) * 2  → maps to gray(1) or red(2)
     *   count >= 8:  index = pow(count+1, 0.25) / pow(1501, 0.25) * 11  → standard ROYGBIV curve
     * White is reached at 1500 pokes.
     *
     * Color brackets:
     *   brown → gray → red → orange → yellow → green → blue → indigo → violet → plum → lavender → white
     */
    private TextColor getColorForCount(int count) {
        double index;
        if (count <= 0) {
            return COLORS[0]; // brown
        } else if (count <= 7) {
            // Gentler power so count=1 lands in gray (index 1) and count=7 lands exactly at red (index 2)
            index = Math.pow(count + 1, 0.5) / Math.pow(8, 0.5) * 2.0;
        } else {
            index = Math.pow(count + 1, 0.25) / Math.pow(1501, 0.25) * 11.0;
        }
        index = Math.min(index, 11.0);

        int lower = (int) Math.floor(index);
        double t = index - lower; // 0..1 smooth interpolation between brackets

        return interpolate(COLORS[lower], COLORS[Math.min(lower + 1, 11)], t);
    }

    /**
     * Linearly interpolate between two TextColors by a factor t (0.0 to 1.0).
     */
    private TextColor interpolate(TextColor from, TextColor to, double t) {
        int r = (int) Math.round(from.red() + (to.red() - from.red()) * t);
        int g = (int) Math.round(from.green() + (to.green() - from.green()) * t);
        int b = (int) Math.round(from.blue() + (to.blue() - from.blue()) * t);
        return TextColor.fromRgb((r << 16) | (g << 8) | b);
    }
}