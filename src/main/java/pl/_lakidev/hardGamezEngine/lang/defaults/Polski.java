package pl._lakidev.hardGamezEngine.lang.defaults;

import pl._lakidev.hardGamezEngine.lang.LangEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Polski {
    public static void setup() {
        Map<String, Object> language = new HashMap<>();

        language.put("data.id", "pl");
        language.put("data.name", "Polski");
        language.put("data.author", "_LakiDev");
        language.put("data.head", "http://textures.minecraft.net/texture/7fa269b8a663f52d6323dcd92edcc83d5f91d508afa819882deda15375f03d");
        language.put("data.slot", 10);

        language.put("perms", "&7Nie posiadasz uprawnień do tej komendy!");
        language.put("only-player", "&cTa komenda jest jedynie dla graczy!");
        language.put("offline", "&cGracz {player} nie istnieje lub jest Offline!");
        language.put("close", "&cZamknij");

        language.put("content.language.title", "&8Zmień język");
        language.put("content.language.set", "&7Twój język serwera to od teraz &a{language}");
        language.put("content.language.item.title", "&6&l{language}");
        List<String> langLore = new ArrayList<>();
        langLore.add("&7");
        langLore.add("&7Język: &e{language}");
        langLore.add("&7Tłumaczył: &b{author}");
        langLore.add("&7");
        langLore.add("&eKliknij by wybrać");
        language.put("content.language.item.lore", langLore);

        LangEngine.register("pl", "hardgamez/langs/polski.yml", language);
    }
}
