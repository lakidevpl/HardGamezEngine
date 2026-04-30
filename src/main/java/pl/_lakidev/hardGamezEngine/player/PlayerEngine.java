package pl._lakidev.hardGamezEngine.player;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.lang.LangData;
import pl._lakidev.hardGamezEngine.lang.LangEngine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class PlayerEngine {

    public enum StorageType { YAML, JSON, MONGODB }

    private static StorageType storageType;
    private static File dataFile;
    private static YamlConfiguration yamlData;
    private static MongoClient mongoClient;
    private static MongoCollection<Document> mongoCollection;
    private static String defaultLang;
    private static boolean registerIp;

    private static final Map<UUID, Document> cache = new HashMap<>();

    public static void init(HardGamezEngine plugin) {
        String type = plugin.getMainConfig().getString("data_storage.type", "YAML").toUpperCase();
        storageType = StorageType.valueOf(type);
        registerIp = plugin.getMainConfig().getBoolean("data_storage.register_ip_addresses", true);
        defaultLang = plugin.getMainConfig().getString("language.default", "pl");

        File root = new File(plugin.getServer().getWorldContainer(), "hardgamez");
        if (!root.exists()) root.mkdirs();

        if (storageType == StorageType.YAML) {
            dataFile = new File(root, "playersdata.yml");
            if (!dataFile.exists()) {
                try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            }
            yamlData = YamlConfiguration.loadConfiguration(dataFile);
        } else if (storageType == StorageType.JSON) {
            dataFile = new File(root, "playersdata.json");
            if (!dataFile.exists()) {
                try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            }
            yamlData = YamlConfiguration.loadConfiguration(dataFile);
        } else if (storageType == StorageType.MONGODB) {
            String host = plugin.getMainConfig().getString("database.host", "localhost");
            int port = plugin.getMainConfig().getInt("database.port", 27017);
            String username = plugin.getMainConfig().getString("database.username", "");
            String password = plugin.getMainConfig().getString("database.password", "");
            String database = plugin.getMainConfig().getString("database.database", "hardgamez");

            try {
                String uri;
                if (!username.isEmpty()) {
                    uri = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + database;
                } else {
                    uri = "mongodb://" + host + ":" + port;
                }
                mongoClient = MongoClients.create(uri);
                MongoDatabase db = mongoClient.getDatabase(database);
                mongoCollection = db.getCollection("players");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[HardGamezEngine] MongoDB connection failed, falling back to YAML!", e);
                storageType = StorageType.YAML;
                dataFile = new File(root, "playersdata.yml");
                if (!dataFile.exists()) {
                    try { dataFile.createNewFile(); } catch (IOException ex) { ex.printStackTrace(); }
                }
                yamlData = YamlConfiguration.loadConfiguration(dataFile);
            }
        }
    }

    public static void registerPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String uuidStr = uuid.toString();

        if (storageType == StorageType.MONGODB) {
            if (mongoCollection == null) return;
            Document existing = mongoCollection.find(Filters.eq("uuid", uuidStr)).first();
            if (existing != null) return;
            Document doc = new Document("uuid", uuidStr)
                    .append("nick", player.getName())
                    .append("first-join", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                    .append("language", defaultLang)
                    .append("chosen_lang", false);
            if (registerIp) doc.append("ip", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "");
            mongoCollection.insertOne(doc);
            cache.put(uuid, doc);
        } else {
            if (yamlData.contains(uuidStr)) return;
            yamlData.set(uuidStr + ".nick", player.getName());
            yamlData.set(uuidStr + ".first-join", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            yamlData.set(uuidStr + ".language", defaultLang);
            yamlData.set(uuidStr + ".chosen_lang", false);
            if (registerIp) yamlData.set(uuidStr + ".ip", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "");
            saveYaml();
        }
    }

    public static ConfigurationSection getPlayer(UUID uuid) {
        if (storageType == StorageType.MONGODB) {
            Document doc = getMongoDoc(uuid);
            if (doc == null) return null;
            YamlConfiguration tmp = new YamlConfiguration();
            for (Map.Entry<String, Object> e : doc.entrySet()) {
                if (!e.getKey().equals("_id")) tmp.set(e.getKey(), e.getValue());
            }
            return tmp.getConfigurationSection("");
        }
        return yamlData.getConfigurationSection(uuid.toString());
    }

    public static ConfigurationSection getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public static LangData getPlayerLang(UUID uuid) {
        String lang = defaultLang;
        if (storageType == StorageType.MONGODB) {
            Document doc = getMongoDoc(uuid);
            if (doc != null) lang = doc.getString("language");
        } else {
            lang = yamlData.getString(uuid + ".language", defaultLang);
        }
        LangData data = LangEngine.getLangData(lang);
        return data != null ? data : LangEngine.getLangData(defaultLang);
    }

    public static LangData getPlayerLang(Player player) {
        return getPlayerLang(player.getUniqueId());
    }

    public static void setPlayerLang(UUID uuid, String langId) {
        if (storageType == StorageType.MONGODB) {
            if (mongoCollection == null) return;
            mongoCollection.updateOne(
                    Filters.eq("uuid", uuid.toString()),
                    new Document("$set", new Document("language", langId).append("chosen_lang", true))
            );
            Document cached = cache.get(uuid);
            if (cached != null) { cached.put("language", langId); cached.put("chosen_lang", true); }
        } else {
            yamlData.set(uuid + ".language", langId);
            yamlData.set(uuid + ".chosen_lang", true);
            saveYaml();
        }
    }

    public static void setPlayerLang(Player player, String langId) {
        setPlayerLang(player.getUniqueId(), langId);
    }

    public static boolean hasChosenLang(UUID uuid) {
        if (storageType == StorageType.MONGODB) {
            Document doc = getMongoDoc(uuid);
            return doc != null && doc.getBoolean("chosen_lang", false);
        }
        return yamlData.getBoolean(uuid + ".chosen_lang", false);
    }

    public static void reload() {
        cache.clear();
        if (storageType != StorageType.MONGODB && dataFile != null) {
            yamlData = YamlConfiguration.loadConfiguration(dataFile);
        }
    }

    public static void shutdown() {
        if (mongoClient != null) {
            try { mongoClient.close(); } catch (Exception ignored) {}
        }
    }

    private static void saveYaml() {
        try {
            yamlData.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Document getMongoDoc(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        if (mongoCollection == null) return null;
        try {
            Document doc = mongoCollection.find(Filters.eq("uuid", uuid.toString())).first();
            if (doc != null) cache.put(uuid, doc);
            return doc;
        } catch (Exception e) {
            return null;
        }
    }
}
