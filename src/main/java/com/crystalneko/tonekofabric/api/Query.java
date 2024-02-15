package com.crystalneko.tonekofabric.api;

import com.crystalneko.ctlibPublic.sql.sqlite;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class Query {
    /**
     * 获取猫娘的主人名称
     * @param neko 猫娘名称
     * @param worldName 世界名称
     * @return 主人名称
     */
    public static String getOwner(String neko,String worldName){
        return sqlite.getColumnValue(worldName+"Nekos","owner","neko",neko);
    }

    /**
     * 获取猫娘是否有主人
     * @param neko 猫娘名称
     * @param worldName 主人名称
     * @return 是否有主人
     */
    public static boolean hasOwner(String neko,String worldName){
        return sqlite.checkValueExists(worldName+"Nekos","neko",neko);
    }

    /**
     * 获取世界名称
     * @param world 世界对象
     * @return 世界名称
     */
    public static String getWorldName(World world){
        String name = world.toString();
        name = name.replace("[","");
        name = name.replace("]","");
        name = name.replace("ServerLevel","");
        return name;
    }

    /**
     * 获取玩家名称
     * @param player 玩家对象
     * @return 玩家名称
     */
    public static String getPlayerName(PlayerEntity player){
        String playerName = player.getName().getString();
        playerName = playerName.replace("literal{", "").replace("}", "");
        return playerName;
    }

    /**
     * 将玩家设置成猫娘
     * @param neko 猫娘名称
     * @param worldName 世界名称
     * @param owner 主人名称
     */
    public static void setPlayerToNeko(String neko,String worldName,String owner) {
        //设置值
        sqlite.saveData(worldName+"Nekos","neko",neko);
        sqlite.saveDataWhere(worldName+"Nekos","xp","neko",neko,"0");
        //设置主人的值
        sqlite.saveDataWhere(worldName+"Nekos","owner","neko",neko,owner);
    }
}
