package org.cneko.toneko.neoforge.fabric.api;

import java.util.HashMap;

public class PlayerInstallToNeko {
    // 用来检查玩家是否安装ToNeko
    public static HashMap<String, Boolean> m = new HashMap<>();

    public static void set(String playerName, boolean status){
        m.put(playerName, status);
    }
    public static void remove(String playerName){
        m.remove(playerName);
    }
    public static boolean get(String playerName){
        return m.getOrDefault(playerName, false);
    }
}
