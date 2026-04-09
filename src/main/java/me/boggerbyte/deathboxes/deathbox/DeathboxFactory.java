package me.boggerbyte.deathboxes.deathbox;

import me.boggerbyte.deathboxes.Main;
import me.boggerbyte.deathboxes.hologram.Hologram;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DeathboxFactory {
    private final Plugin plugin = Main.getInstance();

    private final String hologramLayout;
    private final boolean storeExp;
    private final boolean locked;
    private final boolean breakable;
    private final int duration;
    private final int unlockAfter;

    public DeathboxFactory(
            String hologramLayout,
            boolean storeExp,
            boolean locked,
            boolean breakable,
            int duration,
            int unlockAfter) {
        this.hologramLayout = hologramLayout;
        this.storeExp = storeExp;
        this.locked = locked;
        this.breakable = breakable;
        this.duration = duration;
        this.unlockAfter = unlockAfter;
    }

    public Deathbox create(Player owner) {
        var title = owner.getName() + (owner.getName().endsWith("s") ? "'" : "'s") + " " + "deathbox";
        var inventory = plugin.getServer().createInventory(null, 45, title);
        var contents = owner.getInventory().getContents();
        inventory.setContents(contents);

        var exp = storeExp ? owner.getTotalExperience() : 0;

        var hologramRawLines = hologramLayout.lines()
                .map(line -> line.replace("%player%", owner.getName() + (owner.getName().endsWith("s") ? "'" : "'s")))
                .toList();
        var hologram = new Hologram(hologramRawLines);

        return new Deathbox(owner, inventory, exp, locked, breakable, hologram, duration, unlockAfter);
    }
}
