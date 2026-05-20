package pl._lakidev.hardGamezEngine.player;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl._lakidev.hardGamezEngine.HardGamezEngine;

public class PlayerRegister implements Listener {

    private final HardGamezEngine plugin;

    public PlayerRegister(HardGamezEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // registerPlayer handles async loading for MongoDB internally
        PlayerEngine.registerPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Flush on async thread so the main thread is never blocked
        var player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
            PlayerEngine.onPlayerQuit(player)
        );
    }
}
