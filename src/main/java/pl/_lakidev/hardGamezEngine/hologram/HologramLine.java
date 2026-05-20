package pl._lakidev.hardGamezEngine.hologram;

import org.bukkit.Material;

class HologramLine {

    enum Type { TEXT, ITEM, EMPTY }

    private final Type type;
    private final String raw;       // original string as passed by user
    private final Material material; // only for ITEM lines

    private HologramLine(Type type, String raw, Material material) {
        this.type = type;
        this.raw = raw;
        this.material = material;
    }

    static HologramLine parse(String text) {
        if (text == null) return text("null");
        if (text.equalsIgnoreCase("<EMPTY>")) return new HologramLine(Type.EMPTY, text, null);
        if (text.toUpperCase().startsWith("<ITEM:") && text.endsWith(">")) {
            String name = text.substring(6, text.length() - 1).toUpperCase();
            Material mat = Material.getMaterial(name);
            if (mat != null) return new HologramLine(Type.ITEM, text, mat);
        }
        return text(text);
    }

    private static HologramLine text(String raw) {
        return new HologramLine(Type.TEXT, raw, null);
    }

    Type getType()      { return type; }
    String getRaw()     { return raw; }
    Material getMaterial() { return material; }
}
