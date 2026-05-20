package pl._lakidev.hardGamezEngine;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pl._lakidev.hardGamezEngine.commands.HardGamez;
import pl._lakidev.hardGamezEngine.commands.Lang;
import pl._lakidev.hardGamezEngine.config.ConfigEngine;
import pl._lakidev.hardGamezEngine.config.HardConfig;
import pl._lakidev.hardGamezEngine.lang.LangEngine;
import pl._lakidev.hardGamezEngine.lang.defaults.English;
import pl._lakidev.hardGamezEngine.lang.defaults.Polski;
import pl._lakidev.hardGamezEngine.player.PlayerEngine;
import pl._lakidev.hardGamezEngine.player.PlayerRegister;
import pl._lakidev.hardGamezEngine.hologram.HologramEngine;
import pl._lakidev.hardGamezEngine.scoreboard.ScoreboardEngine;
import pl._lakidev.hardGamezEngine.tablist.TablistEngine;
import pl._lakidev.hardGamezEngine.web.WebEngine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class HardGamezEngine extends JavaPlugin {

    public static final String PLUGINS_LIST_URL = "https://raw.githubusercontent.com/lakidevpl/HardGamezEngine/refs/heads/main/src/main/resources/plugins.json";

    private static HardGamezEngine instance;
    public static HardConfig mainConfig;

    @Override
    public void onEnable() {
        instance = this;
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        console.sendMessage("§7");
        console.sendMessage("§7=-=-=-=-=-=-════ §dHardGamezEngine §7════-=-=-=-=-=-=-=");
        console.sendMessage("§7");

        // --- Konfiguracja ---
        console.sendMessage("§d  [INFO] §fLoading configuration...");

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("data_storage.type", "YAML");
        defaults.put("data_storage.register_ip_addresses", true);
        defaults.put("database.host", "localhost");
        defaults.put("database.port", 27017);
        defaults.put("database.username", "");
        defaults.put("database.password", "");
        defaults.put("database.database", "hardgamez");
        defaults.put("language.enabled", true);
        defaults.put("language.default", "pl");
        defaults.put("language.first_join_menu", true);
        defaults.put("webserver.enabled", true);
        defaults.put("webserver.port", 8080);
        defaults.put("webserver.encryption.enabled", true);
        defaults.put("webserver.encryption.key", "changeme-set-a-strong-key");

        mainConfig = ConfigEngine.register(this, "config", defaults);

        console.sendMessage("§d  [INFO] §fPaper engine");
        console.sendMessage("§d  [DONE] §fRegistered and loaded configuration");
        console.sendMessage("§d  [INFO] §fData storage type: §b"
                + mainConfig.getString("data_storage.type")
                + "§f. §eAvailable types: YAML, JSON and MONGODB.");

        // --- Lang & PlayerEngine ---
        console.sendMessage("§d  [INFO] §fInitializing language engine...");
        File serverRoot = getServer().getWorldContainer();
        LangEngine.init(serverRoot);
        Polski.setup();
        English.setup();
        getServer().getPluginManager().registerEvents(new PlayerRegister(this), this);
        getCommand("lang").setExecutor(new Lang());
        HardGamez hardGamezCmd = new HardGamez();
        getCommand("hardgamez").setExecutor(hardGamezCmd);
        getCommand("hardgamez").setTabCompleter(hardGamezCmd);
        HardGamez.refreshCache();
        console.sendMessage("§d  [DONE] §fLanguage engine initialized");

        console.sendMessage("§d  [INFO] §fInitializing player data engine...");
        PlayerEngine.init(this);
        console.sendMessage("§d  [DONE] §fPlayer data engine initialized");

        // --- GUI & Hologram ---
        console.sendMessage("§d  [INFO] §fInitializing gui engine...");
        HologramEngine.init(this);
        TablistEngine.init(this);
        console.sendMessage("§d  [DONE] §fGui engine initialized");

        // --- Web ---
        console.sendMessage("§d  [INFO] §fInitializing web engine...");
        WebEngine.init(this);
        console.sendMessage("§d  [DONE] §fWeb engine initialized");

        // --- Gotowe ---
        console.sendMessage("§d  [DONE] §7Plugin enabled successfully");
        console.sendMessage("§7");
        console.sendMessage("§7=-=-=-=-=-=-════ §dHardGamezEngine §7════-=-=-=-=-=-=-=");
        console.sendMessage("§7");
    }

    @Override
    public void onDisable() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        console.sendMessage("§7");
        console.sendMessage("§7=-=-=-=-=-=-════ §dHardGamezEngine §7════-=-=-=-=-=-=-=");
        console.sendMessage("§7");
        console.sendMessage("§7                   §6§bHardGamezAPI");
        console.sendMessage("§e            It holds everything together");
        console.sendMessage("§7");
        console.sendMessage("§7 Disabling plugin - LOG:");
        console.sendMessage("§d  [INFO] §7Disabling features");
        HologramEngine.removeAll();
        ScoreboardEngine.removeAll();
        TablistEngine.removeAll();
        PlayerEngine.shutdown();
        if (WebEngine.getInstance() != null) WebEngine.getInstance().shutdown();
        console.sendMessage("§d  [DONE] §7Plugin disabled successfully");
        console.sendMessage("§7");
        console.sendMessage("§7=-=-=-=-=-=-════ §dHardGamezEngine §7════-=-=-=-=-=-=-=");
        console.sendMessage("§7");
    }

    public static HardGamezEngine getInstance() { return instance; }
    public HardConfig getMainConfig() { return mainConfig; }
}