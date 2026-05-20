package pl._lakidev.hardGamezEngine.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl._lakidev.hardGamezEngine.gui.ChestGUI;
import pl._lakidev.hardGamezEngine.gui.GuiEngine;
import pl._lakidev.hardGamezEngine.lang.LangData;
import pl._lakidev.hardGamezEngine.lang.LangEngine;
import pl._lakidev.hardGamezEngine.lang.MsgEngine;
import pl._lakidev.hardGamezEngine.player.PlayerEngine;

public class Lang implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(sender instanceof Player player) {
            ChestGUI gui = GuiEngine.chest(
                    MsgEngine.langText(sender, "language.title"),
                    3
            );

            for(LangData config : LangEngine.getLanguages()) {
                gui.addItem(
                        config.getSlot(),
                        Material.PLAYER_HEAD
                )
                        .setSkullTexture(config.getHeadURL())
                        .setTitle(
                                MsgEngine.langText(sender, "language.item.title").replace("{language}", config.getName())
                        )
                        .setLore(MsgEngine.langList(sender, "language.item.lore", "{language}", config.getName(), "{author}", config.getAuthor(), "{id}", config.getId()))
                        .onClick((e)->{
                            PlayerEngine.setPlayerLang(player, config.getId());
                            MsgEngine.sendMessage(player, "language.set", "{language}", config.getName());
                            gui.close(player);
                        })
                        .moveItem(false)
                        .build();
            }

            gui.open(player);
        } else {
            MsgEngine.sendMessage(null, "only-player");
            return false;
        }
        return false;
    }
}
