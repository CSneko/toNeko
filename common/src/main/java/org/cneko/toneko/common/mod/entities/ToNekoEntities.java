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
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;
import java.util.function.Supplier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEntities {

    public static EntityType<AdventurerNeko> ADVENTURER_NEKO;
    public static EntityType<CrystalNekoEntity> CRYSTAL_NEKO;

    public static void init() {

    }

    @ApiStatus.Internal
    public static Supplier<EntityType<CrystalNekoEntity>> getCrystalNeko(){
        return
                ()-> EntityType.Builder.of(CrystalNekoEntity::new, MobCategory.CREATURE)
                .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                .build("crystal_neko");
    }
    @ApiStatus.Internal
    public static Supplier<EntityType<AdventurerNeko>> getAdventurerNeko(){
        return
                ()-> EntityType.Builder.of(AdventurerNeko::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build("adventure_neko");
    }

}
