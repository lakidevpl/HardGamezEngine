package pl._lakidev.hardGamezEngine.hologram;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.lang.MsgEngine;

import java.util.*;
import java.util.function.Consumer;

public class HardHologram {

    private static final double LINE_SPACING = 0.3;
    private static final float ITEM_SCALE = 0.4f;

    private final String id;
    private final Location location;
    private List<HologramLine> lines;
    private Interaction interaction;

    // per-player raw line overrides (as Strings, same format as constructor input)
    private final Map<UUID, List<String>> playerRawLines = new HashMap<>();

    // per-player TextDisplay entities for TEXT lines (index = line index in parsed list)
    private final Map<UUID, Map<Integer, TextDisplay>> playerTextDisplays = new HashMap<>();

    // global ItemDisplay entities shared by all players (index = line index)
    private final Map<Integer, ItemDisplay> itemDisplays = new HashMap<>();

    private Consumer<Player> onRightClick;
    private Consumer<Player> onLeftClick;

    private BukkitTask placeholderTask;
    private BukkitTask rotationTask;
    private float rotationAngle = 0f;

    HardHologram(String id, Location location, List<String> rawLines) {
        this.id = id;
        this.location = location.clone();
        this.lines = parseLines(rawLines);
        spawnItemDisplays();
        spawnForOnlinePlayers();
        spawnInteractionIfNeeded();
        startTasks();
    }

    // -------------------------------------------------------------------------
    // Line parsing
    // -------------------------------------------------------------------------

    private static List<HologramLine> parseLines(List<String> raw) {
        List<HologramLine> result = new ArrayList<>(raw.size());
        for (String s : raw) result.add(HologramLine.parse(s));
        return result;
    }

    private static List<String> toRawStrings(List<HologramLine> parsed) {
        List<String> result = new ArrayList<>(parsed.size());
        for (HologramLine l : parsed) result.add(l.getRaw());
        return result;
    }

    // -------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------

    private void startTasks() {
        startPlaceholderUpdater();
        startRotationTask();
    }

    private void startPlaceholderUpdater() {
        if (!MsgEngine.isPlaceholderAPIEnabled()) return;
        placeholderTask = HardGamezEngine.getInstance().getServer().getScheduler()
            .runTaskTimer(HardGamezEngine.getInstance(), this::tickPlaceholders, 20L, 20L);
    }

    private void startRotationTask() {
        if (itemDisplays.isEmpty()) return;
        rotationTask = HardGamezEngine.getInstance().getServer().getScheduler()
            .runTaskTimer(HardGamezEngine.getInstance(), this::tickRotation, 1L, 1L);
    }

    private void tickPlaceholders() {
        for (Player player : HardGamezEngine.getInstance().getServer().getOnlinePlayers()) {
            Map<Integer, TextDisplay> pDisplays = playerTextDisplays.get(player.getUniqueId());
            if (pDisplays == null) continue;
            List<HologramLine> playerLines = getPlayerLines(player);
            for (Map.Entry<Integer, TextDisplay> entry : pDisplays.entrySet()) {
                int idx = entry.getKey();
                if (idx >= playerLines.size()) continue;
                HologramLine line = playerLines.get(idx);
                if (line.getType() != HologramLine.Type.TEXT) continue;
                String resolved = MsgEngine.applyPlaceholders(player, line.getRaw());
                entry.getValue().text(LegacyComponentSerializer.legacyAmpersand().deserialize(resolved));
            }
        }
    }

    private void tickRotation() {
        rotationAngle = (rotationAngle + 2f) % 360f;
        // Combine: upright correction (no X tilt) + Y spin
        Quaternionf rotation = new Quaternionf()
            .rotateY((float) Math.toRadians(rotationAngle));
        Transformation t = new Transformation(
            new Vector3f(0, 0, 0),
            rotation,
            new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
            new Quaternionf()
        );
        for (ItemDisplay display : itemDisplays.values()) {
            if (!display.isDead()) display.setTransformation(t);
        }
    }

    // -------------------------------------------------------------------------
    // Y position helpers
    // -------------------------------------------------------------------------

    private double[] computeYPositions(List<HologramLine> parsed) {
        double[] yPos = new double[parsed.size()];
        double topY = location.getY() + (parsed.size() - 1) * LINE_SPACING;
        for (int i = 0; i < parsed.size(); i++) yPos[i] = topY - i * LINE_SPACING;
        return yPos;
    }

