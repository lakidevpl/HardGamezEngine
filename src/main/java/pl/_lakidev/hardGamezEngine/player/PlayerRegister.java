package pl._lakidev.hardGamezEngine.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerRegister implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerEngine.registerPlayer(event.getPlayer());
    }
}
