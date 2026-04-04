package me.boggerbyte.deathboxes;

import me.boggerbyte.deathboxes.deathbox.DeathboxFactory;
import me.boggerbyte.deathboxes.listeners.DeathboxEventsListener;
import me.boggerbyte.deathboxes.listeners.PlayerDeathListener;
import me.boggerbyte.deathboxes.utils.Logger;
import me.boggerbyte.deathboxes.listeners.InventoryClickListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    private static final List<Deathbox> activeGraves = new ArrayList<>();

    public static void addGrave(Deathbox grave) {
        activeGraves.add(grave);
    }

    public static void removeGrave(Deathbox grave) {
        activeGraves.remove(grave);
    }

    public static List<Deathbox> getActiveGraves() {
        return activeGraves;
    }
    @Override
    public void onLoad() {
        saveDefaultConfig();
        var config = getConfig();
        var configRequiredFields = new String[]{
                "deathbox.hologramMeta-layout",
                "deathbox.duration",
                "deathbox.store-exp", // used
                "deathbox.locked",
                "deathbox.breakable",
                "lifecycle.spawn.if-empty", // implement
                "lifecycle.spawn.effect", // implement
                "lifecycle.land.effect", // implement
                "lifecycle.despawn.if-server-reloads", // implement
                "lifecycle.despawn.if-empty", // implement
                "lifecycle.despawn.drop-items", // implement
                "lifecycle.despawn.effect", // implement
                "lifecycle.destroy.if-server-reloads", // implement
                "lifecycle.destroy.drop-items", // implement
                "lifecycle.destroy.effect", // implement
        };
        Arrays.stream(configRequiredFields)
                .filter(field -> !config.contains(field, true))
                .forEach(field -> Logger.log(Level.WARNING, "Missing required config field <" + field + ">. Using default value"));
    }

    @Override
    public void onEnable() {
        var config = getConfig();

        var deathboxFactory = new DeathboxFactory(
                ChatColor.translateAlternateColorCodes('&', config.getString("deathbox.hologram-layout")),
                config.getBoolean("deathbox.store-exp"),
                config.getBoolean("deathbox.locked"),
                config.getBoolean("deathbox.breakable"),
                config.getInt("deathbox.duration"));
        getServer().getPluginManager().registerEvents(new DeathboxEventsListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(deathboxFactory), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
    }

    @Override
    public void onDisable() {
        FileConfiguration storage = new YamlConfiguration();

for (Deathbox grave : activeGraves) {
    String id = UUID.randomUUID().toString();
    storage.set(id + ".world", grave.getBlock().getWorld().getName());
    storage.set(id + ".x", grave.getBlock().getX());
    storage.set(id + ".y", grave.getBlock().getY());
    storage.set(id + ".z", grave.getBlock().getZ());
    storage.set(id + ".owner", grave.getOwner().getUniqueId().toString());
    storage.set(id + ".unlocked", grave.isUnlocked());
    storage.set(id + ".items", grave.getInventory().getContents());
}

storage.save(new File(getDataFolder(), "graves.yml"));

    }

    public static Plugin getInstance() {
        return Main.getPlugin(Main.class);
    }
}
