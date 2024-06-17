package com.crystalneko.tonekofabric.api;

import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class Messages {
    public static final Map<String,Boolean> trash = new HashMap<>();
    public static Text translatable(String key){
        return Text.translatable(key);
    }
    public static Text translatable(String key,String[] replace){
        return Text.translatable(key, (Object[]) replace);
    }
    public static Text translatable(String key,String player){
        if(key.startsWith("msg.toneko")){
            if(trash.containsKey(player) && trash.get(player)){
                key = key + ".trash";
            }
        }
        return Text.translatable(key);
    }
    public static void setTrash(String player,boolean trash){
        Messages.trash.put(player,trash);
    }
}
