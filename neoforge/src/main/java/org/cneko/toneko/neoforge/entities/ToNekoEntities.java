package org.cneko.toneko.neoforge.entities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import java.util.Set;

import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;
public class ToNekoEntities {
    public static DeferredHolder<EntityType<?>, EntityType<?>> CRYSTAL_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<?>> ADVENTURER_NEKO_HOLDER;
    public static void init(){
        CRYSTAL_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register("crystal_neko",
                ()-> EntityType.Builder.of(CrystalNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build("crystal_neko")
        );
        ADVENTURER_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register("adventurer_neko",
                ()-> EntityType.Builder.of(AdventurerNeko::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build("adventurer_neko")
        );

        // 注册皮肤
        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
        // 注册名字
        Set<String> names = Set.of(
                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina","Ayame","Cinnamon","Momo"
        );
        NekoNameRegistry.register(names);


    }
}
