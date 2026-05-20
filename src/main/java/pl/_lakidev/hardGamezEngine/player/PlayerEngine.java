package pl._lakidev.hardGamezEngine.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.lang.LangData;
import pl._lakidev.hardGamezEngine.lang.LangEngine;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class PlayerEngine {

    public enum StorageType { YAML, JSON, MONGODB }

    private static StorageType storageType;
    private static File dataFile;
    private static HardGamezEngine plugin;

    // YAML/JSON in-memory state
    private static YamlConfiguration yamlData;
    private static Map<String, Map<String, Object>> jsonData;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // MongoDB
    private static MongoClient mongoClient;
    private static MongoCollection<Document> mongoCollection;

    private static String defaultLang;
    private static boolean registerIp;

    // ── RAM cache: UUID → Document (MongoDB) or flat map wrapped in Document ──
    // ConcurrentHashMap for thread safety without global locks
    private static final ConcurrentHashMap<UUID, Document> ramCache = new ConcurrentHashMap<>();

    // Dirty set: UUIDs whose cached Document differs from what's persisted
    private static final ConcurrentHashMap<UUID, Boolean> dirtySet = new ConcurrentHashMap<>();

    // Flush guard: prevents two concurrent flushes racing over the same UUID
    private static final ConcurrentHashMap<UUID, Boolean> flushInProgress = new ConcurrentHashMap<>();

    // Debug metrics
    private static final AtomicInteger cacheHits = new AtomicInteger(0);
    private static final AtomicInteger cacheMisses = new AtomicInteger(0);
    private static final AtomicInteger dbWrites = new AtomicInteger(0);
    private static final AtomicLong lastFlushMs = new AtomicLong(0);

    // ── Flush task handle so we can cancel on shutdown ──
    private static int flushTaskId = -1;

    // ── Flush interval: 6 seconds (120 ticks) ──
    private static final long FLUSH_INTERVAL_TICKS = 120L;

    public static void init(HardGamezEngine pluginInstance) {
        plugin = pluginInstance;
        String type = plugin.getMainConfig().getString("data_storage.type", "YAML").toUpperCase();
        storageType = StorageType.valueOf(type);
        registerIp = plugin.getMainConfig().getBoolean("data_storage.register_ip_addresses", true);
        defaultLang = plugin.getMainConfig().getString("language.default", "pl");

        File root = new File(plugin.getServer().getWorldContainer(), "hardgamez");
        if (!root.exists()) root.mkdirs();

        if (storageType == StorageType.YAML) {
            dataFile = new File(root, "playersdata.yml");
            if (!dataFile.exists()) { try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); } }
            yamlData = YamlConfiguration.loadConfiguration(dataFile);

        } else if (storageType == StorageType.JSON) {
            dataFile = new File(root, "playersdata.json");
            loadJson();

        } else if (storageType == StorageType.MONGODB) {
            String host = plugin.getMainConfig().getString("database.host", "localhost");
            int port = plugin.getMainConfig().getInt("database.port", 27017);
            String username = plugin.getMainConfig().getString("database.username", "");
            String password = plugin.getMainConfig().getString("database.password", "");
            String database = plugin.getMainConfig().getString("database.database", "hardgamez");
            try {
                String uri = !username.isEmpty()
                    ? "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + database
                    : "mongodb://" + host + ":" + port;
                mongoClient = MongoClients.create(uri);
                MongoDatabase db = mongoClient.getDatabase(database);
                mongoCollection = db.getCollection("players");
                // Ensure UUID index exists
                mongoCollection.createIndex(new Document("uuid", 1), new IndexOptions().unique(true));
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7PlayerEngine -> MongoDB connection failed, falling back to YAML!");
                plugin.getLogger().log(Level.SEVERE, "Details:", e);
                storageType = StorageType.YAML;
                dataFile = new File(root, "playersdata.yml");
                if (!dataFile.exists()) { try { dataFile.createNewFile(); } catch (IOException ex) { ex.printStackTrace(); } }
                yamlData = YamlConfiguration.loadConfiguration(dataFile);
            }
        }

        startFlushTask();
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────

    public static void registerPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String uuidStr = uuid.toString();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String ip = registerIp && player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";

        // Already in RAM cache → already registered
        if (ramCache.containsKey(uuid)) return;

        if (storageType == StorageType.MONGODB) {
            // Async: check DB then insert if new, then cache
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (mongoCollection == null) return;
                try {
                    Document existing = mongoCollection.find(Filters.eq("uuid", uuidStr)).first();
                    if (existing != null) {
                        ramCache.put(uuid, existing);
                        return;
                    }
                    Document doc = new Document("uuid", uuidStr)
                        .append("nick", player.getName())
                        .append("first-join", now)
                        .append("language", defaultLang)
                        .append("chosen_lang", false);
                    if (registerIp) doc.append("ip", ip);
                    mongoCollection.insertOne(doc);
                    ramCache.put(uuid, doc);
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7PlayerEngine -> Failed to register player " + uuidStr);
                    plugin.getLogger().log(Level.WARNING, "Details:", e);
                }
            });

        } else if (storageType == StorageType.JSON) {
            if (jsonData.containsKey(uuidStr)) {
                // Populate RAM cache from existing JSON data
                ramCache.put(uuid, mapToDocument(jsonData.get(uuidStr)));
                return;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("nick", player.getName());
            data.put("first-join", now);
            data.put("language", defaultLang);
            data.put("chosen_lang", false);
            if (registerIp) data.put("ip", ip);
            jsonData.put(uuidStr, data);
            ramCache.put(uuid, mapToDocument(data));
            dirtySet.put(uuid, Boolean.TRUE);

        } else { // YAML
            if (yamlData.contains(uuidStr)) {
                ramCache.put(uuid, yamlSectionToDocument(uuid));
                return;
            }
            yamlData.set(uuidStr + ".nick", player.getName());
            yamlData.set(uuidStr + ".first-join", now);
            yamlData.set(uuidStr + ".language", defaultLang);
            yamlData.set(uuidStr + ".chosen_lang", false);
            if (registerIp) yamlData.set(uuidStr + ".ip", ip);
            Document doc = new Document("nick", player.getName())
                .append("first-join", now)
                .append("language", defaultLang)
                .append("chosen_lang", false);
            if (registerIp) doc.append("ip", ip);
            ramCache.put(uuid, doc);
            dirtySet.put(uuid, Boolean.TRUE);
        }
    }

    /** Called on PlayerQuitEvent: flush dirty data synchronously then evict from cache. */
    public static void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        flushPlayer(uuid);
        ramCache.remove(uuid);
        dirtySet.remove(uuid);
        flushInProgress.remove(uuid);
    }

    // ── Public read API (100% backwards-compatible) ───────────────────────────

    public static ConfigurationSection getPlayer(UUID uuid) {
        Document doc = getCachedDoc(uuid);
        if (doc == null) return null;
        return documentToSection(doc);
    }

    public static ConfigurationSection getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public static int getInt(UUID uuid, String path, int def) {
        Document doc = getCachedDoc(uuid);
        if (doc == null) return def;
        Object val = doc.get(path);
        if (val instanceof Number) return ((Number) val).intValue();
        return def;
    }

    public static boolean getBoolean(UUID uuid, String path, boolean def) {
        Document doc = getCachedDoc(uuid);
        if (doc == null) return def;
        Object val = doc.get(path);
        if (val instanceof Boolean) return (Boolean) val;
        return def;
    }

    public static String getString(UUID uuid, String path, String def) {
        Document doc = getCachedDoc(uuid);
        if (doc == null) return def;
        Object val = doc.get(path);
        if (val instanceof String) return (String) val;
        return def;
    }

    public static LangData getPlayerLang(UUID uuid) {
        Document doc = getCachedDoc(uuid);
        String lang = defaultLang;
        if (doc != null) {
            Object l = doc.get("language");
            if (l instanceof String) lang = (String) l;
        }
        LangData data = LangEngine.getLangData(lang);
        return data != null ? data : LangEngine.getLangData(defaultLang);
    }

    public static LangData getPlayerLang(Player player) {
        return getPlayerLang(player.getUniqueId());
    }

    public static boolean hasChosenLang(UUID uuid) {
        return getBoolean(uuid, "chosen_lang", false);
    }

    // ── Public write API (100% backwards-compatible) ──────────────────────────

    /** Write-behind: updates RAM cache immediately, flushes to DB in background. */
    public static void set(UUID uuid, String path, Object value) {
        Document doc = ramCache.get(uuid);
        if (doc != null) {
            doc.put(path, value);
            dirtySet.put(uuid, Boolean.TRUE);
        } else {
            // Player not in cache (e.g. offline write) — write through immediately
            writeThroughImmediate(uuid, path, value);
        }
    }

    public static void setPlayerLang(UUID uuid, String langId) {
        set(uuid, "language", langId);
        set(uuid, "chosen_lang", true);
    }

    public static void setPlayerLang(Player player, String langId) {
        setPlayerLang(player.getUniqueId(), langId);
    }

    // ── Lifecycle: reload / shutdown ──────────────────────────────────────────

    public static void reload() {
        flushAll();
        ramCache.clear();
        dirtySet.clear();
        if (storageType == StorageType.YAML && dataFile != null) {
            yamlData = YamlConfiguration.loadConfiguration(dataFile);
        } else if (storageType == StorageType.JSON) {
            loadJson();
        }
    }

    public static void shutdown() {
        if (flushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }
        flushAll();
        if (storageType == StorageType.YAML) saveYaml();
        if (storageType == StorageType.JSON) saveJson();
        if (mongoClient != null) { try { mongoClient.close(); } catch (Exception ignored) {} }
        logMetrics();
    }

    // ── Debug metrics ─────────────────────────────────────────────────────────

    public static String getDebugMetrics() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int total = hits + misses;
        double hitRate = total == 0 ? 100.0 : (hits * 100.0 / total);
        return String.format(
            "§d  [INFO] §7PlayerEngine -> Cache: %d hits / %d misses (%.1f%%) | DB writes: %d | Dirty: %d | Last flush: %dms ago",
            hits, misses, hitRate, dbWrites.get(), dirtySet.size(),
            lastFlushMs.get() == 0 ? -1 : (System.currentTimeMillis() - lastFlushMs.get())
        );
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static void startFlushTask() {
        flushTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirtySet.isEmpty()) return;
            flushAll();
        }, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS).getTaskId();
    }

    /** Flush all dirty players to persistent storage. Uses bulk writes for MongoDB. */
    private static void flushAll() {
        if (dirtySet.isEmpty()) return;
        long start = System.currentTimeMillis();

        Set<UUID> toFlush = new HashSet<>(dirtySet.keySet());

        if (storageType == StorageType.MONGODB) {
            flushMongoAll(toFlush);
        } else if (storageType == StorageType.JSON) {
            flushJsonAll(toFlush);
        } else {
            flushYamlAll(toFlush);
        }

        lastFlushMs.set(System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 200) {
            Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7PlayerEngine -> Flush took " + elapsed + "ms for " + toFlush.size() + " players");
        }
    }

    private static void flushMongoAll(Set<UUID> uuids) {
        if (mongoCollection == null) return;
        List<WriteModel<Document>> bulk = new ArrayList<>();
        List<UUID> flushed = new ArrayList<>();

        for (UUID uuid : uuids) {
            if (flushInProgress.putIfAbsent(uuid, Boolean.TRUE) != null) continue;
            Document doc = ramCache.get(uuid);
            if (doc == null) { flushInProgress.remove(uuid); continue; }

            Document fields = new Document(doc);
            fields.remove("_id");
            bulk.add(new UpdateOneModel<>(
                Filters.eq("uuid", uuid.toString()),
                new Document("$set", fields)
            ));
            flushed.add(uuid);
        }

        if (!bulk.isEmpty()) {
            try {
                mongoCollection.bulkWrite(bulk);
                dbWrites.addAndGet(bulk.size());
                for (UUID uuid : flushed) dirtySet.remove(uuid);
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7PlayerEngine -> Bulk write failed");
                plugin.getLogger().log(Level.WARNING, "Details:", e);
            } finally {
                for (UUID uuid : flushed) flushInProgress.remove(uuid);
            }
        }
    }

    private static void flushJsonAll(Set<UUID> uuids) {
        for (UUID uuid : uuids) {
            if (flushInProgress.putIfAbsent(uuid, Boolean.TRUE) != null) continue;
            try {
                Document doc = ramCache.get(uuid);
                if (doc != null) {
                    jsonData.put(uuid.toString(), documentToMap(doc));
                    dbWrites.incrementAndGet();
                    dirtySet.remove(uuid);
                }
            } finally {
                flushInProgress.remove(uuid);
            }
        }
        saveJson();
    }

    private static void flushYamlAll(Set<UUID> uuids) {
        for (UUID uuid : uuids) {
            if (flushInProgress.putIfAbsent(uuid, Boolean.TRUE) != null) continue;
            try {
                Document doc = ramCache.get(uuid);
                if (doc != null) {
                    String uuidStr = uuid.toString();
                    for (Map.Entry<String, Object> e : doc.entrySet()) {
                        yamlData.set(uuidStr + "." + e.getKey(), e.getValue());
                    }
                    dbWrites.incrementAndGet();
                    dirtySet.remove(uuid);
                }
            } finally {
                flushInProgress.remove(uuid);
            }
        }
        saveYaml();
    }

    /** Synchronous single-player flush (used on quit). */
    private static void flushPlayer(UUID uuid) {
        if (!dirtySet.containsKey(uuid)) return;
        if (flushInProgress.putIfAbsent(uuid, Boolean.TRUE) != null) return;
        try {
            Document doc = ramCache.get(uuid);
            if (doc == null) return;

            if (storageType == StorageType.MONGODB) {
                if (mongoCollection == null) return;
                Document fields = new Document(doc);
                fields.remove("_id");
                mongoCollection.updateOne(
                    Filters.eq("uuid", uuid.toString()),
                    new Document("$set", fields)
                );
                dbWrites.incrementAndGet();
            } else if (storageType == StorageType.JSON) {
                jsonData.put(uuid.toString(), documentToMap(doc));
                saveJson();
                dbWrites.incrementAndGet();
            } else {
                String uuidStr = uuid.toString();
                for (Map.Entry<String, Object> e : doc.entrySet()) {
                    yamlData.set(uuidStr + "." + e.getKey(), e.getValue());
                }
                saveYaml();
                dbWrites.incrementAndGet();
            }
            dirtySet.remove(uuid);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7PlayerEngine -> Failed to flush player " + uuid);
            plugin.getLogger().log(Level.WARNING, "Details:", e);
        } finally {
            flushInProgress.remove(uuid);
        }
    }

    /** Returns cached doc, loading from DB on miss (async for MongoDB, sync for file backends). */
    private static Document getCachedDoc(UUID uuid) {
        Document cached = ramCache.get(uuid);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        cacheMisses.incrementAndGet();

        // Cache miss: load synchronously (only happens for offline players or race at join)
        if (storageType == StorageType.MONGODB) {
            if (mongoCollection == null) return null;
            try {
                Document doc = mongoCollection.find(Filters.eq("uuid", uuid.toString())).first();
                if (doc != null) ramCache.put(uuid, doc);
                return doc;
            } catch (Exception e) { return null; }
        } else if (storageType == StorageType.JSON) {
            Map<String, Object> flat = jsonData.get(uuid.toString());
            if (flat == null) return null;
            Document doc = mapToDocument(flat);
            ramCache.put(uuid, doc);
            return doc;
        } else {
            Document doc = yamlSectionToDocument(uuid);
            if (doc != null) ramCache.put(uuid, doc);
            return doc;
        }
    }

    /** Immediate write-through for offline players (no cache entry). */
    private static void writeThroughImmediate(UUID uuid, String path, Object value) {
        String uuidStr = uuid.toString();
        if (storageType == StorageType.MONGODB) {
            if (mongoCollection == null) return;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    mongoCollection.updateOne(
                        Filters.eq("uuid", uuidStr),
                        new Document("$set", new Document(path, value))
                    );
                    dbWrites.incrementAndGet();
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7PlayerEngine -> Write-through failed for " + uuidStr);
                    plugin.getLogger().log(Level.WARNING, "Details:", e);
                }
            });
        } else if (storageType == StorageType.JSON) {
            jsonData.computeIfAbsent(uuidStr, k -> new LinkedHashMap<>()).put(path, value);
            saveJson();
            dbWrites.incrementAndGet();
        } else {
            yamlData.set(uuidStr + "." + path, value);
            saveYaml();
            dbWrites.incrementAndGet();
        }
    }

    private static ConfigurationSection documentToSection(Document doc) {
        YamlConfiguration tmp = new YamlConfiguration();
        for (Map.Entry<String, Object> e : doc.entrySet()) {
            if (!e.getKey().equals("_id")) tmp.set(e.getKey(), e.getValue());
        }
        return tmp.getConfigurationSection("");
    }

    private static Document mapToDocument(Map<String, Object> map) {
        Document doc = new Document();
        doc.putAll(map);
        return doc;
    }

    private static Map<String, Object> documentToMap(Document doc) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : doc.entrySet()) {
            if (!e.getKey().equals("_id")) map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    private static Document yamlSectionToDocument(UUID uuid) {
        ConfigurationSection sec = yamlData.getConfigurationSection(uuid.toString());
        if (sec == null) return null;
        Document doc = new Document();
        for (String key : sec.getKeys(false)) {
            doc.put(key, sec.get(key));
        }
        return doc;
    }

    private static void saveYaml() {
        try { yamlData.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private static void loadJson() {
        jsonData = new LinkedHashMap<>();
        if (dataFile == null) return;
        if (!dataFile.exists()) { try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); } return; }
        try (Reader r = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
            Map<String, Map<String, Object>> loaded = GSON.fromJson(r, type);
            if (loaded != null) jsonData = loaded;
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void saveJson() {
        if (dataFile == null) return;
        try (Writer w = new FileWriter(dataFile)) {
            GSON.toJson(jsonData, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void logMetrics() {
        Bukkit.getConsoleSender().sendMessage(getDebugMetrics());
    }
}
