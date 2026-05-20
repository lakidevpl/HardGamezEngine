package pl._lakidev.hardGamezEngine.webhook;

import org.bukkit.Bukkit;
import pl._lakidev.hardGamezEngine.HardGamezEngine;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookEngine {

    private final String url;

    private String username    = null;
    private String avatarUrl   = null;
    private String content     = null;

    private String title       = null;
    private String description = null;
    private Integer color      = null;
    private String footerText  = null;
    private String footerIcon  = null;
    private String imageUrl    = null;
    private String thumbnailUrl = null;
    private String authorName  = null;
    private String authorUrl   = null;
    private String authorIcon  = null;

    private final java.util.List<EmbedField> fields = new java.util.ArrayList<>();

    private WebhookEngine(String url) {
        this.url = url;
    }

    public static WebhookEngine sendWebhook(String url) {
        return new WebhookEngine(url);
    }

    public WebhookEngine setUsername(String username) {
        this.username = username;
        return this;
    }

    public WebhookEngine setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public WebhookEngine setContent(String content) {
        this.content = content;
        return this;
    }

    public WebhookEngine setTitle(String title) {
        this.title = title;
        return this;
    }

    public WebhookEngine setDescription(String description) {
        this.description = description;
        return this;
    }

    public WebhookEngine setColor(int color) {
        this.color = color;
        return this;
    }

    public WebhookEngine setColor(java.awt.Color color) {
        this.color = color.getRGB() & 0xFFFFFF;
        return this;
    }

    public WebhookEngine setFooter(String text) {
        this.footerText = text;
        return this;
    }

    public WebhookEngine setFooter(String text, String iconUrl) {
        this.footerText = text;
        this.footerIcon = iconUrl;
        return this;
    }

    public WebhookEngine setImage(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public WebhookEngine setThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public WebhookEngine setAuthor(String name) {
        this.authorName = name;
        return this;
    }

    public WebhookEngine setAuthor(String name, String url) {
        this.authorName = name;
        this.authorUrl  = url;
        return this;
    }

    public WebhookEngine setAuthor(String name, String url, String iconUrl) {
        this.authorName = name;
        this.authorUrl  = url;
        this.authorIcon = iconUrl;
        return this;
    }

    public WebhookEngine addField(String name, String value) {
        fields.add(new EmbedField(name, value, false));
        return this;
    }

    public WebhookEngine addField(String name, String value, boolean inline) {
        fields.add(new EmbedField(name, value, inline));
        return this;
    }

    public void build() {
        Bukkit.getScheduler().runTaskAsynchronously(HardGamezEngine.getInstance(), () -> {
            try {
                String json = buildJson();

                URL target = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) target.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "HardGamezEngine-Webhook/1.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    HardGamezEngine.getInstance().getLogger().warning(
                            "[WebhookEngine] Webhook failed — HTTP " + responseCode + " for URL: " + url);
                }

                conn.disconnect();
            } catch (Exception e) {
                HardGamezEngine.getInstance().getLogger().warning(
                        "[WebhookEngine] Exception while sending webhook: " + e.getMessage());
            }
        });
    }

    private String buildJson() {
        StringBuilder sb = new StringBuilder("{");

        if (content != null)   appendField(sb, "content",    escape(content));
        if (username != null)  appendField(sb, "username",   escape(username));
        if (avatarUrl != null) appendField(sb, "avatar_url", escape(avatarUrl));

        boolean hasEmbed = title != null || description != null || color != null
                || footerText != null || imageUrl != null || thumbnailUrl != null
                || authorName != null || !fields.isEmpty();

        if (hasEmbed) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"embeds\":[{");

            boolean needsComma = false;

            if (title != null) {
                sb.append("\"title\":").append(escape(title));
                needsComma = true;
            }
            if (description != null) {
                if (needsComma) sb.append(",");
                sb.append("\"description\":").append(escape(description));
                needsComma = true;
            }
            if (color != null) {
                if (needsComma) sb.append(",");
                sb.append("\"color\":").append(color);
                needsComma = true;
            }
            if (authorName != null) {
                if (needsComma) sb.append(",");
                sb.append("\"author\":{\"name\":").append(escape(authorName));
                if (authorUrl != null)  sb.append(",\"url\":").append(escape(authorUrl));
                if (authorIcon != null) sb.append(",\"icon_url\":").append(escape(authorIcon));
                sb.append("}");
                needsComma = true;
            }
            if (!fields.isEmpty()) {
                if (needsComma) sb.append(",");
                sb.append("\"fields\":[");
                for (int i = 0; i < fields.size(); i++) {
                    EmbedField f = fields.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"name\":").append(escape(f.name))
                      .append(",\"value\":").append(escape(f.value))
                      .append(",\"inline\":").append(f.inline).append("}");
                }
                sb.append("]");
                needsComma = true;
            }
            if (imageUrl != null) {
                if (needsComma) sb.append(",");
                sb.append("\"image\":{\"url\":").append(escape(imageUrl)).append("}");
                needsComma = true;
            }
            if (thumbnailUrl != null) {
                if (needsComma) sb.append(",");
                sb.append("\"thumbnail\":{\"url\":").append(escape(thumbnailUrl)).append("}");
                needsComma = true;
            }
            if (footerText != null) {
                if (needsComma) sb.append(",");
                sb.append("\"footer\":{\"text\":").append(escape(footerText));
                if (footerIcon != null) sb.append(",\"icon_url\":").append(escape(footerIcon));
                sb.append("}");
            }

            sb.append("}]");
        }

        sb.append("}");
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String key, String value) {
        if (sb.length() > 1) sb.append(",");
        sb.append("\"").append(key).append("\":").append(value);
    }

    private static String escape(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    private record EmbedField(String name, String value, boolean inline) {}
}
