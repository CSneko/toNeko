package org.cneko.toneko.common.mod.entities;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEntities {

    public static EntityType<AdventurerNeko> ADVENTURER_NEKO;
    public static EntityType<CrystalNekoEntity> CRYSTAL_NEKO;

    public static void init() {

    }

}
