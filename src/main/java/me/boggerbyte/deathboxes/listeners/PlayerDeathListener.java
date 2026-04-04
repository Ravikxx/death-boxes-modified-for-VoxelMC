package me.boggerbyte.deathboxes.listeners;

import me.boggerbyte.deathboxes.Main;
import me.boggerbyte.deathboxes.deathbox.DeathboxFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

public class PlayerDeathListener implements Listener {
    private final Plugin plugin = Main.getInstance();
    private final DeathboxFactory deathboxFactory;

    public PlayerDeathListener(DeathboxFactory deathboxFactory) {
        this.deathboxFactory = deathboxFactory;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        event.setDroppedExp(0);
        


        var player = event.getEntity();
        var deathbox = deathboxFactory.create(player);
        deathbox.spawnFalling(plugin, player.getLocation());
        
        player.getInventory().clear();
    }
}
