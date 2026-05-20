package pl._lakidev.hardGamezEngine.gui;

import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

class GuiItem {

    private final ItemStack item;
    private final Supplier<ItemStack> dynamicItem;
    private final boolean movable;
    private final ItemBuilder.SlotType slotType;
    private final String permission;
    private final long cooldownMs;

    private final Consumer<GuiClickEvent> onClick;
    private final Consumer<GuiClickEvent> onRightClick;
    private final Consumer<GuiClickEvent> onLeftClick;
    private final Consumer<GuiClickEvent> onShiftClick;

    GuiItem(
            ItemStack item,
            Supplier<ItemStack> dynamicItem,
            boolean movable,
            ItemBuilder.SlotType slotType,
            String permission,
            long cooldownMs,
            Consumer<GuiClickEvent> onClick,
            Consumer<GuiClickEvent> onRightClick,
            Consumer<GuiClickEvent> onLeftClick,
            Consumer<GuiClickEvent> onShiftClick
    ) {
        this.item         = item;
        this.dynamicItem  = dynamicItem;
        this.movable      = movable;
        this.slotType     = slotType;
        this.permission   = permission;
        this.cooldownMs   = cooldownMs;
        this.onClick      = onClick;
        this.onRightClick = onRightClick;
        this.onLeftClick  = onLeftClick;
        this.onShiftClick = onShiftClick;
    }

    ItemStack getItem() {
        return dynamicItem != null ? dynamicItem.get() : item;
    }

    boolean   isMovable()              { return movable; }
    ItemBuilder.SlotType getSlotType() { return slotType; }
    String    getPermission()          { return permission; }
    long      getCooldownMs()          { return cooldownMs; }

    Consumer<GuiClickEvent> getOnClick()      { return onClick; }
    Consumer<GuiClickEvent> getOnRightClick() { return onRightClick; }
    Consumer<GuiClickEvent> getOnLeftClick()  { return onLeftClick; }
    Consumer<GuiClickEvent> getOnShiftClick() { return onShiftClick; }
}
