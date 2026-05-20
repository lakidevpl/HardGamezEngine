package pl._lakidev.hardGamezEngine.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.config.ConfigEngine;
import pl._lakidev.hardGamezEngine.lang.LangEngine;
import pl._lakidev.hardGamezEngine.lang.MsgEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class HardGamez implements CommandExecutor, TabCompleter {

    private record PluginEntry(String name, String version, String downloadURL) {}

    private static final List<String> SUBCOMMANDS = List.of("info", "reload", "authors", "download");
    private static final Map<String, PluginEntry> cachedPlugins = new LinkedHashMap<>();

    public static void refreshCache() {
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            Map<String, PluginEntry> fresh = fetchList();
            if (fresh != null) {
                cachedPlugins.clear();
                cachedPlugins.putAll(fresh);
            }
        });
    }

    // -------------------------------------------------------------------------
    // CommandExecutor
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hardgamez.admin")) {
            MsgEngine.sendMessage(sender, "perms");
            return false;
        }
        if (args.length == 0) {
            MsgEngine.sendMessage(sender, "hardgamez.usage");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "info"     -> MsgEngine.sendMessage(sender, "hardgamez.info");
            case "reload"   -> {
                ConfigEngine.reloadAll();
                LangEngine.reload();
                MsgEngine.sendMessage(sender, "hardgamez.reload.done");
            }
            case "authors"  -> MsgEngine.sendMessage(sender, "hardgamez.authors");
            case "download" -> handleDownload(sender, args);
            default         -> MsgEngine.sendMessage(sender, "hardgamez.usage");
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // TabCompleter
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hardgamez.admin")) return List.of();
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("download")) {
            if (cachedPlugins.isEmpty()) refreshCache();
            return filter(new ArrayList<>(cachedPlugins.keySet()), args[1]);
        }
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Download logic
    // -------------------------------------------------------------------------

    private void handleDownload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            listPlugins(sender);
        } else {
            downloadPlugin(sender, args[1].toLowerCase());
        }
    }

    private void listPlugins(CommandSender sender) {
        MsgEngine.sendMessage(sender, "hardgamez.download.fetching");
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            Map<String, PluginEntry> plugins = fetchList();
            if (plugins != null) {
                cachedPlugins.clear();
                cachedPlugins.putAll(plugins);
            }
            Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> {
                if (plugins == null) {
                    MsgEngine.sendMessage(sender, "hardgamez.download.fetch-error");
                    return;
                }
                MsgEngine.sendMessage(sender, "hardgamez.download.list-header");
                for (Map.Entry<String, PluginEntry> e : plugins.entrySet()) {
                    MsgEngine.sendMessage(sender, "hardgamez.download.list-item",
                            "{id}", e.getKey(),
                            "{name}", e.getValue().name(),
                            "{version}", e.getValue().version());
                }
            });
        });
    }

    private void downloadPlugin(CommandSender sender, String pluginId) {
        MsgEngine.sendMessage(sender, "hardgamez.download.fetching");
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            Map<String, PluginEntry> plugins = fetchList();
            if (plugins != null) {
                cachedPlugins.clear();
                cachedPlugins.putAll(plugins);
            }
            if (plugins == null || !plugins.containsKey(pluginId)) {
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () ->
                        MsgEngine.sendMessage(sender, "hardgamez.download.not-found", "{id}", pluginId));
                return;
            }
            PluginEntry entry = plugins.get(pluginId);
            Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () ->
                    MsgEngine.sendMessage(sender, "hardgamez.download.starting",
                            "{name}", entry.name(), "{version}", entry.version()));
            boolean success = doDownload(entry);
            Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> {
                if (success) {
                    MsgEngine.sendMessage(sender, "hardgamez.download.success",
                            "{name}", entry.name(), "{version}", entry.version());
                } else {
                    MsgEngine.sendMessage(sender, "hardgamez.download.error",
                            "{name}", entry.name());
                }
            });
        });
    }

    // -------------------------------------------------------------------------
    // Network helpers (static so refreshCache can call without an instance)
    // -------------------------------------------------------------------------

    private static Map<String, PluginEntry> fetchList() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(HardGamezEngine.PLUGINS_LIST_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "HardGamezEngine");

            try (InputStream is = conn.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, PluginEntry> result = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    JsonObject obj = e.getValue().getAsJsonObject();
                    result.put(e.getKey(), new PluginEntry(
                            obj.get("name").getAsString(),
                            obj.get("version").getAsString(),
                            obj.get("downloadURL").getAsString()
                    ));
                }
                return result;
            }
        } catch (Exception e) {
            HardGamezEngine.getInstance().getLogger().warning("[Download] Failed to fetch plugin list: " + e.getMessage());
            return null;
        }
    }

    private static boolean doDownload(PluginEntry entry) {
        try {
            File pluginsDir = HardGamezEngine.getInstance().getDataFolder().getParentFile();
            String fileName = entry.name() + "-" + entry.version() + ".jar";
            File output = new File(pluginsDir, fileName);

            HttpURLConnection conn = (HttpURLConnection) new URL(entry.downloadURL()).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "HardGamezEngine");

            try (InputStream in = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(output)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) fos.write(buffer, 0, read);
            }
            return true;
        } catch (Exception e) {
            HardGamezEngine.getInstance().getLogger().warning("[Download] Failed to download " + entry.name() + ": " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Util
    // -------------------------------------------------------------------------

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
