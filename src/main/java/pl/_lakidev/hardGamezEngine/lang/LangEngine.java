package pl._lakidev.hardGamezEngine.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LangEngine {

    private static File langsDir;
    private static final Map<String, LangData> langs = new HashMap<>();

    public static void init(File serverRoot) {
        langsDir = new File(serverRoot, "hardgamez/langs");
        if (!langsDir.exists()) langsDir.mkdirs();
    }

    public static LangData register(String id, String fileName, Map<String, Object> defaults) {
        String name = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File file = new File(langsDir, name);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (Exception e) { e.printStackTrace(); }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        if (defaults != null) {
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                if (!config.contains(entry.getKey())) {
                    config.set(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }
        if (changed) {
            try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
        }

        LangData data = new LangData(file, config);
        langs.put(id, data);
        return data;
    }

    public static List<LangData> getLanguages() {
        return new ArrayList<>(langs.values());
    }

    public static YamlConfiguration getLang(String id) {
        LangData d = langs.get(id);
        return d != null ? d.getRaw() : null;
    }

    public static LangData getLangData(String id) {
        return langs.get(id);
    }

    public static void addContent(String id, Map<String, Object> content) {
        LangData data = langs.get(id);
        if (data == null) return;
        YamlConfiguration config = data.getRaw();
        File file = data.getFile();
        boolean changed = false;
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            String key = "content." + entry.getKey();
            if (!config.contains(key)) {
                config.set(key, entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void reload() {
        for (LangData data : langs.values()) {
            data.reload();
        }
    }
}