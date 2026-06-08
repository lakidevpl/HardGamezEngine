package pl._lakidev.hardGamezEngine.npc;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class NpcListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (NpcObject npc : NpcEngine.getAll()) {
            npc.sendSpawnTo(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        NpcObject npc = NpcEngine.getByEntityId(entity.getEntityId());
        if (npc == null) return;

        event.setCancelled(true);
        npc.handleClick(event.getPlayer(), NpcClickEvent.ClickType.RIGHT);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        NpcObject npc = NpcEngine.getByEntityId(event.getEntity().getEntityId());
        if (npc == null) return;

        event.setCancelled(true);
        npc.handleClick(player, NpcClickEvent.ClickType.LEFT);
    }
}
