package io.github.caojohnny.fsf_random_death.listener;

import io.github.caojohnny.fsf_random_death.data.ThresholdConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PunchListener implements Listener {
    private final ThresholdConfig thresholdConfig;

    public PunchListener(ThresholdConfig thresholdConfig) {
        this.thresholdConfig = thresholdConfig;
    }

    private Player selectRandomPlayer() {
        List<Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers()
                .stream()
                .collect(Collectors.toUnmodifiableList());

        int randIndex = ThreadLocalRandom.current().nextInt(onlinePlayers.size());
        return onlinePlayers.get(randIndex);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player actor = event.getPlayer();

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR) {
            return;
        }

        double rand = ThreadLocalRandom.current().nextDouble(1);
        if (rand > this.thresholdConfig.getThreshold()) {
            return;
        }

        Player randPlayer = this.selectRandomPlayer();
        randPlayer.setHealth(0.0D);

        Bukkit.getServer().broadcastMessage(randPlayer.getName() + " has been randomly killed by " + actor.getName());
    }
}
