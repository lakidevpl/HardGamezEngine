package pl._lakidev.hardGamezEngine.lang.defaults;

import pl._lakidev.hardGamezEngine.lang.LangEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class English {
    public static void setup() {
        Map<String, Object> language = new HashMap<>();

        language.put("data.id", "en");
        language.put("data.name", "English");
        language.put("data.author", "_LakiDev");
        language.put("data.head", "http://textures.minecraft.net/texture/879d99d9c46474e2713a7e84a95e4ce7e8ff8ea4d164413a592e4435d2c6f9dc");
        language.put("data.slot", 11);
        language.put("perms", "&7You dont have permission to use this command!");
        language.put("only-player", "&cThis command is for players only!");
        language.put("offline", "&cPlayer {player} does not exist or is Offline!");
        language.put("close", "&cClose");

        language.put("content.language.title", "&8Change language");
        language.put("content.language.set", "&7Your server language is now &a{language}");
        language.put("content.language.item.title", "&6&l{language}");
        List<String> langLore = new ArrayList<>();
        langLore.add("&7");
        langLore.add("&7Language: &e{language}");
        langLore.add("&7Translated by: &b{author}");
        langLore.add("&7");
        langLore.add("&eClick to select");
        language.put("content.language.item.lore", langLore);

        LangEngine.register("en", "hardgamez/langs/english.yml", language);
    }
}
