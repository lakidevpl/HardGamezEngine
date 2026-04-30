package pl._lakidev.hardGamezEngine.lang;

import me.clip.placeholderapi.PlaceholderAPI;
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
    public static String formatText(CommandSender target, String message) {
        if (message == null) return "";
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        if (target instanceof Player player) {
            try {
                formatted = PlaceholderAPI.setPlaceholders(player, formatted);
            } catch (NoClassDefFoundError | Exception ignored) {

            }
        }
        return formatted;
    }

    public static String formatText(CommandSender target, String message, String... replacements) {
        if (message == null) return "";
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        if (target instanceof Player player) {
            try {
                formatted = PlaceholderAPI.setPlaceholders(player, formatted);
            } catch (NoClassDefFoundError | Exception ignored) {

            }
        }
        return applyReplacements(formatted, replacements);
    }

    public static String langText(CommandSender target, String path) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;

        ConfigurationSection content = lang.getContent();
        if (content == null) return null;

        if (!content.isList(path)) {
            String msg = content.getString(path);
            if (msg != null) return formatText(target, formatText(target, msg));
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
            if (msg != null) return formatText(target, applyReplacements(formatText(target, msg), replacements));
        }
        return null;
    }

    public static List<String> langList(CommandSender target, String path) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;

        ConfigurationSection content = lang.getContent();
        if (content == null) return null;

        if (content.isList(path)) {
            List<String> lines = content.getStringList(path);
            List<String> formatted = new ArrayList<>();
            for (String line : lines) {
                formatted.add(formatText(target, formatText(target, line)));
            }
            return formatted;
        }
        return null;
    }

    public static List<String> langList(CommandSender target, String path, String... replacements) {
        LangData lang = resolveLang(target);
        if (lang == null) return null;

        ConfigurationSection content = lang.getContent();
        if (content == null) return null;

        if (content.isList(path)) {
            List<String> lines = content.getStringList(path);
            List<String> formatted = new ArrayList<>();
            for (String line : lines) {
                formatted.add(formatText(target, applyReplacements(formatText(target, line), replacements)));
            }
            return formatted;
        }
        return null;
    }

    public static void sendMessage(CommandSender target, String path) {
        LangData lang = resolveLang(target);
        if (lang == null) return;

        ConfigurationSection content = lang.getContent();
        if (content == null) return;

        if (content.isList(path)) {
            List<String> lines = content.getStringList(path);
            for (String line : lines) {
                dispatch(target, formatText(target, line));
            }
        } else {
            String msg = content.getString(path);
            if (msg != null) dispatch(target, formatText(target, msg));
        }
    }
    public static void sendMessage(CommandSender target, Object message) {
        if (message instanceof List<?>) {
            List<String> lines = (List<String>) message;
            for (String line : lines) {
                dispatch(target, formatText(target, line));
            }
        } else {
            String msg = (String) message;
            if (msg != null) dispatch(target, formatText(target, msg));
        }
    }

    public static void sendMessage(CommandSender target, String path, String... replacements) {
        LangData lang = resolveLang(target);
        if (lang == null) return;

        ConfigurationSection content = lang.getContent();
        if (content == null) return;

        if (content.isList(path)) {
            List<String> lines = content.getStringList(path);
            for (String line : lines) {
                dispatch(target, formatText(target, applyReplacements(line, replacements)));
            }
        } else {
            String msg = content.getString(path);
            if (msg != null) dispatch(target, formatText(target, applyReplacements(msg, replacements)));
        }
    }

    public static void sendMessage(CommandSender target, Object message, String... replacements) {
        if (message instanceof List<?>) {
            List<String> lines = (List<String>) message;
            for (String line : lines) {
                dispatch(target, formatText(target, applyReplacements(line, replacements)));
            }
        } else {
            String msg = (String) message;
            if (msg != null) dispatch(target, formatText(target, applyReplacements(msg, replacements)));
        }
    }

    private static LangData resolveLang(CommandSender target) {
        if (target instanceof Player player) {
            return PlayerEngine.getPlayerLang(player);
        }
        return PlayerEngine.getPlayerLang((UUID) null);
    }

    private static void dispatch(CommandSender target, String message) {
        if (target == null) {
            HardGamezEngine.getInstance()
                    .getServer().getConsoleSender().sendMessage(message);
        } else {
            target.sendMessage(message);
        }
    }

    private static String applyReplacements(String text, String... replacements) {
        if (replacements == null || replacements.length < 2) return text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            if (placeholder != null && value != null) {
                text = text.replace(placeholder, value);
            }
        }
        return text;
    }
}