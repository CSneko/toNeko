package com.crystalneko.tonekofabric.api;

import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class Query {
    public static boolean EVERYONE = false;
    /**
     * 获取猫娘的主人名称
     * @param neko 猫娘名称
     * @param worldName 世界名称
     * @return 主人名称
     */
    public static String getOwner(String neko,String worldName){
        if(EVERYONE){
            return "Crystal_Neko";
        }
        return sqlite.getColumnValue(worldName+"Nekos","owner","neko",neko);
    }

    /**
     * 获取猫娘是否有主人
     * @param neko 猫娘名称
     * @param worldName 主人名称
     * @return 是否有主人
     */
    public static boolean hasOwner(String neko,String worldName){
        if(EVERYONE){
            return true;
        }
        return sqlite.checkValueExists(worldName+"Nekos","neko",neko);
    }

    /**
     * 判断是否是猫娘
     * @param neko 猫娘名称
     * @param worldName 世界名称
     * @return 是否是猫娘
     */
    public static boolean isNeko(String neko,String worldName){
        return hasOwner(neko,worldName);
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
