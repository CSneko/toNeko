package org.cneko.toneko.common.mod.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

public class ToNekoEntities {

    public static EntityType<AdventurerNeko> ADVENTURER_NEKO;
    public static EntityType<CrystalNekoEntity> CRYSTAL_NEKO;
    public static EntityType<GhostNekoEntity> GHOST_NEKO;

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
    @ApiStatus.Internal
    public static Supplier<EntityType<GhostNekoEntity>> getGhostNeko(){
        return
                ()-> EntityType.Builder.of(GhostNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.5f,1.6f).eyeHeight(1.5f).clientTrackingRange(8)
                        .build("ghost_neko");
    }

}
