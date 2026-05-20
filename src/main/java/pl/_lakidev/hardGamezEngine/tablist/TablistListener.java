package pl._lakidev.hardGamezEngine.tablist;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

class TablistListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        TablistEngine.onQuit(e.getPlayer().getUniqueId());
    }
}
