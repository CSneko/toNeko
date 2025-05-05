package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.AmmunitionEntity;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;

public class ToNekoEntities {
    public static void init(){
        ADVENTURER_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ADVENTURER_NEKO_ID,
                FabricEntityType.Builder.createMob(AdventurerNeko::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(AdventurerNeko::createAdventurerNekoAttributes)
                        ).
                        sized(0.5f,1.7f).eyeHeight(1.6f).build()
        );
        CRYSTAL_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                CRYSTAL_NEKO_ID,
                FabricEntityType.Builder.createMob(CrystalNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(CrystalNekoEntity::createNekoAttributes)
                        )
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8).build()
        );
        GHOST_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                GHOST_NEKO_ID,
                FabricEntityType.Builder.createMob(GhostNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(GhostNekoEntity::createGhostNekoAttributes)
                )
                        .sized(0.4f,1.2f).eyeHeight(1.5f).clientTrackingRange(8).build()
        );

        AMMUNITION_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                AMMUNITION_ENTITY_ID,
                EntityType.Builder.of(AmmunitionEntity::new, MobCategory.MISC)
                        .sized(0.25f,0.25f).build()
        );

        org.cneko.toneko.common.mod.entities.ToNekoEntities.init();

        // 注册皮肤
        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
        NekoSkinRegistry.register(GHOST_NEKO,GhostNekoEntity.nekoSkins);

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
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN).or(BiomeSelectors.tag(BiomeTags.IS_FOREST)), MobCategory.CREATURE, ADVENTURER_NEKO, 5, 1, 1); // 在主世界的高山会生成一只
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS).or(BiomeSelectors.tag(BiomeTags.HAS_JUNGLE_TEMPLE)).or(BiomeSelectors.tag(BiomeTags.HAS_NETHER_FOSSIL)), MobCategory.CREATURE, GHOST_NEKO, 5, 1, 1); // 与史莱姆一起生成
        if (ConfigUtil.IS_BIRTHDAY){
            BiomeModifications.addSpawn(BiomeSelectors.all(), MobCategory.CREATURE, CRYSTAL_NEKO, 10, 1, 4); // 在所有世界生成一只
        }
    }
}
