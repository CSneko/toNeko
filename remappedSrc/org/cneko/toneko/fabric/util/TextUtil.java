package org.cneko.toneko.fabric.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import static org.cneko.toneko.common.util.LanguageUtil.LANG;
public class TextUtil {
    public static Text translatable(String key, Object... args){
        return Text.translatable(key, args);
    }
    public static Text translatable(String key){
        return Text.translatable(key);
    }
    public static String getPlayerName(PlayerEntity player){
        String playerName = player.getName().getString();
        playerName = playerName.replace("literal{", "").replace("}", "");
        return playerName;
    }
}
