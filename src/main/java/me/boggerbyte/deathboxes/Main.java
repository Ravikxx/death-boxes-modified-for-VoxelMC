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
        
        var world = getServer().getWorld(worldName);
        if (world == null) continue;
        
        var location = new Location(world, x, y, z);
        var owner = getServer().getOfflinePlayer(ownerUUID);
        
        var inventory = getServer().createInventory(null, 45);
        List<ItemStack> items = (List<ItemStack>) storage.getList(key + ".items");
        if (items != null) inventory.setContents(items.toArray(new ItemStack[0]));

        var grave = new Deathbox(owner, inventory, 0, !unlocked, false, new Hologram(List.of()), -1);
        grave.spawn(this, location);
    }
}

    }

@Override
public void onDisable() {
    FileConfiguration storage = new YamlConfiguration();
    File gravesFile = new File(getDataFolder(), "graves.yml");

    int i = 0;
    for (Deathbox grave : activeGraves) {
        storage.set(i + ".world", grave.getBlock().getWorld().getName());
        storage.set(i + ".x", grave.getBlock().getX());
        storage.set(i + ".y", grave.getBlock().getY());
        storage.set(i + ".z", grave.getBlock().getZ());
        storage.set(i + ".owner", grave.getOwner().getUniqueId().toString());
        storage.set(i + ".unlocked", grave.isUnlocked());
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
