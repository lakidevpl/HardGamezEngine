package pl._lakidev.hardGamezEngine.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HologramEngine {

    private static final Map<String, HardHologram> holograms = new HashMap<>();
    private static final Map<UUID, HardHologram> interactionMap = new HashMap<>();

    public static void init(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new HologramListener(), plugin);
    }

    public static HardHologram create(String id, Location location, List<String> lines) {
        if (holograms.containsKey(id)) holograms.get(id).despawn();
        HardHologram holo = new HardHologram(id, location, lines);
        holograms.put(id, holo);
        return holo;
    }

    public static HardHologram get(String id) {
        return holograms.get(id);
    }

    public static boolean remove(String id) {
        HardHologram holo = holograms.remove(id);
        if (holo == null) return false;
        holo.despawn();
        return true;
    }

    public static void removeAll() {
        holograms.values().forEach(HardHologram::despawn);
        holograms.clear();
        interactionMap.clear();
    }

    public static void spawnForPlayer(Player player) {
        for (HardHologram holo : holograms.values()) holo.spawnForPlayer(player);
    }

    public static void cleanupPlayer(Player player) {
        for (HardHologram holo : holograms.values()) holo.resetForPlayer(player);
    }

    static void registerInteraction(Interaction entity, HardHologram holo) {
        interactionMap.put(entity.getUniqueId(), holo);
    }

    static void unregisterInteraction(UUID entityId) {
        interactionMap.remove(entityId);
    }

    public static HardHologram getByInteraction(Entity entity) {
        return interactionMap.get(entity.getUniqueId());
    }

    private HologramEngine() {}
}
