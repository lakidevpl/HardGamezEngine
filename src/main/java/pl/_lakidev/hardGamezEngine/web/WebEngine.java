package pl._lakidev.hardGamezEngine.web;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import pl._lakidev.hardGamezEngine.HardGamezEngine;
import pl._lakidev.hardGamezEngine.config.HardConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebEngine {

    private static WebEngine instance;

    private HttpServer server;
    private WebCrypto  crypto;

    private final int     port;
    private final String  secretKey;
    private final boolean encryptionEnabled;

    private final Map<String, EndpointBuilder> endpoints      = new HashMap<>();
    private final Map<String, HttpHandler>     pendingHandlers = new HashMap<>();

    private WebEngine(int port, String secretKey, boolean encryptionEnabled) {
        this.port              = port;
        this.secretKey         = secretKey;
        this.encryptionEnabled = encryptionEnabled;
    }

    public static WebEngine init(HardGamezEngine plugin) {
        HardConfig cfg = HardGamezEngine.mainConfig;

        if (!cfg.getBoolean("webserver.enabled")) {
            Bukkit.getConsoleSender().sendMessage("§d  [INFO] §7WebEngine -> Disabled in config.");
            return null;
        }

        int     port      = cfg.getInt("webserver.port", 8080);
        boolean encEnable = cfg.getBoolean("webserver.encryption.enabled", true);
        String  key       = cfg.getString("webserver.encryption.key", "changeme-set-a-strong-key");

        instance = new WebEngine(port, key, encEnable);
        instance.start(plugin);
        return instance;
    }

    public static WebEngine getInstance() { return instance; }

    public static EndpointBuilder createEndpoint(String path) {
        if (instance == null) throw new IllegalStateException("[WebEngine] Not initialised — call WebEngine.init() first");
        return new EndpointBuilder(instance, path);
    }

    public static EndpointReader readEndpoint(String path, Map<String, String> params) {
        if (instance == null) throw new IllegalStateException("[WebEngine] Not initialised");
        EndpointBuilder endpoint = instance.endpoints.get(path);
        if (endpoint == null) throw new IllegalArgumentException(
                "[WebEngine] No endpoint registered at: " + path
                + " — call createEndpoint(...).register() in onEnable() after WebEngine.init()");
        String json = endpoint.invoke(params);
        return new EndpointReader(json, params);
    }

    public static String query(String targetUrl, Map<String, String> params) {
        if (instance == null) throw new IllegalStateException("[WebEngine] Not initialised");
        try {
            StringBuilder body = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!first) body.append(",");
                body.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
                first = false;
            }
            body.append("}");

            String rawResponse = httpPost(targetUrl, body.toString());

            if (instance.encryptionEnabled && rawResponse.contains("\"encrypted\"")) {
                String encrypted = extractJsonString(rawResponse, "encrypted");
                return instance.crypto.decrypt(encrypted);
            }
            return rawResponse;
        } catch (Exception e) {
            throw new RuntimeException("[WebEngine] Query failed: " + e.getMessage(), e);
        }
    }

    void registerEndpoint(String path, EndpointBuilder endpoint) {
        endpoints.put(path, endpoint);
        endpoint.attachHttpHandler();
    }

    void registerHttpHandler(String path, HttpHandler handler) {
        if (server != null) {
            server.createContext(path, handler);
        } else {
            pendingHandlers.put(path, handler);
        }
    }

    boolean hasCrypto() { return encryptionEnabled && crypto != null; }
    WebCrypto getCrypto() { return crypto; }

    private void start(HardGamezEngine plugin) {
        try {
            if (encryptionEnabled) {
                if ("changeme-set-a-strong-key".equals(secretKey)) {
                    Bukkit.getConsoleSender().sendMessage(
                            "§c  [WARN] §7WebEngine -> Using default encryption key — set webserver.encryption.key in config.yml!");
                }
                crypto = new WebCrypto(secretKey);
            }

            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            for (Map.Entry<String, HttpHandler> e : pendingHandlers.entrySet()) {
                server.createContext(e.getKey(), e.getValue());
            }
            pendingHandlers.clear();

            server.start();
            Bukkit.getConsoleSender().sendMessage("§d  [INFO] §7WebEngine -> HTTP server started on port " + port
                    + (encryptionEnabled ? " (encryption: ON)" : " (encryption: OFF)"));
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("§c  [WARN] §7WebEngine -> Failed to start HTTP server: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (server != null) {
            server.stop(1);
            Bukkit.getConsoleSender().sendMessage("§d  [INFO] §7WebEngine -> HTTP server stopped.");
        }
    }

    private static String httpPost(String url, String jsonBody) throws IOException {
        java.net.HttpURLConnection conn =
                (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "HardGamezEngine-WebEngine/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (java.io.InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) throw new RuntimeException("[WebEngine] Key not found in response: " + key);
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
