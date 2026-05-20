package pl._lakidev.hardGamezEngine.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigEngine {

    private static final Map<String, HardConfig> configs = new HashMap<>();

    public static HardConfig register(JavaPlugin plugin, String fileName, Map<String, Object> defaults) {
        String key = plugin.getName() + ":" + fileName;
        if (configs.containsKey(key)) return configs.get(key);

        File dir = new File(plugin.getServer().getWorldContainer(), "hardgamez/configs/" + plugin.getName());
        if (!dir.exists()) dir.mkdirs();

        String name = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File file = new File(dir, name);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (Exception e) { e.printStackTrace(); }
        }

        HardConfig config = new HardConfig(file, defaults != null ? defaults : new HashMap<>());
        configs.put(key, config);
        return config;
    }

    public static HardConfig get(JavaPlugin plugin, String fileName) {
        return configs.get(plugin.getName() + ":" + fileName);
    }

    public static void reloadAll() {
        for (HardConfig config : configs.values()) {
            config.reload();
        }
    }
}
