package pl._lakidev.hardGamezEngine.scoreboard;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.lang.MsgEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HardScoreboard {

    // Unique fake player names for each row slot (max 15 rows)
    private static final String[] ENTRIES = {
        "§0§r", "§1§r", "§2§r", "§3§r", "§4§r",
        "§5§r", "§6§r", "§7§r", "§8§r", "§9§r",
        "§a§r", "§b§r", "§c§r", "§d§r", "§e§r"
    };

    private final String id;
    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;

    private String rawTitle;
    private final List<String> rawRows;

    private BukkitTask updateTask;

    HardScoreboard(String id, Player player, String title, List<String> rows) {
        this.id = id;
        this.player = player;
        this.rawTitle = title;
        this.rawRows = new ArrayList<>(rows);

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("sidebar", "dummy",
            LegacyComponentSerializer.legacyAmpersand().deserialize(resolveTitle()));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        renderRows();
        player.setScoreboard(scoreboard);
        startUpdater();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public HardScoreboard setTitle(String title) {
        rawTitle = title;
        objective.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(resolveTitle()));
        return this;
    }

    public HardScoreboard setRow(int index, String text) {
        if (index >= 0 && index < rawRows.size()) {
            rawRows.set(index, text);
            renderRows();
        }
        return this;
    }

    public HardScoreboard setRows(List<String> rows) {
        rawRows.clear();
        rawRows.addAll(rows);
        renderRows();
        return this;
    }

    public void remove() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
        if (player.isOnline()) player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }

    public String getId() { return id; }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void renderRows() {
        // remove stale entries beyond current row count
        for (int i = rawRows.size(); i < ENTRIES.length; i++) {
            scoreboard.resetScores(ENTRIES[i]);
        }

        int size = Math.min(rawRows.size(), ENTRIES.length);
        for (int i = 0; i < size; i++) {
            String resolved = resolveRow(rawRows.get(i));
            String entry = ENTRIES[i];

            // Team splits the resolved text into prefix + suffix to bypass 16-char limit
            String teamName = "row_" + i;
            Team team = scoreboard.getTeam(teamName);
            if (team == null) team = scoreboard.registerNewTeam(teamName);

            String[] parts = split(resolved);
            team.setPrefix(parts[0]);
            team.setSuffix(parts[1]);

            if (!team.hasEntry(entry)) team.addEntry(entry);

            // score = rows displayed top-to-bottom: highest score first
            objective.getScore(entry).setScore(size - i);
        }
    }

    private void tickUpdate() {
        objective.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(resolveTitle()));
        renderRows();
    }

    private void startUpdater() {
        // 1 tick = ~50ms, so this refreshes every tick — smooth for ping/placeholders
        updateTask = Bukkit.getScheduler().runTaskTimer(
            HardGamezEngine.getInstance(), this::tickUpdate, 1L, 1L);
    }

    // -------------------------------------------------------------------------
    // Text resolution
    // -------------------------------------------------------------------------

    private String resolveTitle() {
        return resolve(rawTitle);
    }

    private String resolveRow(String raw) {
        return resolve(raw)
            .replace("{name}", player.getName())
            .replace("{ping}", String.valueOf(player.getPing()));
    }

    private String resolve(String text) {
        if (text == null) return "";
        String result = ChatColor.translateAlternateColorCodes('&', text);
        if (MsgEngine.isPlaceholderAPIEnabled()) {
            result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
        }
        return result;
    }

    // Splits a string into prefix (max 16 chars) + suffix (rest, max 16 chars)
    // preserves last color code across the split boundary
    private static String[] split(String text) {
        if (text.length() <= 16) return new String[]{text, ""};

        String prefix = text.substring(0, 16);
        String suffix = text.substring(16);

        // carry trailing color code into suffix so colors don't break
        String lastColor = ChatColor.getLastColors(prefix);
        if (suffix.length() > 16 - lastColor.length()) {
            suffix = lastColor + suffix.substring(0, 16 - lastColor.length());
        } else {
            suffix = lastColor + suffix;
        }
        return new String[]{prefix, suffix};
    }
}
