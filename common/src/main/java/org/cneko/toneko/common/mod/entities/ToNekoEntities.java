package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;
import java.util.function.Supplier;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoEntities {
    public static ResourceLocation ADVENTURER_NEKO_ID = toNekoLoc("adventurer_neko");
    public static EntityType<AdventurerNeko> ADVENTURER_NEKO;
    public static ResourceLocation CRYSTAL_NEKO_ID = toNekoLoc("crystal_neko");
    public static EntityType<CrystalNekoEntity> CRYSTAL_NEKO;
    public static ResourceLocation GHOST_NEKO_ID = toNekoLoc("ghost_neko");
    public static EntityType<GhostNekoEntity> GHOST_NEKO;
    public static ResourceLocation FIGHTING_NEKO_ID = toNekoLoc("fighting_neko");
    public static EntityType<FightingNekoEntity> FIGHTING_NEKO;
    public static ResourceLocation MOUFLET_NEKO_BOSS_ID = toNekoLoc("mouflet_neko_boss");
    public static EntityType<MoufletNekoBoss> MOUFLET_NEKO_BOSS;
    public static EntityType<AmmunitionEntity> AMMUNITION_ENTITY;
    public static ResourceLocation AMMUNITION_ENTITY_ID = toNekoLoc("ammunition_entity");
    public static void init() {
        // 注册名字
        Set<String> names = Set.of(
                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina","Ayame","Cinnamon","Momo",
                "蜜柚柚","桃桃酥","璃玖喵","星奈铃","幽月小菓",
                "にゃん子","綿菓子","千夏コメット","蜜波リリエル","月詠ネオン",
                "Stardustle","Mewblette","Velvetpaw","Luminaeon",
                "にゃも","夢羽","みるき","鈴菜","小夜子","月見里","千代乃","雨鈴","未羽"
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
    @ApiStatus.Internal
     public static Supplier<EntityType<FightingNekoEntity>> getFightingNeko(){
        return
                ()-> EntityType.Builder.of(FightingNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build("fighting_neko");
    }
    @ApiStatus.Internal
    public static Supplier<EntityType<MoufletNekoBoss>> getMoufletNekoBoss(){
        return
                ()-> EntityType.Builder.of(MoufletNekoBoss::new, MobCategory.MONSTER)
                        .sized(0.5f,1.6f).clientTrackingRange(8).updateInterval(3)
                        .build("mouflet_neko_boss");
    }
    @ApiStatus.Internal
    public static Supplier<EntityType<AmmunitionEntity>> getAmmunitionEntity(){
        return
                ()-> EntityType.Builder.of(AmmunitionEntity::new, MobCategory.MISC)
                        .sized(0.25f,0.25f).clientTrackingRange(4).updateInterval(20)
                        .build("ammunition_entity");
    }

}
