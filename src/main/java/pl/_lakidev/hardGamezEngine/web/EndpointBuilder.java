package pl._lakidev.hardGamezEngine.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EndpointBuilder {

    private final WebEngine engine;
    private final String path;

    private final List<String> requiredParams = new ArrayList<>();
    private final List<String[]> returnFields = new ArrayList<>();
    private final List<Function<Map<String, String>, String>> valueSuppliers = new ArrayList<>();
    private final List<String> readFields = new ArrayList<>();

    EndpointBuilder(WebEngine engine, String path) {
        this.engine = engine;
        this.path   = path;
    }

    public EndpointBuilder requireParam(String paramName) {
        requiredParams.add(paramName);
        return this;
    }

    public EndpointBuilder returnField(String keyTemplate, Function<Map<String, String>, String> valueSupplier) {
        returnFields.add(new String[]{ keyTemplate });
        valueSuppliers.add(valueSupplier);
        return this;
    }

    public EndpointBuilder readField(String fieldName) {
        readFields.add(fieldName);
        return this;
    }

    public WebEngine register() {
        engine.registerEndpoint(path, this);
        return engine;
    }

    String invoke(Map<String, String> params) {
        for (String required : requiredParams) {
            if (!params.containsKey(required) || params.get(required).isBlank()) {
                return "{\"error\":\"Missing required parameter: " + required + "\"}";
            }
        }

        Map<String, String> jsonFields = new LinkedHashMap<>();

        for (int i = 0; i < returnFields.size(); i++) {
            String resolvedKey = resolveTemplate(returnFields.get(i)[0], params);
            String value;
            try {
                value = valueSuppliers.get(i).apply(params);
            } catch (Exception e) {
                value = "error: " + e.getMessage();
            }
            jsonFields.put(resolvedKey, value);
        }

        for (String field : readFields) {
            jsonFields.put("received." + field, params.getOrDefault(field, ""));
        }

        return buildJson(jsonFields);
    }

    void attachHttpHandler() {
        engine.registerHttpHandler(path, exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed — use POST\"}");
                return;
            }

            Map<String, String> params = parsePostBody(exchange);
            String json = invoke(params);

            String responseBody;
            if (engine.hasCrypto()) {
                responseBody = "{\"encrypted\":\"" + engine.getCrypto().encrypt(json) + "\"}";
            } else {
                responseBody = json;
            }

            sendResponse(exchange, 200, responseBody);
        });
    }

    private static Map<String, String> parsePostBody(HttpExchange exchange) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (body.startsWith("{")) {
                result = parseSimpleJson(body);
            } else {
                for (String pair : body.split("&")) {
                    if (pair.isEmpty()) continue;
                    String[] kv = pair.split("=", 2);
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                    result.put(k, v);
                }
            }
        }
        return result;
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0, json.length() - 1);

        for (String token : json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            int colon = token.indexOf(':');
            if (colon < 0) continue;
            String key   = token.substring(0, colon).trim().replaceAll("^\"|\"$", "");
            String value = token.substring(colon + 1).trim().replaceAll("^\"|\"$", "");
            map.put(key, value);
        }
        return map;
    }

    private static String resolveTemplate(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> e : params.entrySet()) {
            result = result.replace("<" + e.getKey() + ">", e.getValue());
        }
        return result;
    }

    private static String buildJson(Map<String, String> fields) {
        Map<String, Object> tree = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            insertIntoTree(tree, e.getKey().split("\\.", -1), 0, e.getValue());
        }
        return serializeTree(tree);
    }

    @SuppressWarnings("unchecked")
    private static void insertIntoTree(Map<String, Object> node, String[] parts, int depth, String value) {
        String key = parts[depth];
        if (depth == parts.length - 1) {
            node.put(key, value);
            return;
        }
        Object existing = node.get(key);
        Map<String, Object> child;
        if (existing instanceof Map) {
            child = (Map<String, Object>) existing;
        } else {
            child = new LinkedHashMap<>();
            node.put(key, child);
        }
        insertIntoTree(child, parts, depth + 1, value);
    }

    @SuppressWarnings("unchecked")
    private static String serializeTree(Map<String, Object> tree) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : tree.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(jsonEscape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v instanceof Map) {
                sb.append(serializeTree((Map<String, Object>) v));
            } else {
                String s = v == null ? "" : v.toString();
                if (s.matches("-?\\d+(\\.\\d+)?") || s.equals("true") || s.equals("false")) {
                    sb.append(s);
                } else {
                    sb.append("\"").append(jsonEscape(s)).append("\"");
                }
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
