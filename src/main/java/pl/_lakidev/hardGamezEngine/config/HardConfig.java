package pl._lakidev.hardGamezEngine.config;

import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HardConfig {

    private final File file;
    private YamlConfiguration config;
    private final Map<String, Object> defaults;

    HardConfig(File file, Map<String, Object> defaults) {
        this.file = file;
        this.defaults = defaults;
        load();
    }

    private void load() {
        config = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) save();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        load();
    }

    public void set(String path, Object value) {
        config.set(path, value);
        save();
    }

    public Object get(String path) { return config.get(path); }
    public Object get(String path, Object def) { return config.get(path, def); }
    public String getString(String path) { return config.getString(path); }
    public String getString(String path, String def) { return config.getString(path, def); }
    public int getInt(String path) { return config.getInt(path); }
    public int getInt(String path, int def) { return config.getInt(path, def); }
    public double getDouble(String path) { return config.getDouble(path); }
    public double getDouble(String path, double def) { return config.getDouble(path, def); }
    public long getLong(String path) { return config.getLong(path); }
    public long getLong(String path, long def) { return config.getLong(path, def); }
    public boolean getBoolean(String path) { return config.getBoolean(path); }
    public boolean getBoolean(String path, boolean def) { return config.getBoolean(path, def); }
    public List<?> getList(String path) { return config.getList(path); }
    public List<String> getStringList(String path) { return config.getStringList(path); }
    public List<Integer> getIntegerList(String path) { return config.getIntegerList(path); }
    public List<Double> getDoubleList(String path) { return config.getDoubleList(path); }
    public List<Long> getLongList(String path) { return config.getLongList(path); }
    public List<Boolean> getBooleanList(String path) { return config.getBooleanList(path); }
    public ConfigurationSection getConfigurationSection(String path) { return config.getConfigurationSection(path); }
    public boolean contains(String path) { return config.contains(path); }
    public boolean isSet(String path) { return config.isSet(path); }
    public ItemStack getItemStack(String path) { return config.getItemStack(path); }
    public ItemStack getItemStack(String path, ItemStack def) { return config.getItemStack(path, def); }
    public Color getColor(String path) { return config.getColor(path); }
    public Color getColor(String path, Color def) { return config.getColor(path, def); }
    public Vector getVector(String path) { return config.getVector(path); }
    public Vector getVector(String path, Vector def) { return config.getVector(path, def); }
    public OfflinePlayer getOfflinePlayer(String path) { return config.getOfflinePlayer(path); }
    public YamlConfiguration getRaw() { return config; }
}
