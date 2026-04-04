package me.boggerbyte.deathboxes.deathbox;

import me.boggerbyte.deathboxes.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class Deathbox {
    public static final String metadataKey = "deathboxes_deathbox";

    private final Player owner;
    private final Inventory inventory;
    private final int exp;
    private final boolean locked;
    private final boolean breakable;
    private final Hologram hologram;
    private final int duration;

    public Deathbox(
            Player owner,
            Inventory inventory,
            int exp,
            boolean locked,
            boolean breakable,
            Hologram hologram,
            int duration
    ) {
        this.owner = owner;
        this.inventory = inventory;
        this.exp = exp;
        this.locked = locked;
        this.breakable = breakable;
        this.hologram = hologram;
        this.duration = duration;
    }

    private Block block;
    private boolean sealed = true;

    public void spawnFalling(Plugin plugin, Location location) {
        var blockLocation = location.getBlock().getLocation();

        var fallingBlock = blockLocation.getWorld().spawnFallingBlock(blockLocation, Material.ANVIL.createBlockData());
        fallingBlock.setHurtEntities(false);
        fallingBlock.setDropItem(false);
        fallingBlock.setMetadata(Deathbox.metadataKey, new FixedMetadataValue(plugin, this));
    }

public void spawn(Plugin plugin, Location location) {
    block = location.getBlock();
    block.setType(Material.CHEST);
    block.setMetadata(Deathbox.metadataKey, new FixedMetadataValue(plugin, this));

    var blockLocation = block.getLocation();
    var blockCenterLocation = blockLocation.add(blockLocation.getX() > 0 ? -0.5 : 0.5, 0.5, blockLocation.getZ() > 0 ? -0.5 : 0.5);
    hologram.spawn(plugin, blockCenterLocation, duration);

    if (duration != -1) {
        plugin.getServer().getScheduler()
            .runTaskLater(plugin, () -> unlocked = true, duration / 2);
        
        plugin.getServer().getScheduler()
            .runTaskLater(plugin, () -> despawn(plugin, false), duration);
    }
}


    public void despawn(Plugin plugin, boolean dropContents) {
        hologram.remove();

        if (block == null) return;
        block.setType(Material.AIR);
        block.removeMetadata(Deathbox.metadataKey, plugin);

        if (dropContents) inventory.forEach(item -> {
            if (item != null) block.getWorld().dropItemNaturally(block.getLocation(), item);
        });
    }

    public void openInventory(HumanEntity humanEntity) {
        humanEntity.openInventory(inventory);

    
    if (sealed && humanEntity instanceof Player player && player.equals(owner)) {
        sealed = false;
        player.giveExp(exp);
    }
}


    public Player getOwner() {
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

}
