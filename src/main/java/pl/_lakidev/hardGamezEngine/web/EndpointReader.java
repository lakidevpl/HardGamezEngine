package pl._lakidev.hardGamezEngine.web;

import java.util.LinkedHashMap;
import java.util.Map;

public class EndpointReader {

    private final String json;
    private final Map<String, String> params;

    EndpointReader(String json, Map<String, String> params) {
        this.json   = json;
        this.params = params;
    }

    public String read(String pathTemplate) {
        String path = resolveTemplate(pathTemplate, params);
        String[] parts = path.split("\\.", -1);
        return traverse(json, parts, 0);
    }

    public int readInt(String pathTemplate) {
        String val = read(pathTemplate);
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }

    public double readDouble(String pathTemplate) {
        String val = read(pathTemplate);
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return 0.0; }
    }

    public boolean readBoolean(String pathTemplate) {
        return Boolean.parseBoolean(read(pathTemplate));
    }

    public String rawJson() {
        return json;
    }

    private static String resolveTemplate(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> e : params.entrySet()) {
            result = result.replace("<" + e.getKey() + ">", e.getValue());
        }
        return result;
    }

    private static String traverse(String json, String[] parts, int depth) {
        String key = parts[depth];
        String objectJson = extractObject(json, key);

        if (depth == parts.length - 1) {
            if (objectJson != null && objectJson.startsWith("{")) {
                return objectJson;
            }
            return extractScalar(json, key);
        }

        if (objectJson == null) return null;
        return traverse(objectJson, parts, depth + 1);
    }

    private static String extractObject(String json, String key) {
        String search = "\"" + key + "\":{";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length() - 1;

        int depth = 0;
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(start, i + 1);
            }
            i++;
        }
        return null;
    }

    private static String extractScalar(String json, String key) {
        String searchStr = "\"" + key + "\":\"";
        int start = json.indexOf(searchStr);
        if (start >= 0) {
            start += searchStr.length();
            int end = json.indexOf("\"", start);
            return end >= 0 ? json.substring(start, end) : null;
        }

        String searchNum = "\"" + key + "\":";
        start = json.indexOf(searchNum);
        if (start >= 0) {
            start += searchNum.length();
            int end = start;
            while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }

        return null;
    }
}
