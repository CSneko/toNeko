package org.cneko.toneko.common.mod.api;

import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NekoSkinRegistry {
    public static final Map<String, List<String>> SKINS = new HashMap<>();
    public static void register(@NotNull EntityType<?> neko, List<String> skins){
        register(neko.getDescriptionId(),skins);
    }
    public static void register(@NotNull EntityType<?> neko, String skin){
        List<String> skinList = SKINS.getOrDefault(neko.getDescriptionId(),new ArrayList<>());
        skinList.add(skin);
        register(neko,skinList);
    }
    public static void register(@NotNull String neko, List<String> skins){
        List<String> skinList = SKINS.getOrDefault(neko,new ArrayList<>());
        skinList.addAll(skins);
        SKINS.put(neko,skinList);
    }
    public static void register(@NotNull String neko, String skin){
        List<String> skinList = SKINS.getOrDefault(neko,new ArrayList<>());
        skinList.add(skin);
        SKINS.put(neko,skinList);
    }
    public static List<String> getSkins(EntityType<?> neko){
        return SKINS.get(neko.getDescriptionId());
    }
    public static String getRandomSkin(EntityType<?> neko){
        List<String> skins = getSkins(neko);
        if (skins == null){
            return null;
        }
        return skins.get((int)(Math.random()*skins.size()));
    }
}
