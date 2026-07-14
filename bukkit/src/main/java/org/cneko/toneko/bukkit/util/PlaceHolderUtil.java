package org.cneko.toneko.bukkit.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceHolderUtil {
    private static boolean isPapiEnabled = false;
    public static void init(){
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            isPapiEnabled = true;
        }catch (ClassNotFoundException e){
            isPapiEnabled = false;
        }
    }

    public static String replace(Player player, String message){
        if (isPapiEnabled){
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }
}
