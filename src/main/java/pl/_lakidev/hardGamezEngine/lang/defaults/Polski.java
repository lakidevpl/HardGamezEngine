package pl._lakidev.hardGamezEngine.lang.defaults;

import pl._lakidev.hardGamezEngine.lang.LangEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Polski {
    public static void setup() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("data.id", "pl");
        meta.put("data.name", "Polski");
        meta.put("data.author", "_LakiDev");
        meta.put("data.head", "http://textures.minecraft.net/texture/7fa269b8a663f52d6323dcd92edcc83d5f91d508afa819882deda15375f03d");
        meta.put("data.slot", 10);
        LangEngine.register("pl", "polski.yml", meta);

        Map<String, Object> content = new HashMap<>();
        content.put("perms", "&7Nie posiadasz uprawnień do tej komendy!");
        content.put("only-player", "&cTa komenda jest jedynie dla graczy!");
        content.put("offline", "&cGracz {player} nie istnieje lub jest Offline!");
        content.put("close", "&cZamknij");
        content.put("reload", "&7Przeładowano plugin &e{name}");
        content.put("language.title", "&8Zmień język");
        content.put("language.set", "&7Twój język serwera to od teraz &a{language}");
        content.put("language.item.title", "&6&l{language}");
        List<String> langLore = new ArrayList<>();
        langLore.add("&7");
        langLore.add("&7Język: &e{language}");
        langLore.add("&7Tłumaczył: &b{author}");
        langLore.add("&7");
        langLore.add("&eKliknij by wybrać");
        content.put("language.item.lore", langLore);
        content.put("hardgamez.usage", "&7Użycie: &d/hardgamez &7<info|reload|authors|download>");
        content.put("hardgamez.reload.done", "&d[HardGamezEngine] &7Przeładowano wszystkie pliki konfiguracyjne i językowe wszystkich pluginów.");

        List<String> info = new ArrayList<>();
        info.add("&7");
        info.add("&7=-=-=&d HardGamezEngine &7-=- &dKomendy &7=-=-=");
        info.add("&7");
        info.add("&d /hardgamez info &7- Lista dostępnych komend");
        info.add("&d /hardgamez reload &7- Przeładowanie konfiguracji i języków wszystkich pluginów");
        info.add("&d /hardgamez authors &7- Autorzy i prawa autorskie");
        info.add("&d /hardgamez download &7- Przeglądaj i pobieraj pluginy HardGamez");
        info.add("&d /lang &7- Zmień język serwera");
        info.add("&7");
        content.put("hardgamez.info", info);

        content.put("hardgamez.download.fetching", "&d[HardGamezEngine] &7Pobieranie listy pluginów...");
        content.put("hardgamez.download.fetch-error", "&c[HardGamezEngine] &7Nie udało się pobrać listy pluginów. Sprawdź połączenie z internetem.");
        content.put("hardgamez.download.list-header", "&7=-=-=&d HardGamezEngine &7-=- &dPluginy &7=-=-=");
        content.put("hardgamez.download.list-item", "&d {id} &8| &f{name} &7v{version}  &8» &e/hardgamez download {id}");
        content.put("hardgamez.download.not-found", "&c[HardGamezEngine] &7Nie znaleziono pluginu o ID: &e{id}");
        content.put("hardgamez.download.starting", "&d[HardGamezEngine] &7Pobieranie &f{name} &7v{version}...");
        content.put("hardgamez.download.success", "&d[HardGamezEngine] &7Plugin &f{name} &7v{version} &apomyślnie pobrany! &eWymagany restart serwera.");
        content.put("hardgamez.download.error", "&c[HardGamezEngine] &7Błąd podczas pobierania pluginu &f{name}&7.");

        List<String> authors = new ArrayList<>();
        authors.add("&7");
        authors.add("&7=-=-=&d HardGamezEngine &7-=- &dAutorzy &7=-=-=");
        authors.add("&7");
        authors.add("&d  HardGamezStudio");
        authors.add("&d  qweyypl");
        authors.add("&d  _LakiDevPL");
        authors.add("&d  grubyplaypl");
        authors.add("&7");
        authors.add("&e© HardGamez &7- Wszelkie prawa zastrzeżone.");
        authors.add("&7");
        content.put("hardgamez.authors", authors);

        LangEngine.addContent("pl", content);
    }
}
