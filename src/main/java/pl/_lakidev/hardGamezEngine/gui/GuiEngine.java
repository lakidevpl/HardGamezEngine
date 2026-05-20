package pl._lakidev.hardGamezEngine.gui;

public class GuiEngine {
    public static ChestGUI chest(String title, int rows) {
        if (rows < 1) rows = 1;
        if (rows > 6) rows = 6;
        return new ChestGUI(title, rows);
    }
}

/*
PRZYKŁAD UŻYCIA
ChestGUI gui = GuiEngine.chest("§8Menu", 3);

gui.fillBorder(Material.BLACK_STAINED_GLASS_PANE);

gui.addItem(13, Material.DIAMOND_SWORD)
    .setTitle("§bUltra Miecz")
    .setLore("§7Kliknij aby kupić", "§6Cena: §e100$")
    .setEnchanted(true)
    .moveItem(false)
    .setPermission("shop.buy")
    .setCooldown(500)
    .onClick(e -> e.getPlayer().sendMessage("§aDodano do koszyka!"))
    .onRightClick(e -> e.getPlayer().sendMessage("§eInfo o itemie"))
    .build();

gui.setCancelable(true);
gui.open(player);

*/