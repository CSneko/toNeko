package com.crystalneko.tonekofabric.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class TextUtil {
    public static String getPlayerName(PlayerEntity player){
        String playerName = player.getName().getString();
        playerName = playerName.replace("literal{", "").replace("}", "");
        return playerName;
    }

    public static String getWorldName(World world){
        String name = world.toString();
        name = name.replace("[","");
        name = name.replace("]","");
        name = name.replace("ServerLevel","");
        return name;
    }
}
