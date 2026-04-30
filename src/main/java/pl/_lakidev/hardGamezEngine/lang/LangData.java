package pl._lakidev.hardGamezEngine.lang;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class LangData {

    private final YamlConfiguration config;

    public LangData(YamlConfiguration config) {
        this.config = config;
    }

    public String getId() { return config.getString("data.id", ""); }
    public String getName() { return config.getString("data.name", ""); }
    public String getAuthor() { return config.getString("data.author", ""); }
    public String getHeadURL() { return config.getString("data.head", ""); }
    public Integer getSlot() { return config.getInt("data.slot", 0); }
    public ConfigurationSection getContent() { return config.getConfigurationSection("content"); }
    public YamlConfiguration getRaw() { return config; }
}
