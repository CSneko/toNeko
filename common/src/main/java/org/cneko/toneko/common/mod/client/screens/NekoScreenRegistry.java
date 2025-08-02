package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.cneko.toneko.common.mod.client.screens.factories.ScreenBuilders;

import java.util.*;

import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;

public class NekoScreenRegistry {
    private static Map<ResourceLocation, NekoScreenBuilder> screens = new HashMap<>();

    public static void register(ResourceLocation id, NekoScreenBuilder builder){
        screens.put(id, builder);
    }
    public static NekoScreenBuilder get(ResourceLocation id){
        return screens.get(id);
    }
    public static NekoScreenBuilder get(EntityType<?> entityType){
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }
    public static void init(){
        register(ADVENTURER_NEKO_ID, ScreenBuilders.COMMON_INTERACTION_SCREEN);
        register(CRYSTAL_NEKO_ID, ScreenBuilders.CRYSTAL_NEKO_INTERACTION_SCREEN);
        register(GHOST_NEKO_ID, ScreenBuilders.COMMON_INTERACTION_SCREEN);
        register(FIGHTING_NEKO_ID, ScreenBuilders.COMMON_INTERACTION_SCREEN);
        register(MOUFLET_NEKO_BOSS_ID, ScreenBuilders.COMMON_INTERACTION_SCREEN);
        register(RAVENN_ID,ScreenBuilders.RAVENN_SCREEN);
    }

}
