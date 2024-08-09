package org.cneko.toneko.neoforge.fabric.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
public class TextUtil {
    public static Component translatable(String key, Object... args){
        return Component.translatable(key, args);
    }
    public static Component translatable(String key){
        return Component.translatable(key);
    }
    public static String getPlayerName(Player player){
        String playerName = player.getName().getString();
        playerName = playerName.replace("literal{", "").replace("}", "");
        return playerName;
    }
}
