package me.boggerbyte.deathboxes.listeners;

import me.boggerbyte.deathboxes.Main;
import me.boggerbyte.deathboxes.deathbox.Deathbox;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

public class InventoryClickListener implements Listener {
    private final Plugin plugin = Main.getInstance();

    private Deathbox getDeathbox(InventoryClickEvent event) {
        return Main.getGraveByInventory(event.getView().getTopInventory());
    }

    private Deathbox getDeathbox(InventoryDragEvent event) {
        return Main.getGraveByInventory(event.getView().getTopInventory());
    }

    private Deathbox getDeathbox(InventoryCloseEvent event) {
        return Main.getGraveByInventory(event.getInventory());
    }

    private void despawnIfEmpty(Deathbox deathbox) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (deathbox.getInventory().isEmpty()) {
                deathbox.despawn(plugin, false);
            }
        }, 1L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        var deathbox = getDeathbox(event);
        if (deathbox == null) return;

        var topInventory = event.getView().getTopInventory();
        var clickedTop = event.getRawSlot() < topInventory.getSize();
        var clickedBottom = event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory());
        var cursorHasItem = event.getCursor() != null && event.getCursor().getType() != Material.AIR;

        if (event.isShiftClick() && clickedBottom) {
            event.setCancelled(true);
            return;
        }

        if (clickedTop && (cursorHasItem || event.getHotbarButton() >= 0)) {
            event.setCancelled(true);
            return;
        }

        despawnIfEmpty(deathbox);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        var deathbox = getDeathbox(event);
        if (deathbox == null) return;

        var topSize = event.getView().getTopInventory().getSize();
        for (var rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }

        despawnIfEmpty(deathbox);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        var deathbox = getDeathbox(event);
        if (deathbox == null) return;

        deathbox.setBeingViewed(false);
        if (deathbox.getInventory().isEmpty()) {
            deathbox.despawn(plugin, false);
        }
    }
}
