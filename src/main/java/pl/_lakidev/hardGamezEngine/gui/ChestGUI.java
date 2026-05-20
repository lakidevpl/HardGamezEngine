package pl._lakidev.hardGamezEngine.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import pl._lakidev.hardGamezEngine.HardGamezEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChestGUI implements Listener {

    private final String title;
    private final int rows;
    private final int size;

    private String id = null;
    private boolean cancelable = true;
    private int currentPage = 0;

    private final Map<Integer, GuiItem> items      = new HashMap<>();
    private final Map<String, Object>  data        = new HashMap<>();
    private final Map<UUID, Long>      cooldowns   = new HashMap<>();

    private Consumer<Player> onOpenAction  = null;
    private Consumer<Player> onCloseAction = null;

    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    private boolean listenerRegistered = false;
    private BukkitTask autoRefreshTask = null;

    ChestGUI(String title, int rows) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.rows  = rows;
        this.size  = rows * 9;
    }

    public ItemBuilder addItem(int slot, Material material) {
        return new ItemBuilder(this, slot, material);
    }

    void registerItem(int slot, GuiItem item) {
        items.put(slot, item);
    }

    public ChestGUI fill(Material material) {
        ItemStack stack = makeDeco(material);
        for (int i = 0; i < size; i++) {
            if (!items.containsKey(i)) {
                items.put(i, decoItem(stack));
            }
        }
        return this;
    }

    public ChestGUI fillBorder(Material material) {
        ItemStack stack = makeDeco(material);
        for (int i = 0; i < 9; i++) placeDecoIfEmpty(i, stack);           // top row
        for (int i = size - 9; i < size; i++) placeDecoIfEmpty(i, stack); // bottom row
        for (int row = 1; row < rows - 1; row++) {
            placeDecoIfEmpty(row * 9, stack);         // left
            placeDecoIfEmpty(row * 9 + 8, stack);     // right
        }
        return this;
    }

    public ChestGUI setBackground(Material material) {
        ItemStack stack = makeDeco(material);
        for (int i = 0; i < size; i++) {
            GuiItem existing = items.get(i);
            if (existing == null || existing.getSlotType() == ItemBuilder.SlotType.DECORATION) {
                items.put(i, decoItem(stack));
            }
        }
        return this;
    }

    public ChestGUI setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }

    public ChestGUI onOpen(Consumer<Player> action) {
        this.onOpenAction = action;
        return this;
    }

    public ChestGUI onClose(Consumer<Player> action) {
        this.onCloseAction = action;
        return this;
    }

    public ChestGUI setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() { return id; }

    public ChestGUI setData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) data.get(key);
    }

    public ChestGUI setPage(int page) {
        this.currentPage = Math.max(0, page);
        return this;
    }

    public int getPage() { return currentPage; }

    public ChestGUI nextPage() {
        currentPage++;
        update();
        return this;
    }

    public ChestGUI previousPage() {
        if (currentPage > 0) currentPage--;
        update();
        return this;
    }

    public ChestGUI update() {
        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            renderInto(entry.getValue());
        }
        return this;
    }

    /**
     * Uruchamia automatyczne odświeżanie wszystkich dynamicznych itemów co intervalTicks ticków.
     * Zatrzymuje poprzedni task jeśli był aktywny.
     */
    public ChestGUI startAutoRefresh(long intervalTicks) {
        stopAutoRefresh();
        autoRefreshTask = Bukkit.getScheduler().runTaskTimer(
                HardGamezEngine.getInstance(),
                () -> { if (!openInventories.isEmpty()) update(); },
                intervalTicks, intervalTicks
        );
        return this;
    }

    public ChestGUI stopAutoRefresh() {
        if (autoRefreshTask != null && !autoRefreshTask.isCancelled()) {
            autoRefreshTask.cancel();
        }
        autoRefreshTask = null;
        return this;
    }

    public ChestGUI refreshSlot(int slot) {
        GuiItem guiItem = items.get(slot);
        ItemStack stack = (guiItem != null) ? guiItem.getItem() : null;
        for (Inventory inv : openInventories.values()) {
            inv.setItem(slot, stack);
        }
        return this;
    }

    public void open(Player player) {
        ensureListenerRegistered();

        Inventory inv = Bukkit.createInventory(null, size, title);
        renderInto(inv);

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    public void close(Player player) {
        UUID uuid = player.getUniqueId();
        if (openInventories.containsKey(uuid)) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory openInv = openInventories.get(uuid);
        if (openInv == null) return;
        if (!event.getInventory().equals(openInv)) return;

        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(openInv)) {
            if (cancelable) event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();
        GuiItem guiItem = items.get(slot);

        if (guiItem == null) {
            if (cancelable) event.setCancelled(true);
            return;
        }

        if (guiItem.getSlotType() == ItemBuilder.SlotType.INPUT) return;

        event.setCancelled(!guiItem.isMovable());

        if (guiItem.getPermission() != null
                && !player.hasPermission(guiItem.getPermission())) return;

        if (guiItem.getCooldownMs() > 0) {
            long now  = System.currentTimeMillis();
            long last = cooldowns.getOrDefault(uuid, 0L);
            if (now - last < guiItem.getCooldownMs()) return;
            cooldowns.put(uuid, now);
        }

        ClickType clickType = event.getClick();
        GuiClickEvent clickEvent = new GuiClickEvent(player, slot, clickType, event, this);

        if (clickType.isShiftClick() && guiItem.getOnShiftClick() != null) {
            guiItem.getOnShiftClick().accept(clickEvent);
            return;
        }
        if (clickType == ClickType.RIGHT && guiItem.getOnRightClick() != null) {
            guiItem.getOnRightClick().accept(clickEvent);
            return;
        }
        if (clickType == ClickType.LEFT && guiItem.getOnLeftClick() != null) {
            guiItem.getOnLeftClick().accept(clickEvent);
            return;
        }
        if (guiItem.getOnClick() != null) {
            guiItem.getOnClick().accept(clickEvent);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!openInventories.containsKey(uuid)) return;
        if (!event.getInventory().equals(openInventories.get(uuid))) return;

        if (onOpenAction != null) onOpenAction.accept(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        if (!openInventories.containsKey(uuid)) return;
        if (!event.getInventory().equals(openInventories.get(uuid))) return;

        openInventories.remove(uuid);
        cooldowns.remove(uuid);

        if (onCloseAction != null) onCloseAction.accept(player);

        if (openInventories.isEmpty()) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
            stopAutoRefresh();
        }
    }

    private void renderInto(Inventory inv) {
        inv.clear();
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, entry.getValue().getItem());
            }
        }
    }

    private void ensureListenerRegistered() {
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, HardGamezEngine.getInstance());
            listenerRegistered = true;
        }
    }

    private ItemStack makeDeco(Material material) {
        ItemStack stack = new ItemStack(material);
        var meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private GuiItem decoItem(ItemStack stack) {
        return new GuiItem(stack, null, false, ItemBuilder.SlotType.DECORATION,
                null, 0L, null, null, null, null);
    }

    private void placeDecoIfEmpty(int slot, ItemStack stack) {
        if (!items.containsKey(slot)) items.put(slot, decoItem(stack));
    }
}