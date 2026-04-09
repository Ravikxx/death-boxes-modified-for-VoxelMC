package me.boggerbyte.deathboxes;

import me.boggerbyte.deathboxes.deathbox.Deathbox;
import me.boggerbyte.deathboxes.deathbox.DeathboxFactory;
import me.boggerbyte.deathboxes.listeners.DeathboxEventsListener;
import me.boggerbyte.deathboxes.listeners.PlayerDeathListener;
import me.boggerbyte.deathboxes.utils.Logger;
import me.boggerbyte.deathboxes.listeners.InventoryClickListener;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.boggerbyte.deathboxes.hologram.Hologram;

public final class Main extends JavaPlugin {
    private static final Map<String, Deathbox> activeGraves = new LinkedHashMap<>();

    private static String toGraveKey(Location location) {
        var blockLocation = location.getBlock().getLocation();
        return blockLocation.getWorld().getName() + ":" + blockLocation.getBlockX() + ":" + blockLocation.getBlockY() + ":" + blockLocation.getBlockZ();
    }

    public static void addGrave(Deathbox grave) {
        var location = grave.getLocation();
        if (location == null) return;

        activeGraves.put(toGraveKey(location), grave);
    }

    public static void removeGrave(Deathbox grave) {
        var location = grave.getLocation();
        if (location == null) return;

        activeGraves.remove(toGraveKey(location));
    }

    public static Collection<Deathbox> getActiveGraves() {
        return activeGraves.values();
    }

    public static Deathbox getGraveAt(Location location) {
        return activeGraves.get(toGraveKey(location));
    }

    public static Deathbox getGraveByInventory(Inventory inventory) {
        for (var grave : activeGraves.values()) {
            if (grave.getInventory().equals(inventory)) {
                return grave;
            }
        }

        return null;
    }

    private static String getOwnerDisplayName(UUID ownerUUID) {
        var owner = Main.getInstance().getServer().getOfflinePlayer(ownerUUID);
        var name = owner.getName() == null ? ownerUUID.toString() : owner.getName();
        return name + (name.endsWith("s") ? "'" : "'s");
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
        var config = getConfig();
        var configRequiredFields = new String[]{
                "deathbox.hologramMeta-layout",
                "deathbox.duration",
                "deathbox.unlock-after",
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
        var hologramRawLines = ChatColor.translateAlternateColorCodes('&', config.getString("deathbox.hologram-layout"))
                .lines()
                .toList();

        var deathboxFactory = new DeathboxFactory(
                ChatColor.translateAlternateColorCodes('&', config.getString("deathbox.hologram-layout")),
                config.getBoolean("deathbox.store-exp"),
                config.getBoolean("deathbox.locked"),
                config.getBoolean("deathbox.breakable"),
                config.getInt("deathbox.duration"),
                config.getInt("deathbox.unlock-after"));
        getServer().getPluginManager().registerEvents(new DeathboxEventsListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(deathboxFactory), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        File gravesFile = new File(getDataFolder(), "graves.yml");
        if (gravesFile.exists()) {
            FileConfiguration storage = YamlConfiguration.loadConfiguration(gravesFile);

            for (String key : storage.getKeys(false)) {
                String worldName = storage.getString(key + ".world");
                int x = storage.getInt(key + ".x");
                int y = storage.getInt(key + ".y");
                int z = storage.getInt(key + ".z");
                UUID ownerUUID = UUID.fromString(storage.getString(key + ".owner"));
                boolean unlocked = storage.getBoolean(key + ".unlocked");
                long unlockAtMillis = storage.getLong(key + ".unlock-at", unlocked ? -1L : 0L);
                long despawnAtMillis = storage.getLong(key + ".despawn-at", 0L);

                var world = getServer().getWorld(worldName);
                if (world == null) continue;

                var location = new Location(world, x, y, z);
                var owner = getServer().getOfflinePlayer(ownerUUID);

                var inventory = getServer().createInventory(null, 45);
                List<ItemStack> items = (List<ItemStack>) storage.getList(key + ".items");
                if (items != null) inventory.setContents(items.toArray(new ItemStack[0]));

                var grave = new Deathbox(
                        owner,
                        inventory,
                        0,
                        config.getBoolean("deathbox.locked"),
                        config.getBoolean("deathbox.breakable"),
                        new Hologram(hologramRawLines.stream()
                                .map(line -> line.replace("%player%", getOwnerDisplayName(ownerUUID)))
                                .toList()),
                        config.getInt("deathbox.duration"),
                        config.getInt("deathbox.unlock-after"));
                grave.restore(this, location, unlocked, unlockAtMillis, despawnAtMillis);
            }
        }

    }

@Override
public void onDisable() {
    FileConfiguration storage = new YamlConfiguration();
    File gravesFile = new File(getDataFolder(), "graves.yml");

    int i = 0;
    for (Deathbox grave : activeGraves.values()) {
        var location = grave.getLocation();
        if (location == null) continue;

        storage.set(i + ".world", location.getWorld().getName());
        storage.set(i + ".x", location.getBlockX());
        storage.set(i + ".y", location.getBlockY());
        storage.set(i + ".z", location.getBlockZ());
        storage.set(i + ".owner", grave.getOwner().getUniqueId().toString());
        storage.set(i + ".unlocked", grave.isUnlocked());
        storage.set(i + ".unlock-at", grave.getUnlockAtMillis());
        storage.set(i + ".despawn-at", grave.getDespawnAtMillis());
        storage.set(i + ".items", Arrays.asList(grave.getInventory().getContents()));
        i++;
    }

    try { storage.save(gravesFile); } 
    catch (IOException e) { e.printStackTrace(); }
}


    public static Plugin getInstance() {
        return Main.getPlugin(Main.class);
    }
}
