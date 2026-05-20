package pl._lakidev.hardGamezEngine.lang;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.player.PlayerEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MsgEngine {

    // -------------------------------------------------------------------------
    // format
    // -------------------------------------------------------------------------

    public static String formatText(CommandSender target, String message) {
        if (message == null) return "";
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        if (target instanceof Player player && isPlaceholderAPIEnabled()) {
            formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formatted);
        }
        return formatted;
    }

    public static String formatText(CommandSender target, String message, String... replacements) {
        if (message == null) return "";
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        if (target instanceof Player player && isPlaceholderAPIEnabled()) {
            formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formatted);
        }
        return applyReplacements(formatted, replacements);
    }

    public static List<String> formatList(CommandSender target, List<String> lines) {
        if (lines == null) return new ArrayList<>();
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) result.add(formatText(target, line));
        return result;
    }

    public static List<String> formatList(CommandSender target, List<String> lines, String... replacements) {
        if (lines == null) return new ArrayList<>();
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) result.add(formatText(target, applyReplacements(line, replacements)));
        return result;
    }

    // -------------------------------------------------------------------------
    // lang helpers
    // -------------------------------------------------------------------------

    public static String langText(CommandSender target, String path) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;
        ConfigurationSection content = lang.getContent();
        if (content == null) return null;
        if (!content.isList(path)) {
            String msg = content.getString(path);
            if (msg != null) return formatText(target, msg);
        }
        return null;
    }

    public static String langText(CommandSender target, String path, String... replacements) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;
        ConfigurationSection content = lang.getContent();
        if (content == null) return null;
        if (!content.isList(path)) {
            String msg = content.getString(path);
            if (msg != null) return formatText(target, applyReplacements(msg, replacements));
        }
        return null;
    }

    public static List<String> langList(CommandSender target, String path) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;
        ConfigurationSection content = lang.getContent();
        if (content == null) return null;
        if (content.isList(path)) return formatList(target, content.getStringList(path));
        return null;
    }

    public static List<String> langList(CommandSender target, String path, String... replacements) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;
        ConfigurationSection content = lang.getContent();
        if (content == null) return null;
        if (content.isList(path)) return formatList(target, content.getStringList(path), replacements);
        return null;
    }

    // -------------------------------------------------------------------------
    // sendMessage  – z pliku językowego
    // sendText     – gotowa wiadomość (String lub List<String>)
    // -------------------------------------------------------------------------

    public static void sendMessage(CommandSender target, String path) {
        LangData lang = resolveLang(target);
        if (lang == null) return;
        ConfigurationSection content = lang.getContent();
        if (content == null) return;
        if (content.isList(path)) {
            for (String line : content.getStringList(path))
                dispatch(target, formatText(target, line));
        } else {
            String msg = content.getString(path);
            if (msg != null) dispatch(target, formatText(target, msg));
        }
    }

    public static void sendMessage(CommandSender target, String path, String... replacements) {
        LangData lang = resolveLang(target);
        if (lang == null) return;
        ConfigurationSection content = lang.getContent();
        if (content == null) return;
        if (content.isList(path)) {
            for (String line : content.getStringList(path))
                dispatch(target, formatText(target, applyReplacements(line, replacements)));
        } else {
            String msg = content.getString(path);
            if (msg != null) dispatch(target, formatText(target, applyReplacements(msg, replacements)));
        }
    }

    public static void sendText(CommandSender target, Object message) {
        if (message instanceof List<?> list) {
            for (Object item : list) dispatch(target, formatText(target, String.valueOf(item)));
        } else if (message instanceof String msg) {
            dispatch(target, formatText(target, msg));
        }
    }

    public static void sendText(CommandSender target, Object message, String... replacements) {
        if (message instanceof List<?> list) {
            for (Object item : list)
                dispatch(target, formatText(target, applyReplacements(String.valueOf(item), replacements)));
        } else if (message instanceof String msg) {
            dispatch(target, formatText(target, applyReplacements(msg, replacements)));
        }
    }

    // -------------------------------------------------------------------------
    // sendTitle – z pliku językowego
    // -------------------------------------------------------------------------

    public static void sendTitle(Object target, String titlePath, String subtitlePath) {
        sendTitle(target, titlePath, subtitlePath, 10, 70, 20);
    }

    public static void sendTitle(Object target, String titlePath, String subtitlePath,
                                 int timeIn, int timeStay, int timeOut) {
        Player player = asPlayer(target);
        if (player == null) return;
        CommandSender sender = player;
        String title    = titlePath    != null ? langText(sender, titlePath)    : null;
        String subtitle = subtitlePath != null ? langText(sender, subtitlePath) : null;
        player.sendTitle(
            title    != null ? title    : "",
            subtitle != null ? subtitle : "",
            timeIn, timeStay, timeOut
        );
    }

    public static void sendTitle(Object target, String titlePath, String subtitlePath,
                                 String... replacements) {
        sendTitle(target, titlePath, subtitlePath, 10, 70, 20, replacements);
    }

    public static void sendTitle(Object target, String titlePath, String subtitlePath,
                                 int timeIn, int timeStay, int timeOut, String... replacements) {
        Player player = asPlayer(target);
        if (player == null) return;
        CommandSender sender = player;
        String title    = titlePath    != null ? langText(sender, titlePath,    replacements) : null;
        String subtitle = subtitlePath != null ? langText(sender, subtitlePath, replacements) : null;
        player.sendTitle(
            title    != null ? title    : "",
            subtitle != null ? subtitle : "",
            timeIn, timeStay, timeOut
        );
    }

    // -------------------------------------------------------------------------
    // sendNormalTitle – gotowy tekst (formatowany przez formatText)
    // -------------------------------------------------------------------------

    public static void sendNormalTitle(Object target, String title, String subtitle) {
        sendNormalTitle(target, title, subtitle, 10, 70, 20);
    }

    public static void sendNormalTitle(Object target, String title, String subtitle,
                                       int timeIn, int timeStay, int timeOut) {
        Player player = asPlayer(target);
        if (player == null) return;
        CommandSender sender = player;
        player.sendTitle(
            title    != null ? formatText(sender, title)    : "",
            subtitle != null ? formatText(sender, subtitle) : "",
            timeIn, timeStay, timeOut
        );
    }

    // -------------------------------------------------------------------------
    // sendActionBar – z pliku językowego
    // -------------------------------------------------------------------------

    public static void sendActionBar(Object target, String path) {
        Player player = asPlayer(target);
        if (player == null) return;
        String msg = langText(player, path);
        if (msg != null) player.sendActionBar(msg);
    }

    public static void sendActionBar(Object target, String path, String... replacements) {
        Player player = asPlayer(target);
        if (player == null) return;
        String msg = langText(player, path, replacements);
        if (msg != null) player.sendActionBar(msg);
    }

    // -------------------------------------------------------------------------
    // sendNormalActionBar – gotowy tekst
    // -------------------------------------------------------------------------

    public static void sendNormalActionBar(Object target, String message) {
        Player player = asPlayer(target);
        if (player == null) return;
        if (message != null) player.sendActionBar(formatText(player, message));
    }

    public static void sendNormalActionBar(Object target, String message, String... replacements) {
        Player player = asPlayer(target);
        if (player == null) return;
        if (message != null)
            player.sendActionBar(formatText(player, applyReplacements(message, replacements)));
    }

    // -------------------------------------------------------------------------
    // internal
    // -------------------------------------------------------------------------

    private static LangData resolveLang(CommandSender target) {
        if (target instanceof Player player) return PlayerEngine.getPlayerLang(player);
        return PlayerEngine.getPlayerLang((UUID) null);
    }

    private static Player asPlayer(Object target) {
        if (target instanceof Player player) return player;
        return null;
    }

    private static void dispatch(CommandSender target, String message) {
        if (target == null) {
            HardGamezEngine.getInstance().getServer().getConsoleSender().sendMessage(message);
        } else {
            target.sendMessage(message);
        }
    }

    private static String applyReplacements(String text, String... replacements) {
        if (replacements == null || replacements.length < 2) return text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            if (placeholder != null && value != null) text = text.replace(placeholder, value);
        }
        return text;
    }

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static String applyPlaceholders(Player player, String text) {
        if (text == null || !isPlaceholderAPIEnabled()) return text;
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }
}
