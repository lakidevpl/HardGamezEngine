package pl._lakidev.hardGamezEngine.npc;

import org.bukkit.entity.Player;

public class NpcClickEvent {

    public enum ClickType {
        LEFT,
        RIGHT
    }

    private final Player player;
    private final NpcObject npc;
    private final ClickType clickType;

    NpcClickEvent(Player player, NpcObject npc, ClickType clickType) {
        this.player = player;
        this.npc = npc;
        this.clickType = clickType;
    }

    public Player getPlayer() { return player; }
    public NpcObject getNpc() { return npc; }
    public ClickType getClickType() { return clickType; }
    public boolean isLeftClick()  { return clickType == ClickType.LEFT; }
    public boolean isRightClick() { return clickType == ClickType.RIGHT; }
}
