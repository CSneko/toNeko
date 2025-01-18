package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;
import java.util.function.Supplier;

public class ToNekoEntities {

    public static EntityType<AdventurerNeko> ADVENTURER_NEKO;
    public static EntityType<CrystalNekoEntity> CRYSTAL_NEKO;
    public static EntityType<GhostNekoEntity> GHOST_NEKO;

    public static void init() {
        // 注册名字
        Set<String> names = Set.of(
                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina","Ayame","Cinnamon","Momo"
        );
        NekoNameRegistry.register(names);


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
