package pl._lakidev.hardGamezEngine.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.lang.MsgEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HardTablist {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final String id;
    private final Player viewer;

    private List<String> rawHeader;
    private List<String> rawFooter;

    // null = show all online players (default behaviour)
    private Set<UUID> customPlayers = null;

    private BukkitTask updateTask;

    HardTablist(String id, Player viewer, List<String> header, List<String> footer) {
        this.id = id;
        this.viewer = viewer;
        this.rawHeader = new ArrayList<>(header);
        this.rawFooter = new ArrayList<>(footer);
        startUpdater();
        tick();
    }

    // -------------------------------------------------------------------------
    // Public API — header / footer
    // -------------------------------------------------------------------------

    /** Replace the entire header with a single line. */
    public HardTablist setHeader(String line) {
        rawHeader = new ArrayList<>(List.of(line));
        tick();
        return this;
    }

    /** Replace the entire header with multiple lines (joined with \n). */
    public HardTablist setHeader(List<String> lines) {
        rawHeader = new ArrayList<>(lines);
        tick();
        return this;
    }

    /** Replace the entire footer with a single line. */
    public HardTablist setFooter(String line) {
        rawFooter = new ArrayList<>(List.of(line));
        tick();
        return this;
    }

    /** Replace the entire footer with multiple lines (joined with \n). */
    public HardTablist setFooter(List<String> lines) {
        rawFooter = new ArrayList<>(lines);
        tick();
        return this;
    }

    // -------------------------------------------------------------------------
    // Public API — player list
    // -------------------------------------------------------------------------

    /**
     * Sets a custom player list shown in the tab for this viewer.
     * Only the listed players will be visible; everyone else is hidden.
     */
    public HardTablist setPlayers(Collection<Player> players) {
        customPlayers = new LinkedHashSet<>();
        for (Player p : players) customPlayers.add(p.getUniqueId());
        applyPlayerVisibility();
        return this;
    }

    /** Adds a player to the custom list (enables custom mode if not already active). */
    public HardTablist addPlayer(Player player) {
        if (customPlayers == null) customPlayers = new LinkedHashSet<>();
        customPlayers.add(player.getUniqueId());
        applyPlayerVisibility();
        return this;
    }

    /** Removes a player from the custom list. */
    public HardTablist removePlayer(Player player) {
        if (customPlayers != null) {
            customPlayers.remove(player.getUniqueId());
            applyPlayerVisibility();
        }
        return this;
    }

    /**
     * Resets to default: show all online players.
     * Reveals every player back to the viewer.
     */
    public HardTablist resetPlayers() {
        customPlayers = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(viewer)) viewer.showPlayer(HardGamezEngine.getInstance(), p);
        }
        return this;
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    public void remove() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
        // restore blank header/footer
        if (viewer.isOnline()) viewer.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        // reveal all players
        if (viewer.isOnline()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(viewer)) viewer.showPlayer(HardGamezEngine.getInstance(), p);
            }
        }
    }

    public String getId() { return id; }

    // -------------------------------------------------------------------------
    // Internal tick
    // -------------------------------------------------------------------------

    private void startUpdater() {
        updateTask = Bukkit.getScheduler().runTaskTimer(
            HardGamezEngine.getInstance(), this::tick, 1L, 1L);
    }

    private void tick() {
        if (!viewer.isOnline()) return;
        viewer.sendPlayerListHeaderAndFooter(buildComponent(rawHeader), buildComponent(rawFooter));
    }

    private Component buildComponent(List<String> lines) {
        if (lines.isEmpty()) return Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(resolve(lines.get(i)));
        }
        return LEGACY.deserialize(sb.toString());
    }

    private String resolve(String text) {
        if (text == null) return "";
        String result = ChatColor.translateAlternateColorCodes('&', text);
        result = result.replace("{name}", viewer.getName())
                       .replace("{ping}", String.valueOf(viewer.getPing()));
        if (MsgEngine.isPlaceholderAPIEnabled()) {
            result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, result);
        }
        return result;
    }

    private void applyPlayerVisibility() {
        if (!viewer.isOnline()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(viewer)) continue;
            if (customPlayers == null || customPlayers.contains(p.getUniqueId())) {
                viewer.showPlayer(HardGamezEngine.getInstance(), p);
            } else {
                viewer.hidePlayer(HardGamezEngine.getInstance(), p);
            }
        }
    }
}
