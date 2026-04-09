package me.boggerbyte.deathboxes.deathbox;

import me.boggerbyte.deathboxes.Main;
import me.boggerbyte.deathboxes.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class Deathbox {
    public static final String metadataKey = "deathboxes_deathbox";

    private final OfflinePlayer owner;
    private final Inventory inventory;
    private final int exp;
    private final boolean locked;
    private final boolean breakable;
    private final Hologram hologram;
    private final int duration;
    private final int unlockAfter;

    public Deathbox(
            OfflinePlayer owner,
            Inventory inventory,
            int exp,
            boolean locked,
            boolean breakable,
            Hologram hologram,
            int duration,
            int unlockAfter
    ) {
        this.owner = owner;
        this.inventory = inventory;
        this.exp = exp;
        this.locked = locked;
        this.breakable = breakable;
        this.hologram = hologram;
        this.duration = duration;
        this.unlockAfter = unlockAfter;
    }

    private Block block;
    private Location location;
    private boolean sealed = true;
    private long unlockAtMillis = -1L;
    private long despawnAtMillis = -1L;
    private BukkitTask unlockTask;
    private BukkitTask despawnTask;

    public void spawnFalling(Plugin plugin, Location location) {
        var blockLocation = location.getBlock().getLocation();

        var fallingBlock = blockLocation.getWorld().spawnFallingBlock(blockLocation, Material.ANVIL.createBlockData());
        fallingBlock.setHurtEntities(false);
        fallingBlock.setDropItem(false);
        fallingBlock.setMetadata(Deathbox.metadataKey, new FixedMetadataValue(plugin, this));
    }

    public void spawn(Plugin plugin, Location location) {
        this.location = location.getBlock().getLocation();
        Main.addGrave(this);
        refresh(plugin);
        scheduleLifecycle(plugin, System.currentTimeMillis());
    }

    public void restore(Plugin plugin, Location location) {
        this.location = location.getBlock().getLocation();
        Main.addGrave(this);
        refresh(plugin);
        scheduleLifecycle(plugin, System.currentTimeMillis());
    }

    public void restore(Plugin plugin, Location location, boolean unlocked, long unlockAtMillis, long despawnAtMillis) {
        this.location = location.getBlock().getLocation();
        this.unlocked = unlocked;
        this.unlockAtMillis = unlockAtMillis;
        this.despawnAtMillis = despawnAtMillis;
        Main.addGrave(this);
        refresh(plugin);
        scheduleLifecycle(plugin, System.currentTimeMillis());
    }

    private void scheduleLifecycle(Plugin plugin, long now) {
        cancelLifecycleTasks();

        if (locked) {
            if (unlocked) {
                unlockAtMillis = -1L;
            } else if (unlockAtMillis <= 0L && unlockAfter >= 0) {
                unlockAtMillis = now + (unlockAfter * 50L);
            }

            if (!unlocked && unlockAtMillis > 0L) {
                var unlockDelayTicks = Math.max(1L, (unlockAtMillis - now + 49L) / 50L);
                if (unlockAtMillis <= now) {
                    unlocked = true;
                    unlockAtMillis = -1L;
                } else {
                    unlockTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        unlocked = true;
                        unlockAtMillis = -1L;
                    }, unlockDelayTicks);
                }
            }
        } else {
            unlocked = true;
            unlockAtMillis = -1L;
        }

        if (duration >= 0) {
            if (despawnAtMillis <= 0L) {
                despawnAtMillis = now + (duration * 50L);
            }

            if (despawnAtMillis <= now) {
                despawn(plugin, false);
                return;
            }

            var despawnDelayTicks = Math.max(1L, (despawnAtMillis - now + 49L) / 50L);
            despawnTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> despawn(plugin, false), despawnDelayTicks);
        } else {
            despawnAtMillis = -1L;
        }
    }

    private void cancelLifecycleTasks() {
        if (unlockTask != null) {
            unlockTask.cancel();
            unlockTask = null;
        }
        if (despawnTask != null) {
            despawnTask.cancel();
            despawnTask = null;
        }
    }

    public void refresh(Plugin plugin) {
        if (location == null) return;

        var world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

        block = world.getBlockAt(location);
        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST);
        }
        block.setMetadata(Deathbox.metadataKey, new FixedMetadataValue(plugin, this));

        if (!hologram.isSpawned()) {
            var blockLocation = block.getLocation();
            var blockCenterLocation = blockLocation.add(blockLocation.getX() > 0 ? -0.5 : 0.5, 0.5, blockLocation.getZ() > 0 ? -0.5 : 0.5);
            removeNearbyDisplayEntities(blockCenterLocation);
            hologram.spawn(plugin, blockCenterLocation, getHologramDuration());
        }
    }

    private int getHologramDuration() {
        if (duration < 0 || despawnAtMillis <= 0L) {
            return duration;
        }

        var remainingTicks = (int) Math.max(0L, (despawnAtMillis - System.currentTimeMillis() + 49L) / 50L);
        return remainingTicks;
    }

    public void despawn(Plugin plugin, boolean dropContents) {
        Main.removeGrave(this);
        cancelLifecycleTasks();
        hologram.remove();

        if (location == null) return;

        var world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

        block = world.getBlockAt(location);
        removeNearbyDisplayEntities(block.getLocation().add(block.getX() > 0 ? -0.5 : 0.5, 0.5, block.getZ() > 0 ? -0.5 : 0.5));
        block.setType(Material.AIR);
        block.removeMetadata(Deathbox.metadataKey, plugin);

        if (dropContents) inventory.forEach(item -> {
            if (item != null) block.getWorld().dropItemNaturally(block.getLocation(), item);
        });
    }

    private void removeNearbyDisplayEntities(Location centerLocation) {
        var world = centerLocation.getWorld();
        if (world == null) return;

        for (var entity : world.getNearbyEntities(centerLocation, 0.75, 2.0, 0.75)) {
            if (entity instanceof AreaEffectCloud cloud && cloud.getRadius() == 0 && cloud.isCustomNameVisible()) {
                cloud.remove();
            }
        }
    }

    public void openInventory(HumanEntity humanEntity) {
        humanEntity.openInventory(inventory);

        if (sealed && humanEntity instanceof Player player && player.getUniqueId().equals(owner.getUniqueId())) {
            sealed = false;
            player.giveExp(exp);
        }
    }


    public OfflinePlayer getOwner() {
        return owner;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isBreakable() {
        return breakable;
    }
    private boolean beingViewed = false;

public boolean isBeingViewed() {
    return beingViewed;
}

public void setBeingViewed(boolean beingViewed) {
    this.beingViewed = beingViewed;
}
private boolean unlocked = false;

public boolean isUnlocked() {
    return unlocked;
}
public Block getBlock() { return block; }
public Inventory getInventory() { return inventory; }
public Location getLocation() { return location == null ? null : location.clone(); }
public long getUnlockAtMillis() { return unlockAtMillis; }
public long getDespawnAtMillis() { return despawnAtMillis; }

}
