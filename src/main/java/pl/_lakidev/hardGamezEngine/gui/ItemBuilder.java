package pl._lakidev.hardGamezEngine.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import pl._lakidev.hardGamezEngine.HardGamezEngine;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemBuilder {

    public enum SlotType {
        DECORATION,
        CLICKABLE,
        INPUT
    }

    private final ChestGUI gui;
    private final int slot;
    private final Material material;

    private Supplier<String> titleSupplier        = null;
    private Supplier<List<String>> loreSupplier   = null;
    private Supplier<String> skullTextureSupplier = null;

    private String staticTitle       = null;
    private List<String> staticLore  = null;
    private String staticTexture     = null;
    private String skullOwnerName    = null;
    private UUID skullOwnerUUID      = null;

    private int amount               = 1;
    private boolean enchanted        = false;
    private boolean unbreakable      = false;
    private boolean dynamic          = false;
    private final List<Enchantment> enchants     = new ArrayList<>();
    private final List<Integer> enchantLevels    = new ArrayList<>();
    private final List<ItemFlag> flags           = new ArrayList<>();
    private Integer customModelData  = null;

    private boolean movable          = false;
    private SlotType slotType        = SlotType.CLICKABLE;
    private String permission        = null;
    private long cooldownMs          = 0L;

    private Consumer<GuiClickEvent> onClickAction = null;
    private Consumer<GuiClickEvent> onRightClick  = null;
    private Consumer<GuiClickEvent> onLeftClick   = null;
    private Consumer<GuiClickEvent> onShiftClick  = null;

    ItemBuilder(ChestGUI gui, int slot, Material material) {
        this.gui      = gui;
        this.slot     = slot;
        this.material = material;
    }

    // --- static setters ---

    public ItemBuilder setTitle(String title) {
        this.staticTitle = title;
        return this;
    }

    public ItemBuilder setLore(String... lines) {
        this.staticLore = Arrays.asList(lines);
        return this;
    }

    public ItemBuilder setLore(List<String> lines) {
        this.staticLore = new ArrayList<>(lines);
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (staticLore == null) staticLore = new ArrayList<>();
        staticLore.add(line);
        return this;
    }

    public ItemBuilder setSkullTexture(String textureUrl) {
        this.staticTexture = textureUrl;
        return this;
    }

    public ItemBuilder setSkullOwner(String playerName) {
        this.skullOwnerName = playerName;
        return this;
    }

    public ItemBuilder setSkullOwner(UUID uuid) {
        this.skullOwnerUUID = uuid;
        return this;
    }

    // --- dynamic suppliers (enable auto-refresh) ---

    public ItemBuilder setTitle(Supplier<String> supplier) {
        this.titleSupplier = supplier;
        this.dynamic = true;
        return this;
    }

    public ItemBuilder setLore(Supplier<List<String>> supplier) {
        this.loreSupplier = supplier;
        this.dynamic = true;
        return this;
    }

    public ItemBuilder setSkullTexture(Supplier<String> supplier) {
        this.skullTextureSupplier = supplier;
        this.dynamic = true;
        return this;
    }

    /** Wymusza tryb dynamiczny — item będzie przebudowywany przy każdym renderze GUI. */
    public ItemBuilder setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
        return this;
    }

    // --- pozostałe ustawienia ---

    public ItemBuilder setAmount(int amount) {
        this.amount = Math.max(1, Math.min(amount, 64));
        return this;
    }

    public ItemBuilder setEnchanted(boolean enchanted) {
        this.enchanted = enchanted;
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchant, int level) {
        enchants.add(enchant);
        enchantLevels.add(level);
        return this;
    }

    public ItemBuilder addFlags(ItemFlag... itemFlags) {
        flags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        this.customModelData = data;
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public ItemBuilder moveItem(boolean movable) {
        this.movable = movable;
        return this;
    }

    public ItemBuilder onClick(Consumer<GuiClickEvent> action) {
        this.onClickAction = action;
        return this;
    }

    public ItemBuilder onRightClick(Consumer<GuiClickEvent> action) {
        this.onRightClick = action;
        return this;
    }

    public ItemBuilder onLeftClick(Consumer<GuiClickEvent> action) {
        this.onLeftClick = action;
        return this;
    }

    public ItemBuilder onShiftClick(Consumer<GuiClickEvent> action) {
        this.onShiftClick = action;
        return this;
    }

    public ItemBuilder setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public ItemBuilder setCooldown(long milliseconds) {
        this.cooldownMs = milliseconds;
        return this;
    }

    public ItemBuilder setSlotType(SlotType type) {
        this.slotType = type;
        return this;
    }

    // --- build ---

    public ChestGUI build() {
        Supplier<ItemStack> supplier = dynamic ? this::buildStack : null;
        ItemStack staticStack = dynamic ? null : buildStack();

        GuiItem guiItem = new GuiItem(
                staticStack, supplier,
                movable, slotType, permission, cooldownMs,
                onClickAction, onRightClick, onLeftClick, onShiftClick
        );
        gui.registerItem(slot, guiItem);
        return gui;
    }

    ItemStack buildRaw() {
        return buildStack();
    }

    private ItemStack buildStack() {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String title = titleSupplier != null ? titleSupplier.get() : staticTitle;
        if (title != null) meta.setDisplayName(title);

        List<String> lore = loreSupplier != null ? loreSupplier.get() : staticLore;
        if (lore != null) meta.setLore(new ArrayList<>(lore));

        if (enchanted) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        for (int i = 0; i < enchants.size(); i++) {
            meta.addEnchant(enchants.get(i), enchantLevels.get(i), true);
        }
        if (!flags.isEmpty()) meta.addItemFlags(flags.toArray(new ItemFlag[0]));
        if (customModelData != null) meta.setCustomModelData(customModelData);
        meta.setUnbreakable(unbreakable);

        stack.setItemMeta(meta);

        if (meta instanceof SkullMeta skullMeta) {
            String texture = skullTextureSupplier != null ? skullTextureSupplier.get() : staticTexture;
            if (texture != null) {
                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
                    profile.getTextures().setSkin(new URL(texture));
                    skullMeta.setOwnerProfile(profile);
                    stack.setItemMeta(skullMeta);
                } catch (MalformedURLException e) {
                    HardGamezEngine.getInstance().getLogger().warning(
                            "[GuiEngine] Invalid skull texture URL: " + texture);
                }
            } else if (skullOwnerName != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwnerName));
                stack.setItemMeta(skullMeta);
            } else if (skullOwnerUUID != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwnerUUID));
                stack.setItemMeta(skullMeta);
            }
        }

        return stack;
    }
}
