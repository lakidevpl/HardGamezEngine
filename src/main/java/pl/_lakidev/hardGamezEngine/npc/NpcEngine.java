package pl._lakidev.hardGamezEngine.npc;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcEngine {

    private static final Map<UUID, NpcObject> npcs = new HashMap<>();

    public static void init(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new NpcListener(), plugin);
    }

    public static NpcObject register(NpcType npcType, String name) {
        return new NpcObject(npcType, name);
    }

    public static NpcObject register(NpcType npcType, String name, Location spawnLocation) {
        return new NpcObject(npcType, name, spawnLocation);
    }

    public static NpcObject get(UUID id) {
        return npcs.get(id);
    }

    public static Collection<NpcObject> getAll() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    public static boolean remove(UUID id) {
        NpcObject npc = npcs.get(id);
        if (npc == null) return false;
        npc.remove();
        return true;
    }

    public static void removeAll() {
        for (NpcObject npc : new java.util.ArrayList<>(npcs.values())) {
            npc.remove();
        }
        npcs.clear();
    }

    static void trackNpc(NpcObject npc) {
        npcs.put(npc.getId(), npc);
    }

    static void untrack(UUID id) {
        npcs.remove(id);
    }

    static NpcObject getByEntityId(int entityId) {
        for (NpcObject npc : npcs.values()) {
            if (npc.getEntityId() == entityId) return npc;
        }
        return null;
    }

    private NpcEngine() {}
}
