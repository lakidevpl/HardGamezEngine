# HardGamezEngine

A Paper plugin framework for building Minecraft plugins. Provides ready-made engines for config, player data, language, GUI, scheduling, HTTP server, and Discord webhooks — all usable via fluent builder APIs.

- **Platform:** Paper 1.20.1
- **Java:** 17
- **Package:** `pl._lakidev.hardGamezEngine`

---

## Installation

1. Download `HardGamezEngine.jar` from releases and place it in your plugin project's `/lib` directory.
2. Add the dependency to `pom.xml`:

```xml
<dependency>
    <groupId>pl._lakidev</groupId>
    <artifactId>HardGamezEngine</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${basedir}/lib/HardGamezEngine.jar</systemPath>
</dependency>
```

---

## Engines overview

| Engine | Entry point | Purpose |
|---|---|---|
| [ConfigEngine](#configengine) | `ConfigEngine.register(...)` | YAML config files with defaults |
| [PlayerEngine](#playerengine) | `PlayerEngine.set/get(...)` | Per-player data (YAML / JSON / MongoDB) |
| [LangEngine & MsgEngine](#langengine--msgengine) | `LangEngine.register(...)` | Multi-language YAML files |
| [GuiEngine](#guiengine--chestgui) | `GuiEngine.chest(...)` | Chest inventory GUIs |
| [HologramEngine](#hologramengine) | `HologramEngine.create(...)` | TextDisplay-based holograms |
| [ScoreboardEngine](#scoreboardengine) | `ScoreboardEngine.create(...)` | Per-player sidebar scoreboards |
| [TablistEngine](#tablistengine) | `TablistEngine.create(...)` | Per-player tablist header/footer & player list |
| [TimeEngine](#timeengine) | `new TimeEngine(plugin)` | Delayed / repeating tasks |
| [WebEngine](#webengine) | `WebEngine.createEndpoint(...)` | HTTP API server (Paper ↔ Velocity) |
| [WebhookEngine](#webhookengine) | `WebhookEngine.sendWebhook(...)` | Discord webhook sender |
| [NpcEngine](#npcengine) | `NpcEngine.register(...)` | Fake-player and mob NPCs with pathfinding |

---

## ConfigEngine

Creates and manages YAML config files. Files are saved to `hardgamez/configs/<PluginName>/<name>.yml`.

```java
Map<String, Object> defaults = new HashMap<>();
defaults.put("some.key", "value");
defaults.put("some.number", 42);

HardConfig cfg = ConfigEngine.register(plugin, "config", defaults);

// read
String val = cfg.getString("some.key");
int num    = cfg.getInt("some.number", 0);

// write
cfg.set("some.key", "new-value");

// reload from disk
cfg.reload();
```

Reload all registered configs across all plugins at once:

```java
ConfigEngine.reloadAll();
```

**HardConfig read methods:** `getString`, `getInt`, `getDouble`, `getLong`, `getBoolean`, `getList`, `getStringList`, `getIntegerList`, `getDoubleList`, `getLongList`, `getBooleanList`, `getConfigurationSection`, `getItemStack`, `getColor`, `getVector`, `getOfflinePlayer`, `getRaw`, `contains`, `isSet`.

---

## PlayerEngine

Stores per-player data. Backend is configured in `config.yml` (`data_storage.type: YAML | JSON | MONGODB`).

```java
UUID uuid = player.getUniqueId();

// write
PlayerEngine.set(uuid, "stats.wins", 10);
PlayerEngine.set(uuid, "stats.kills", 55);

// read
int wins    = PlayerEngine.getInt(uuid, "stats.wins", 0);
String rank = PlayerEngine.getString(uuid, "rank", "default");
boolean vip = PlayerEngine.getBoolean(uuid, "vip", false);

// language helpers
LangData lang = PlayerEngine.getPlayerLang(player);
PlayerEngine.setPlayerLang(player, "en");
boolean chose = PlayerEngine.hasChosenLang(uuid);
```

---

## LangEngine & MsgEngine

### Registering a language

```java
Map<String, Object> content = new HashMap<>();
content.put("messages.welcome", "&aWelcome, %player_name%!");
content.put("messages.goodbye", "&cBye, {0}!");

LangData lang = LangEngine.register("en", "english", content);
```

### Sending messages

```java
// from language file (resolves player's language automatically)
MsgEngine.sendMessage(player, "messages.welcome");
MsgEngine.sendMessage(player, "messages.goodbye", "{0}", "Steve");

// title / subtitle
MsgEngine.sendTitle(player, "titles.main", "titles.sub");

// action bar
MsgEngine.sendActionBar(player, "messages.bar");

// format only (returns String)
String text = MsgEngine.langText(player, "messages.welcome");
String raw  = MsgEngine.formatText(player, "&aHello {0}!", "{0}", "Steve");
```

Replacements are pairs: `"placeholder", "value"` repeated.

---

## GuiEngine & ChestGUI

```java
ChestGUI gui = GuiEngine.chest("&8Player Menu", 3)  // title, rows (1-6)
    .setBackground(Material.GRAY_STAINED_GLASS_PANE)
    .fillBorder(Material.BLACK_STAINED_GLASS_PANE)
    .onOpen(p  -> p.sendMessage("opened"))
    .onClose(p -> p.sendMessage("closed"));

// add item
gui.addItem(13, Material.DIAMOND)
    .setTitle("&bStats")
    .setLore("&7Wins: &f" + wins, "&7Kills: &f" + kills)
    .setCooldown(1000)
    .setPermission("myplugin.stats")
    .onClick(e -> e.getPlayer().sendMessage("clicked!"))
    .build();

// dynamic item (re-renders on each GUI refresh)
gui.addItem(22, Material.PLAYER_HEAD)
    .setTitle(() -> "&e" + player.getName())
    .setSkullOwner(player.getUniqueId())
    .setDynamic(true)
    .build();

gui.open(player);

// auto-refresh every 20 ticks
gui.startAutoRefresh(20);

// page helpers
gui.setPage(2);
gui.nextPage();
gui.previousPage();

// store/retrieve arbitrary data on the GUI instance
gui.setData("filter", "pvp");
String filter = gui.getData("filter");
```

### GuiClickEvent

```java
.onClick(event -> {
    Player p       = event.getPlayer();
    int slot       = event.getSlot();
    ClickType type = event.getClickType();
    ChestGUI g     = event.getGui();
})
```

### ItemBuilder click handlers

```java
.onClick(e -> { })       // any click
.onLeftClick(e -> { })
.onRightClick(e -> { })
.onShiftClick(e -> { })
```

---

## HologramEngine

Creates and manages server-side holograms using Paper 1.20.1 `TextDisplay` entities. Holograms are stored in memory only (`setPersistent(false)`) and cleaned up automatically on server shutdown.

```java
List<String> lines = new ArrayList<>();
lines.add("&6&lHardGamez");
lines.add("&7Kills: &f" + kills);

HardHologram holo = HologramEngine.create("my_holo", location, lines);
```

Multiple lines are stacked vertically (0.3 blocks apart, top-down). Each line is a separate `TextDisplay` entity with `Billboard.CENTER` — always facing the player.

### HologramEngine methods

```java
HardHologram holo = HologramEngine.create("id", location, lines); // create or replace
HardHologram holo = HologramEngine.get("id");                     // get existing, null if missing
boolean removed   = HologramEngine.remove("id");                   // despawn & unregister
HologramEngine.removeAll();                                        // despawn all (auto on disable)
```

### HardHologram — global edit

```java
holo.setLine(0, "&aNew first line");  // replace line by index
holo.addLine("&7Extra line");         // append line at bottom
holo.removeLine(1);                   // remove line by index
holo.update(newLines);                // replace all lines at once
holo.move(newLocation);               // teleport hologram to new location
holo.despawn();                       // remove all entities from world

Location loc      = holo.getLocation();
List<String> text = holo.getLines();
String id         = holo.getId();
```

All edit methods return `this` and can be chained:

```java
holo.setLine(0, "&6Title").addLine("&7Subtitle");
```

### HardHologram — per-player edit

Shows customized lines to a specific player while all others see the global version.

```java
holo.editForPlayer(player)
    .setLine(0, "&aWins: " + wins)
    .addLine("&7Rank: " + rank)
    .removeLine(2)
    .apply();

// reset player back to global lines
holo.resetForPlayer(player);
```

### HardHologram — click handlers

Click detection uses an invisible `Interaction` entity spawned at the hologram center. Setting any handler automatically spawns it; the entity is removed when `despawn()` or `resetForPlayer()` is called.

```java
// both left and right click
holo.onClick(player -> player.sendMessage("clicked!"));

// separate handlers
holo.onRightClick(player -> player.sendMessage("right click"));
holo.onLeftClick(player  -> player.sendMessage("left click"));
```

Handlers can be set after `create()` and are preserved across `setLine` / `addLine` / `move` calls.

### Supported color codes

Standard `&` color codes and `&l`, `&o`, `&n`, `&m`, `&k` formatting codes are supported in line text.

---

## ScoreboardEngine

Creates and manages per-player sidebar scoreboards. Each player gets their own `Scoreboard` instance. Placeholders and colors are resolved every tick.

```java
HardScoreboard sb = ScoreboardEngine.create("main", player,
    "&6&lMój Serwer",
    List.of(
        "&7Gracz: &f{name}",
        "&7Ping: &a{ping}ms",
        "&7Online: &f%server_online%"
    )
);
```

### ScoreboardEngine methods

```java
HardScoreboard sb = ScoreboardEngine.create("id", player, title, rows); // create or replace
HardScoreboard sb = ScoreboardEngine.get(player);                        // get active board, null if none
boolean removed   = ScoreboardEngine.remove(player);                     // remove & restore default scoreboard
ScoreboardEngine.removeAll();                                             // remove all (auto on disable)
```

### HardScoreboard — editing

```java
sb.setTitle("&6Nowy tytuł");
sb.setRow(0, "&7Nowa linia");
sb.setRows(List.of("&7Linia 1", "&7Linia 2"));
sb.remove();
```

All setters return `this` and can be chained.

### Supported placeholders

| Placeholder | Value |
|---|---|
| `{name}` | Player's display name |
| `{ping}` | Player's ping in ms |
| `%papi_placeholder%` | Any PlaceholderAPI placeholder (if PAPI is installed) |

Standard `&` color codes are supported in title and all rows. Maximum 15 rows.

---

## TablistEngine

Creates and manages per-player tablist header/footer. Supports multi-line header and footer, custom player list visibility, and all the same placeholders as ScoreboardEngine.

```java
// single-line header and footer
TablistEngine.create("main", player,
    "&6&lMój Serwer",
    "&7Ping: &a{ping}ms"
);

// multi-line header and footer
TablistEngine.create("main", player,
    List.of("", "&6&lMój Serwer", ""),
    List.of(
        "&7Gracz: &f{name}",
        "&7Ping: &a{ping}ms &8| &7Online: &f%server_online%",
        ""
    )
);
```

### TablistEngine methods

```java
HardTablist tab = TablistEngine.create("id", player, header, footer); // create or replace
HardTablist tab = TablistEngine.get(player);                           // get active tablist, null if none
boolean removed = TablistEngine.remove(player);                        // remove & clear header/footer
TablistEngine.removeAll();                                              // remove all (auto on disable)
```

### HardTablist — header & footer

```java
tab.setHeader("&aNowy nagłówek");
tab.setHeader(List.of("", "&aNagłówek wieloliniowy", ""));

tab.setFooter("&7Stopka");
tab.setFooter(List.of("&7Linia 1", "&7Linia 2"));
```

### HardTablist — player list

By default all online players are visible. Use `setPlayers` to show only a specific subset — everyone else is hidden from the viewer's tab.

```java
tab.setPlayers(List.of(player1, player2, player3)); // hide everyone else
tab.addPlayer(player4);                             // add to visible list
tab.removePlayer(player2);                          // remove from visible list
tab.resetPlayers();                                 // restore all players
```

All methods return `this` and can be chained. The tablist is automatically cleaned up when the player disconnects.

---

## TimeEngine

```java
TimeEngine time = new TimeEngine(plugin);

// run once after delay
time.setTimeout(() -> player.sendMessage("3 seconds passed"), 60L);  // 60 ticks = 3s

// run repeatedly
time.setInterval(() -> Bukkit.broadcastMessage("tick"), 20L);  // every second
```

---

## WebEngine

HTTP server for communication between Paper and Velocity (or any HTTP client). Uses AES-256-GCM encryption — both sides must share the same key set in `webengine.yml`.

### Config — `hardgamez/configs/HardGamezEngine/config.yml`

```yaml
webserver:
  enabled: true
  port: 8080
  encryption:
    enabled: true
    key: "your-secret-key"
```

### Defining endpoints (Paper side)

```java
WebEngine.init(plugin);  // called automatically in HardGamezEngine.onEnable()

WebEngine.createEndpoint("/api/player")
    .requireParam("uuid")
    .returnField("<uuid>.stats.pvp.wins",      p -> String.valueOf(Stats.get(p.get("uuid")).getWins()))
    .returnField("<uuid>.stats.pvp.kills",     p -> String.valueOf(Stats.get(p.get("uuid")).getKills()))
    .returnField("<uuid>.stats.economy.coins", p -> String.valueOf(Stats.get(p.get("uuid")).getCoins()))
    .returnField("<uuid>.meta.rank",           p -> Stats.get(p.get("uuid")).getRank())
    .register();
```

Response JSON (dot-separated keys become nested objects, infinitely deep):

```json
{
  "550e8400-...": {
    "stats": {
      "pvp":     { "wins": 42, "kills": 137 },
      "economy": { "coins": 500 }
    },
    "meta": { "rank": "vip" }
  }
}
```

### Reading POST data sent by Velocity

```java
WebEngine.createEndpoint("/api/sync")
    .requireParam("uuid")
    .readField("coins")
    .readField("rank")
    .register();
// response includes: { "received.coins": "500", "received.rank": "vip" }
```

### Reading endpoint data locally

`readEndpoint` calls the endpoint in-memory — no HTTP round-trip needed when reading from the same server the endpoint is registered on.

**Important:** `createEndpoint(...).register()` must be called in `onEnable()` **after** `WebEngine.init()`, before any `readEndpoint` call.

```java
// onEnable() — rejestracja przed użyciem
WebEngine.init(this);

WebEngine.createEndpoint("/api/stats")
    .requireParam("uuid")
    .returnField("<uuid>.wins",  params -> String.valueOf(Stats.get(params.get("uuid")).getWins()))
    .returnField("<uuid>.kills", params -> String.valueOf(Stats.get(params.get("uuid")).getKills()))
    .register();

getCommand("webping").setExecutor(new WebTestPing());
```

```java
// w klasie komendy
String uuid = player.getUniqueId().toString();
EndpointReader reader = WebEngine.readEndpoint("/api/stats", Map.of("uuid", uuid));

String wins  = reader.read("wins");
int    kills = reader.readInt("kills");
```

`read(path)` uses dot-notation relative to the response root — no need to include the uuid prefix. Available methods: `read(path)` → String, `readInt(path)`, `readDouble(path)`, `readBoolean(path)`, `rawJson()`.

### Querying a remote endpoint (Velocity → Paper)

Use `query` when the endpoint is on a different server. Decryption is applied automatically if the key matches.

```java
String json = WebEngine.query(
    "http://paper-host:8080/api/stats",
    Map.of("uuid", player.getUniqueId().toString())
);
```

POST body is accepted as `application/json` or `application/x-www-form-urlencoded`. Missing required params return HTTP 400.

---

## WebhookEngine

Sends Discord webhook messages asynchronously (non-blocking).

```java
WebhookEngine.sendWebhook("https://discord.com/api/webhooks/...")
    .setUsername("HardGamez")
    .setAvatarUrl("https://...")
    .setContent("Optional plain text above the embed")
    .setTitle("Player joined")
    .setDescription("Steve connected to the server")
    .setColor(0x8B00FF)
    .setAuthor("HardGamez", null, null)
    .addField("Wins",  "42",  true)
    .addField("Kills", "137", true)
    .setFooter("HardGamezEngine", null)
    .setThumbnail("https://...")
    .setImage("https://...")
    .build();
```

All setter methods are optional. `.build()` fires the request on a Bukkit async thread.

---

## Built-in commands

### `/hardgamez` (aliases: `/hge`, `/hgz`)

**Permission:** `hardgamez.admin` (default: OP)

Full tab completion is supported for all subcommands and plugin IDs.

| Subcommand | Description |
|---|---|
| `/hardgamez info` | List of all available HardGamezEngine commands |
| `/hardgamez reload` | Reload all config and language files for **all** registered plugins |
| `/hardgamez authors` | Display authors and copyright information |
| `/hardgamez download` | Browse the HardGamez plugin catalogue |
| `/hardgamez download <id>` | Download a plugin JAR directly into the `plugins/` folder |

### Plugin catalogue

The download list is fetched from the URL defined in `HardGamezEngine.java`:

```java
public static final String PLUGINS_LIST_URL = "https://hardgamez.pl/api/plugins.json";
```

Expected JSON format:

```json
{
  "core": {
    "name": "HardCore",
    "version": "1.0",
    "downloadURL": "https://example.com/HardCore-1.0.jar"
  },
  "pvp": {
    "name": "HardPvP",
    "version": "2.1",
    "downloadURL": "https://example.com/HardPvP-2.1.jar"
  }
}
```

The list is fetched asynchronously (no server lag). Tab completion for plugin IDs is cached on startup and refreshed with each `/hardgamez download` call. Downloaded JARs are saved as `plugins/<name>-<version>.jar` and require a server restart to load.

---

## Player data storage backends

Set in `config.yml`:

```yaml
data_storage:
  type: YAML       # YAML | JSON | MONGODB
  register_ip_addresses: true

database:          # only for MONGODB
  host: localhost
  port: 27017
  username: ""
  password: ""
  database: hardgamez
```

---

## NpcEngine

Creates and manages server-side NPCs. Human NPCs (`NpcType.HUMAN`) are implemented as fake `ServerPlayer` entities sent via NMS packets — they look exactly like players and support custom skins. Mob NPCs use Bukkit's entity API with AI disabled.

```java
NpcObject npc = NpcEngine.register(NpcType.HUMAN, "Steve", location)
    .setSkinName("Notch")
    .setLooking(true)
    .clickEvent(e -> {
        if (e.isRightClick()) e.getPlayer().sendMessage("Right clicked!");
        if (e.isLeftClick())  e.getPlayer().sendMessage("Left clicked!");
    })
    .hideNickname(false)
    .invicible(true)
    .setScale(1.0f)
    .setGlowing(false)
    .setWaypoints(new Location[]{ loc1, loc2, loc3 })
    .loopType(LoopType.SHERIFF)
    .spawn();
```

### NpcEngine methods

```java
NpcObject npc = NpcEngine.register(NpcType.HUMAN, "name");                     // no location (set later)
NpcObject npc = NpcEngine.register(NpcType.HUMAN, "name", location);           // with spawn location
NpcObject npc = NpcEngine.get(uuid);                                            // get by NPC uuid
Collection<NpcObject> all = NpcEngine.getAll();                                 // get all registered NPCs
boolean removed = NpcEngine.remove(uuid);                                       // remove by uuid
NpcEngine.removeAll();                                                           // remove all (auto on disable)
```

### NpcObject — configuration (before and after spawn)

```java
npc.setSkinURL("https://textures.minecraft.net/texture/...");  // skin from texture URL
npc.setSkinName("Notch");                                       // skin fetched by player name (async)
npc.setSkinUUID(uuid);                                          // skin fetched by player UUID (async)
npc.setLooking(true);                                           // track and look at nearest player
npc.hideNickname(true);                                         // hide the name tag
npc.setInventory(inventory);                                    // equip items (slots: 0=main, 1=off, 2-5=armor)
npc.invicible(true);                                            // make invulnerable (default: true)
npc.setWaypoints(new Location[]{ ... });                        // set movement waypoints
npc.loopType(LoopType.FIRST_POINT);                             // set waypoint loop type
npc.setScale(1.5f);                                             // resize the NPC (1.21+ servers only)
npc.setGlowing(true);                                           // enable glow outline
npc.setGlowColor(ChatColor.AQUA);                               // set glow color
npc.setSilent(true);                                            // suppress sounds
npc.setGravity(false);                                          // disable gravity
npc.setCollidable(false);                                       // disable player collision
npc.setSneaking(true);                                          // put NPC in crouching pose (HUMAN only)
npc.setName("NewName");                                         // change display name
npc.setLocation(location);                                      // teleport NPC
```

### NpcObject — actions

```java
npc.clearWaypoints();    // stop movement, teleport back to spawn
npc.remove();            // despawn and unregister NPC
```

### NpcType

| Type | Description |
|---|---|
| `HUMAN` | Fake player entity — supports custom skins, player model |
| `ZOMBIE` | Zombie mob |
| `SKELETON` | Skeleton mob |
| `CREEPER` | Creeper mob |
| `SPIDER` / `CAVE_SPIDER` | Spider variants |
| `ENDERMAN` | Enderman |
| `BLAZE` | Blaze |
| `WITCH` | Witch |
| `WITHER_SKELETON` | Wither Skeleton |
| `PILLAGER` / `VINDICATOR` / `EVOKER` | Illager variants |
| `PHANTOM` | Phantom |
| `DROWNED` / `HUSK` / `STRAY` | Zombie/Skeleton variants |
| `ZOMBIE_VILLAGER` | Zombie Villager |
| `PIGLIN` / `PIGLIN_BRUTE` / `ZOMBIFIED_PIGLIN` | Piglin variants |
| `HOGLIN` / `ZOGLIN` | Hoglin variants |
| `GUARDIAN` / `ELDER_GUARDIAN` | Guardians |
| `SHULKER` | Shulker |
| `SILVERFISH` / `ENDERMITE` | Small arthropods |
| `VEX` | Vex |
| `RAVAGER` | Ravager |
| `WARDEN` | Warden |

### LoopType

| Type | Pattern (3 waypoints) | Description |
|---|---|---|
| `FIRST_POINT` | `1 → 2 → 3 → 1 → 2 → 3` | After last, jump to first and repeat |
| `SHERIFF` | `1 → 2 → 3 → 2 → 1 → 2 → 3` | Ping-pong back and forth |
| `NONE` | `1 → 2 → 3` | Walk once, stop at last waypoint |

### NpcClickEvent

```java
npc.clickEvent(e -> {
    Player player     = e.getPlayer();
    NpcObject npc     = e.getNpc();
    ClickType type    = e.getClickType();   // LEFT or RIGHT
    boolean isLeft    = e.isLeftClick();
    boolean isRight   = e.isRightClick();
});
```

### Getters

```java
UUID id          = npc.getId();
NpcType type     = npc.getNpcType();
String name      = npc.getName();
Location loc     = npc.getLocation();
Entity entity    = npc.getEntity();
boolean spawned  = npc.isSpawned();
boolean looking  = npc.isLooking();
boolean invincible = npc.isInvincible();
LoopType loop    = npc.getLoopType();
NpcSkin skin     = npc.getSkin();
```

### Notes

- Human NPCs are sent purely via packets — they do not exist in the server's entity list. This means they appear to clients but will not trigger normal entity events.
- Click detection for HUMAN NPCs uses `EntityDamageByEntityEvent` (left click) and `PlayerInteractAtEntityEvent` (right click) on the fake player entity.
- Skin fetching (`setSkinName`, `setSkinUUID`, `setSkinURL`) is done asynchronously. If called before `.spawn()`, the skin is applied at spawn time. If called after, the NPC is respawned with the new skin.
- Human NPC names are truncated to 16 characters (Minecraft limit for player names in `GameProfile`).
