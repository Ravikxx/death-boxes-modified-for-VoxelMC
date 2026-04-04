package me.boggerbyte.deathboxes.listeners;

import me.boggerbyte.deathboxes.Main;
import me.boggerbyte.deathboxes.deathbox.Deathbox;
import org.bukkit.block.Chest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
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
        if (deathbox == null) return;

        event.setCancelled(true);

        if (deathbox.isBreakable()){
            deathbox.despawn(plugin, true);
        }
    }

    @EventHandler
    public void onDeathboxInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;
        var block = chest.getBlock();
        var deathbox = fetchDeathboxValue(block);
        if (deathbox == null) return;
        deathbox.setBeingViewed(true);
        deathbox.openInventory(event.getPlayer());


        event.setCancelled(true);

        if (deathbox.isLocked() && !event.getPlayer().getUniqueId().equals(deathbox.getOwner().getUniqueId())) return;

        deathbox.openInventory(event.getPlayer());
    }
}
