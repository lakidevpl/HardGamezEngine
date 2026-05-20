package pl._lakidev.hardGamezEngine.hologram;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HologramListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent e) {
        HardHologram holo = HologramEngine.getByInteraction(e.getRightClicked());
        if (holo == null) return;
        e.setCancelled(true);
        holo.fireRightClick(e.getPlayer());
    }

    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        HardHologram holo = HologramEngine.getByInteraction(e.getEntity());
        if (holo == null) return;
        e.setCancelled(true);
        holo.fireLeftClick(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        HologramEngine.spawnForPlayer(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        HologramEngine.cleanupPlayer(e.getPlayer());
    }
}
