package pl._lakidev.hardGamezEngine.scoreboard;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardEngine {

    // one scoreboard per player
    private static final Map<UUID, HardScoreboard> scoreboards = new HashMap<>();

    /**
     * Creates (or replaces) a scoreboard for the given player.
     *
     * @param id     identifier — used to retrieve/remove the board later
     * @param player target player
     * @param title  sidebar title — supports &amp; color codes, {name}, {ping}, PlaceholderAPI
     * @param rows   list of row strings (top to bottom, max 15) — same placeholder support
     */
    public static HardScoreboard create(String id, Player player, String title, List<String> rows) {
        remove(player);
        HardScoreboard sb = new HardScoreboard(id, player, title, rows);
        scoreboards.put(player.getUniqueId(), sb);
        return sb;
    }

    /** Returns the active scoreboard for the player, or null. */
    public static HardScoreboard get(Player player) {
        return scoreboards.get(player.getUniqueId());
    }

    /** Removes and clears the scoreboard for the player. */
    public static boolean remove(Player player) {
        HardScoreboard sb = scoreboards.remove(player.getUniqueId());
        if (sb == null) return false;
        sb.remove();
        return true;
    }

    /** Removes all active scoreboards (call on plugin disable). */
    public static void removeAll() {
        scoreboards.values().forEach(HardScoreboard::remove);
        scoreboards.clear();
    }

    private ScoreboardEngine() {}
}
