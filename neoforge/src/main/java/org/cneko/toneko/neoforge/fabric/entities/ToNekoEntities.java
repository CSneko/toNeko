package org.cneko.toneko.neoforge.fabric.entities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEntities {
//    public static final EntityType<AdventurerNeko> ADVENTURER_NEKO = Registry.register(
//            BuiltInRegistries.ENTITY_TYPE,
//            ResourceLocation.fromNamespaceAndPath(MODID,"adventurer_neko"),
//            FabricEntityType.Builder.createMob(AdventurerNeko::new, MobCategory.CREATURE,(builder)->{
//                return builder.defaultAttributes(AdventurerNeko::createAdventurerNekoAttributes);
//            }).sized(0.6f,1.8f).eyeHeight(1.7f).build("adventurer_neko")
//    );

    public static void init() {
//        // 注册皮肤
//        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
//        // 注册名字
//        Set<String> names = Set.of(
//                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
//                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
//                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina"
//        );
//        NekoNameRegistry.register(names);
    }

}