    // -------------------------------------------------------------------------
    // Spawn / despawn — global ItemDisplays
    // -------------------------------------------------------------------------

    private void spawnItemDisplays() {
        double[] yPos = computeYPositions(lines);
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            if (line.getType() != HologramLine.Type.ITEM) continue;
            final double y = yPos[i];
            final ItemStack item = new ItemStack(line.getMaterial());
            Location loc = location.clone();
            loc.setY(y);
            loc.setYaw(0f);
            loc.setPitch(0f);
            ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
                d.setItemStack(item);
                d.setBillboard(Display.Billboard.FIXED);
                d.setPersistent(false);
                d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                    new Quaternionf()
                ));
            });
            itemDisplays.put(i, display);
        }
    }

    private void removeItemDisplays() {
        itemDisplays.values().forEach(d -> { if (!d.isDead()) d.remove(); });
        itemDisplays.clear();
    }

    // -------------------------------------------------------------------------
    // Spawn / despawn — per-player TextDisplays
    // -------------------------------------------------------------------------

    private void spawnForOnlinePlayers() {
        for (Player player : HardGamezEngine.getInstance().getServer().getOnlinePlayers()) {
            spawnTextDisplaysForPlayer(player);
        }
    }

    void spawnForPlayer(Player player) {
        if (playerTextDisplays.containsKey(player.getUniqueId())) return;
        spawnTextDisplaysForPlayer(player);
    }

    private void spawnTextDisplaysForPlayer(Player player) {
        List<HologramLine> playerLines = getPlayerLines(player);
        double[] yPos = computeYPositions(playerLines);
        Map<Integer, TextDisplay> pDisplays = new HashMap<>();
        for (int i = 0; i < playerLines.size(); i++) {
            HologramLine line = playerLines.get(i);
            if (line.getType() != HologramLine.Type.TEXT) continue;
            String resolved = MsgEngine.applyPlaceholders(player, line.getRaw());
            final String text = resolved;
            final double y = yPos[i];
            Location loc = location.clone();
            loc.setY(y);
            TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
                d.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setSeeThrough(false);
                d.setPersistent(false);
                d.setVisibleByDefault(false);
            });
            player.showEntity(HardGamezEngine.getInstance(), display);
            pDisplays.put(i, display);
        }
        playerTextDisplays.put(player.getUniqueId(), pDisplays);
    }

    private void removeTextDisplaysForPlayer(Player player) {
        Map<Integer, TextDisplay> old = playerTextDisplays.remove(player.getUniqueId());
        if (old != null) old.values().forEach(d -> { if (!d.isDead()) d.remove(); });
    }

    private List<HologramLine> getPlayerLines(Player player) {
        List<String> override = playerRawLines.get(player.getUniqueId());
        return override != null ? parseLines(override) : lines;
    }

    // -------------------------------------------------------------------------
    // Interaction entity
    // -------------------------------------------------------------------------

    private void spawnInteractionIfNeeded() {
        if (onRightClick == null && onLeftClick == null) return;
        float height = (float) Math.max(0.5, (lines.size() - 1) * LINE_SPACING + 0.3);
        interaction = location.getWorld().spawn(location.clone(), Interaction.class, i -> {
            i.setInteractionWidth(1.0f);
            i.setInteractionHeight(height);
            i.setPersistent(false);
        });
        HologramEngine.registerInteraction(interaction, this);
    }

    private void removeInteraction() {
        if (interaction == null) return;
        HologramEngine.unregisterInteraction(interaction.getUniqueId());
        if (!interaction.isDead()) interaction.remove();
        interaction = null;
    }

    // -------------------------------------------------------------------------
    // Full despawn
    // -------------------------------------------------------------------------

    public void despawn() {
        if (placeholderTask != null) { placeholderTask.cancel(); placeholderTask = null; }
        if (rotationTask    != null) { rotationTask.cancel();    rotationTask = null;    }
        removeItemDisplays();
        playerTextDisplays.values().forEach(map -> map.values().forEach(d -> { if (!d.isDead()) d.remove(); }));
        playerTextDisplays.clear();
        playerRawLines.clear();
        removeInteraction();
    }

    // -------------------------------------------------------------------------
    // Global edit
    // -------------------------------------------------------------------------

    public HardHologram setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, HologramLine.parse(text));
            refreshDisplays();
        }
        return this;
    }

    public HardHologram addLine(String text) {
        lines.add(HologramLine.parse(text));
        refreshDisplays();
        return this;
    }

    public HardHologram removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            refreshDisplays();
        }
        return this;
    }

    public HardHologram update(List<String> newLines) {
        lines = parseLines(newLines);
        refreshDisplays();
        return this;
    }

    private void refreshDisplays() {
        if (rotationTask != null) { rotationTask.cancel(); rotationTask = null; }
        removeItemDisplays();
        spawnItemDisplays();
        startRotationTask();

        for (Player player : HardGamezEngine.getInstance().getServer().getOnlinePlayers()) {
            removeTextDisplaysForPlayer(player);
            spawnTextDisplaysForPlayer(player);
        }
        removeInteraction();
        spawnInteractionIfNeeded();
    }

    // -------------------------------------------------------------------------
    // Per-player edit
    // -------------------------------------------------------------------------

    public PlayerHologramEditor editForPlayer(Player player) {
        List<String> base = playerRawLines.getOrDefault(player.getUniqueId(), toRawStrings(lines));
        return new PlayerHologramEditor(this, player, new ArrayList<>(base));
    }

    void applyForPlayer(Player player, List<String> newRawLines) {
        playerRawLines.put(player.getUniqueId(), new ArrayList<>(newRawLines));
        removeTextDisplaysForPlayer(player);
        spawnTextDisplaysForPlayer(player);
    }

    public void resetForPlayer(Player player) {
        playerRawLines.remove(player.getUniqueId());
        removeTextDisplaysForPlayer(player);
        if (player.isOnline()) spawnTextDisplaysForPlayer(player);
    }

    boolean hasPlayerOverride(UUID uuid) { return playerRawLines.containsKey(uuid); }

    // -------------------------------------------------------------------------
    // Click handlers
    // -------------------------------------------------------------------------

    public HardHologram onClick(Consumer<Player> handler) {
        onRightClick = handler;
        onLeftClick = handler;
        removeInteraction();
        spawnInteractionIfNeeded();
        return this;
    }

    public HardHologram onRightClick(Consumer<Player> handler) {
        onRightClick = handler;
        removeInteraction();
        spawnInteractionIfNeeded();
        return this;
    }

    public HardHologram onLeftClick(Consumer<Player> handler) {
        onLeftClick = handler;
        removeInteraction();
        spawnInteractionIfNeeded();
        return this;
    }

    void fireRightClick(Player player) { if (onRightClick != null) onRightClick.accept(player); }
    void fireLeftClick(Player player)  { if (onLeftClick  != null) onLeftClick.accept(player);  }

    // -------------------------------------------------------------------------
    // Move
    // -------------------------------------------------------------------------

    public void move(Location newLocation) {
        despawn();
        location.setWorld(newLocation.getWorld());
        location.setX(newLocation.getX());
        location.setY(newLocation.getY());
        location.setZ(newLocation.getZ());
        spawnItemDisplays();
        spawnForOnlinePlayers();
        spawnInteractionIfNeeded();
        startTasks();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getId()            { return id; }
    public Location getLocation()    { return location.clone(); }
    public List<String> getLines()   { return toRawStrings(lines); }

    // -------------------------------------------------------------------------
    // PlayerHologramEditor
    // -------------------------------------------------------------------------

    public static class PlayerHologramEditor {

        private final HardHologram holo;
        private final Player player;
        private final List<String> editLines;

        PlayerHologramEditor(HardHologram holo, Player player, List<String> editLines) {
            this.holo = holo;
            this.player = player;
            this.editLines = editLines;
        }

        public PlayerHologramEditor setLine(int index, String text) {
            if (index >= 0 && index < editLines.size()) editLines.set(index, text);
            return this;
        }

        public PlayerHologramEditor addLine(String text) {
            editLines.add(text);
            return this;
        }

        public PlayerHologramEditor removeLine(int index) {
            if (index >= 0 && index < editLines.size()) editLines.remove(index);
            return this;
        }

        public HardHologram apply() {
            holo.applyForPlayer(player, editLines);
            return holo;
        }
    }
}
