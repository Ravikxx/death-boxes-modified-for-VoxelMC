package me.boggerbyte.deathboxes.listeners;

import me.boggerbyte.deathboxes.Main;
import me.boggerbyte.deathboxes.deathbox.Deathbox;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

public class DeathboxEventsListener implements Listener {
    private final Plugin plugin = Main.getInstance();

    private Deathbox fetchDeathboxValue(Metadatable metadatable) {
        var rawValue = metadatable.getMetadata(Deathbox.metadataKey);
        if (rawValue.isEmpty()) return null;
        if (!(rawValue.get(0).value() instanceof Deathbox value)) return null;
        return value;
    }

    private Deathbox fetchDeathboxValue(Entity entity) {
        return fetchDeathboxValue((Metadatable) entity);
    }

    @EventHandler
    public void onDeathboxLanding(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        var deathbox = fetchDeathboxValue(fallingBlock);
        if (deathbox == null) return;

        event.setCancelled(true);
        event.getEntity().remove();

        deathbox.spawn(plugin, event.getEntity().getLocation());
    }

    @EventHandler
    public void onDeathboxBlockBreak(BlockBreakEvent event) {
        var block = event.getBlock();
        var deathbox = fetchDeathboxValue(block);
        if (deathbox == null) deathbox = Main.getGraveAt(block.getLocation());
        if (deathbox == null) return;

        event.setCancelled(true);

        if (deathbox.isBreakable()){
            deathbox.despawn(plugin, true);
        }
    }

    @EventHandler
    public void onDeathboxInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var block = event.getClickedBlock();
        if (!(block.getState() instanceof Chest)) return;

        var deathbox = fetchDeathboxValue(block);
        if (deathbox == null) deathbox = Main.getGraveAt(block.getLocation());
        if (deathbox == null) return;

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        if (deathbox.isLocked() && !deathbox.isUnlocked() && !event.getPlayer().getUniqueId().equals(deathbox.getOwner().getUniqueId())) {
            return;
        }

        var targetDeathbox = deathbox;
        deathbox.setBeingViewed(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> targetDeathbox.openInventory(event.getPlayer()));
    }
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (var grave : Main.getActiveGraves()) {
            var location = grave.getLocation();
            if (location == null) continue;
            if (!location.getWorld().equals(event.getWorld())) continue;
            if ((location.getBlockX() >> 4) == event.getChunk().getX() && (location.getBlockZ() >> 4) == event.getChunk().getZ()) {
                grave.refresh(plugin);
            }
        }
    }

}
