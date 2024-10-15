package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEntities {
    private static final TagKey<Biome> IS_MOUNTAIN = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c","is_mountain"));

    public static final EntityType<AdventurerNeko> ADVENTURER_NEKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID,"adventurer_neko"),
            FabricEntityType.Builder.createMob(AdventurerNeko::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(AdventurerNeko::createAdventurerNekoAttributes)
//                    .spawnRestriction(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, AdventurerNeko::checkMobSpawnRules)
                    ).
                    sized(0.5f,1.7f).eyeHeight(1.6f).build()
    );
    public static final EntityType<CrystalNekoEntity> CRYSTAL_NEKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID,"crystal_neko"),
            FabricEntityType.Builder.createMob(CrystalNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(CrystalNekoEntity::createNekoAttributes)
//                            .spawnRestriction(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrystalNekoEntity::checkCrystalNekoSpawnRules)
                    )
                    .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8).build()
    );

    public static void init() {
        // 注册皮肤
        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
        // 注册名字
        Set<String> names = Set.of(
                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina","Ayame","Cinnamon","Momo"
        );
        NekoNameRegistry.register(names);

        /*
        不知道为什么喵，我测试的时候总是不生成，真的好奇怪的问题
        后来测试了很多次喵，都没生成
        这个我也是改来改去的喵，就是很奇怪
        哪怕我看了其它模组的代码，和我的似乎也差不多，但是我的就是不生成喵
        太她喵的奇怪了！
        但是喵...
        重新创建了个世界，它生成了喵！好逆天的Bug喵！
         */
        // 设置生成条件
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.CREATURE, ADVENTURER_NEKO, 5, 1, 1); // 在主世界的高山会生成一只

        if (ConfigUtil.IS_BIRTHDAY){
            BiomeModifications.addSpawn(BiomeSelectors.all(), MobCategory.CREATURE, CRYSTAL_NEKO, 10, 1, 4); // 在所有世界生成一只
//            SpawnPlacements.register(CRYSTAL_NEKO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrystalNekoEntity::checkMobSpawnRules);
        }
    }

}
