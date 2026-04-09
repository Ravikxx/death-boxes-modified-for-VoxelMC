package me.boggerbyte.deathboxes.hologram;


import me.boggerbyte.deathboxes.tasks.RenderHologramTimerTask;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hologram {
    private final List<String> rawLines;

    public Hologram(List<String> rawLines) {
        this.rawLines = rawLines;
    }

    private List<Entity> lines = new ArrayList<>();
    private List<BukkitRunnable> tasks = new ArrayList<>();

    public void spawn(Plugin plugin, Location location, int duration) {
        var reversedRawLines = new ArrayList<>(rawLines);
        Collections.reverse(reversedRawLines);

        if (reversedRawLines.isEmpty()) {
            lines.clear();
            tasks.forEach(BukkitRunnable::cancel);
            tasks.clear();
            return;
        }

        List<Entity> lines = new ArrayList<>();
        for (String reversedRawLine : reversedRawLines) {
            var line = location.getWorld().spawn(location, AreaEffectCloud.class);
            line.setWaitTime(0);
            line.setRadius(0);
            line.setDuration(Integer.MAX_VALUE);
            line.setCustomNameVisible(true);
            line.setCustomName(reversedRawLine);
            lines.add(line);
        }

        var linesIterator = lines.iterator();
        var mount = linesIterator.next();
        while (linesIterator.hasNext()) {
            var nextLine = linesIterator.next();
            mount.addPassenger(nextLine);
            mount = nextLine;
        }

        if (duration != -1) {
            var rawTimerLines = reversedRawLines.stream()
                    .filter(line -> line.contains("%timer%"))
                    .toList();
            var timerLines = lines.stream()
                    .filter(line -> rawTimerLines.contains(line.getCustomName()))
                    .toList();
            List<BukkitRunnable> tasks = new ArrayList<>();
            for (int i = 0; i < timerLines.size(); i++) {
                var task = new RenderHologramTimerTask(timerLines.get(i), rawTimerLines.get(i), duration / 20);
                task.runTaskTimer(plugin, 0, 20);
                tasks.add(task);
            }

            this.tasks = tasks;
        }

        this.lines = lines;
    }

    public void remove() {
        lines.forEach(Entity::remove);
        lines.clear();
        tasks.forEach(BukkitRunnable::cancel);
        tasks.clear();
    }

    public boolean isSpawned() {
        return !lines.isEmpty();
    }
}
