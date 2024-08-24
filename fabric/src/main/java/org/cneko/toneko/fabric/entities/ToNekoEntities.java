package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEntities {
    public static final EntityType<AdventurerNeko> ADVENTURER_NEKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID,"adventurer_neko"),
            FabricEntityType.Builder.createMob(AdventurerNeko::new, MobCategory.CREATURE,(builder)->{
                return builder.defaultAttributes(AdventurerNeko::createAdventurerNekoAttributes);
            }).build()
    );

}
