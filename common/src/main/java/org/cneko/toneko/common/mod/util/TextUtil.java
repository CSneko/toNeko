package org.cneko.toneko.common.mod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TextUtil {
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
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

    public static Component randomTranslatabledComponent(String key,int range, Object... args){
        int num = RANDOM.nextInt(range);
        return Component.translatable(key+"."+num, args);
    }
    public static Component randomTranslatabledComponent(RandomSource random, String key, int range, Object... args){
        int num = random.nextInt(range);
        return Component.translatable(key+"."+num, args);
    }
}
