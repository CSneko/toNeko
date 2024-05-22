package org.cneko.toneko.fabric.util;

import net.minecraft.text.Text;

public class TextUtil {
    public static Text translatable(String key, Object... args){
        return Text.translatable(key, args);
    }
    public static Text translatable(String key){
        return Text.translatable(key);
    }
}
