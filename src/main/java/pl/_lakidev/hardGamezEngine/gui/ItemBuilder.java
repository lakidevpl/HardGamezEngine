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

public class ItemBuilder {

    public enum SlotType {
        DECORATION,
        CLICKABLE,
        INPUT
    }

    private final ChestGUI gui;
    private final int slot;
    private ItemStack item;
    private ItemMeta meta;

    private boolean movable = false;
    private SlotType slotType = SlotType.CLICKABLE;
    private String permission = null;
    private long cooldownMs = 0L;

    private Consumer<GuiClickEvent> onClickAction    = null;
    private Consumer<GuiClickEvent> onRightClick     = null;
    private Consumer<GuiClickEvent> onLeftClick      = null;
    private Consumer<GuiClickEvent> onShiftClick     = null;

    ItemBuilder(ChestGUI gui, int slot, Material material) {
        this.gui  = gui;
        this.slot = slot;
        this.item = new ItemStack(material, 1);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setTitle(String title) {
        if (meta != null) meta.setDisplayName(title);
        return this;
    }

    public ItemBuilder setLore(String... lines) {
        if (meta != null) meta.setLore(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder setLore(List<String> lines) {
        if (meta != null) meta.setLore(new ArrayList<>(lines));
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (meta == null) return this;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(line);
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(Math.max(1, Math.min(amount, 64)));
        return this;
    }

    public ItemBuilder setEnchanted(boolean enchanted) {
        if (meta == null) return this;
        if (enchanted) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(Enchantment.LUCK);
        }
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchant, int level) {
        if (meta != null) meta.addEnchant(enchant, level, true);
        return this;
    }

    public ItemBuilder addFlags(ItemFlag... flags) {
        if (meta != null) meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (meta != null) meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder setSkullOwner(String playerName) {
        if (!(meta instanceof SkullMeta skullMeta)) return this;
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        meta = skullMeta;
        return this;
    }

    public ItemBuilder setSkullOwner(UUID uuid) {
        if (!(meta instanceof SkullMeta skullMeta)) return this;
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        meta = skullMeta;
        return this;
    }

    public ItemBuilder setSkullTexture(String textureUrl) {
        if (!(meta instanceof SkullMeta skullMeta)) return this;
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
            profile.getTextures().setSkin(new URL(textureUrl));
            skullMeta.setOwnerProfile(profile);
            meta = skullMeta;
        } catch (MalformedURLException e) {
            HardGamezEngine.getInstance().getLogger().warning(
                    "[GuiEngine] Invalid skull texture URL: " + textureUrl);
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (meta != null) meta.setUnbreakable(unbreakable);
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

    public ChestGUI build() {
        item.setItemMeta(meta);
        GuiItem guiItem = new GuiItem(
                item, movable, slotType, permission, cooldownMs,
                onClickAction, onRightClick, onLeftClick, onShiftClick
        );
        gui.registerItem(slot, guiItem);
        return gui;
    }

    ItemStack buildRaw() {
        item.setItemMeta(meta);
        return item;
    }
}