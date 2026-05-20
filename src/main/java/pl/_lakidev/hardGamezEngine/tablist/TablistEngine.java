package pl._lakidev.hardGamezEngine.tablist;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TablistEngine {

    private static final Map<UUID, HardTablist> tablists = new HashMap<>();

    public static void init(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new TablistListener(), plugin);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /** Creates a tablist with a single-line header and footer. */
    public static HardTablist create(String id, Player player, String header, String footer) {
        return create(id, player, List.of(header), List.of(footer));
    }

    /** Creates a tablist with multi-line header and/or footer. */
    public static HardTablist create(String id, Player player, List<String> header, List<String> footer) {
        remove(player);
        HardTablist tablist = new HardTablist(id, player, header, footer);
        tablists.put(player.getUniqueId(), tablist);
        return tablist;
    }

    // -------------------------------------------------------------------------
    // Get / remove
    // -------------------------------------------------------------------------

    public static HardTablist get(Player player) {
        return tablists.get(player.getUniqueId());
    }

    public static boolean remove(Player player) {
        HardTablist tablist = tablists.remove(player.getUniqueId());
        if (tablist == null) return false;
        tablist.remove();
        return true;
    }

    public static void removeAll() {
        tablists.values().forEach(HardTablist::remove);
        tablists.clear();
    }

    // called from TablistListener when a player quits
    static void onQuit(UUID uuid) {
        HardTablist tablist = tablists.remove(uuid);
        if (tablist != null) tablist.remove();
    }

    private TablistEngine() {}
}
