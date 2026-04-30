package pl._lakidev.hardGamezEngine.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiClickEvent {

    private final Player player;
    private final int slot;
    private final ClickType clickType;
    private final InventoryClickEvent bukkitEvent;
    private final ChestGUI gui;

    GuiClickEvent(Player player, int slot, ClickType clickType,
                  InventoryClickEvent bukkitEvent, ChestGUI gui) {
        this.player      = player;
        this.slot        = slot;
        this.clickType   = clickType;
        this.bukkitEvent = bukkitEvent;
        this.gui         = gui;
    }

    public Player getPlayer()                    { return player; }

    public int getSlot()                         { return slot; }

    public ClickType getClickType()              { return clickType; }

    public InventoryClickEvent getBukkitEvent()  { return bukkitEvent; }

    public ChestGUI getGui()                     { return gui; }
}