package pl._lakidev.hardGamezEngine.lang;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LangData {

    private final File file;
    private YamlConfiguration config;

    public LangData(File file, YamlConfiguration config) {
        this.file = file;
        this.config = config;
    }

    public File getFile() { return file; }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String getId() { return config.getString("data.id", ""); }
    public String getName() { return config.getString("data.name", ""); }
    public String getAuthor() { return config.getString("data.author", ""); }
    public String getHeadURL() { return config.getString("data.head", ""); }
    public Integer getSlot() { return config.getInt("data.slot", 0); }
    public ConfigurationSection getContent() { return config.getConfigurationSection("content"); }
    public YamlConfiguration getRaw() { return config; }
}