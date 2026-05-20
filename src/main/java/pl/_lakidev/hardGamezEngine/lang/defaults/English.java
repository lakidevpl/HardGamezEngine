package pl._lakidev.hardGamezEngine.lang.defaults;

import pl._lakidev.hardGamezEngine.lang.LangEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class English {
    public static void setup() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("data.id", "en");
        meta.put("data.name", "English");
        meta.put("data.author", "_LakiDev");
        meta.put("data.head", "http://textures.minecraft.net/texture/879d99d9c46474e2713a7e84a95e4ce7e8ff8ea4d164413a592e4435d2c6f9dc");
        meta.put("data.slot", 11);
        LangEngine.register("en", "english.yml", meta);

        Map<String, Object> content = new HashMap<>();
        content.put("perms", "&7You dont have permission to use this command!");
        content.put("only-player", "&cThis command is for players only!");
        content.put("offline", "&cPlayer {player} does not exist or is Offline!");
        content.put("close", "&cClose");
        content.put("reload", "&7Reloaded &e{name} &7plugin");
        content.put("language.title", "&8Change language");
        content.put("language.set", "&7Your server language is now &a{language}");
        content.put("language.item.title", "&6&l{language}");
        List<String> langLore = new ArrayList<>();
        langLore.add("&7");
        langLore.add("&7Language: &e{language}");
        langLore.add("&7Translated by: &b{author}");
        langLore.add("&7");
        langLore.add("&eClick to select");
        content.put("language.item.lore", langLore);
        content.put("hardgamez.usage", "&7Usage: &d/hardgamez &7<info|reload|authors|download>");
        content.put("hardgamez.reload.done", "&d[HardGamezEngine] &7Reloaded all configuration and language files for all plugins.");

        List<String> info = new ArrayList<>();
        info.add("&7");
        info.add("&7=-=-= &d HardGamezEngine &7-=- &d Commands &7=-=-=");
        info.add("&7");
        info.add("&d /hardgamez info &7- List of available commands");
        info.add("&d /hardgamez reload &7- Reload configuration and language files for all plugins");
        info.add("&d /hardgamez authors &7- Authors and copyright");
        info.add("&d /hardgamez download &7- Browse and download HardGamez plugins");
        info.add("&d /lang &7- Change server language");
        info.add("&7");
        content.put("hardgamez.info", info);

        content.put("hardgamez.download.fetching", "&d[HardGamezEngine] &7Fetching plugin list...");
        content.put("hardgamez.download.fetch-error", "&c[HardGamezEngine] &7Failed to fetch plugin list. Check your internet connection.");
        content.put("hardgamez.download.list-header", "&7=-=-= &d HardGamezEngine &7-=- &d Plugins &7=-=-=");
        content.put("hardgamez.download.list-item", "&d {id} &8| &f{name} &7v{version}  &8» &e/hardgamez download {id}");
        content.put("hardgamez.download.not-found", "&c[HardGamezEngine] &7No plugin found with ID: &e{id}");
        content.put("hardgamez.download.starting", "&d[HardGamezEngine] &7Downloading &f{name} &7v{version}...");
        content.put("hardgamez.download.success", "&d[HardGamezEngine] &7Plugin &f{name} &7v{version} &adownloaded successfully! &eServer restart required.");
        content.put("hardgamez.download.error", "&c[HardGamezEngine] &7Failed to download plugin &f{name}&7.");

        List<String> authors = new ArrayList<>();
        authors.add("&7");
        authors.add("&7=-=-= &d HardGamezEngine &7-=- &d Authors &7=-=-=");
        authors.add("&7");
        authors.add("&d  HardGamezStudio");
        authors.add("&d  qweyypl");
        authors.add("&d  _LakiDevPL");
        authors.add("&d  grubyplaypl");
        authors.add("&7");
        authors.add("&e© HardGamez &7- All rights reserved.");
        authors.add("&7");
        content.put("hardgamez.authors", authors);

        LangEngine.addContent("en", content);
    }
}
