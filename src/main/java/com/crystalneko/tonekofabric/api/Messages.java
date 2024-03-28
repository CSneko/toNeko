package com.crystalneko.tonekofabric.api;

import net.minecraft.text.Text;

public class Messages {
    public static Text translatable(String key){
        return Text.translatable(key);
    }
    public static Text translatable(String key,String[] replace){
        return Text.translatable(key, (Object[]) replace);
    }
}
