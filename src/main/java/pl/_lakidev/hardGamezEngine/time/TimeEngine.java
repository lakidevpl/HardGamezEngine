package pl._lakidev.hardGamezEngine.time;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class TimeEngine {

    private final Plugin plugin;

    public TimeEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setTimeout(Runnable task, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    public void setInterval(Runnable task, long ticks) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, ticks, ticks);
    }
}
