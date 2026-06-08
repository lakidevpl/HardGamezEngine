package pl._lakidev.hardGamezEngine.npc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import pl._lakidev.hardGamezEngine.HardGamezEngine;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class NpcSkin {

    private final String value;
    private final String signature;

    public NpcSkin(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    public String getValue()     { return value; }
    public String getSignature() { return signature; }

    public static void fetchByName(String playerName, java.util.function.Consumer<NpcSkin> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            try {
                NpcSkin skin = fetchByNameSync(playerName);
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> callback.accept(skin));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> callback.accept(null));
            }
        });
    }

    public static void fetchByUUID(UUID uuid, java.util.function.Consumer<NpcSkin> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            try {
                NpcSkin skin = fetchByUUIDSync(uuid.toString().replace("-", ""));
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> callback.accept(skin));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> callback.accept(null));
            }
        });
    }

    public static void fetchByURL(String textureUrl, java.util.function.Consumer<NpcSkin> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            try {
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
                String value = Base64.getEncoder().encodeToString(json.getBytes());
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> callback.accept(new NpcSkin(value, "")));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(HardGamezEngine.getInstance(), () -> callback.accept(null));
            }
        });
    }

    private static NpcSkin fetchByNameSync(String name) throws Exception {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        JsonObject obj = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
        String id = obj.get("id").getAsString();
        return fetchByUUIDSync(id);
    }

    private static NpcSkin fetchByUUIDSync(String id) throws Exception {
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        JsonObject profile = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
        JsonObject property = profile.get("properties").getAsJsonArray().get(0).getAsJsonObject();

        String value = property.get("value").getAsString();
        String signature = property.has("signature") ? property.get("signature").getAsString() : "";
        return new NpcSkin(value, signature);
    }
}
