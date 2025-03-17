package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
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

    public static void init() {
        // 注册名字
        Set<String> names = Set.of(
                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina","Ayame","Cinnamon","Momo",
                "蜜柚柚","桃桃酥","璃玖喵","星奈铃","幽月小菓",
                "にゃん子","綿菓子","千夏コメット","蜜波リリエル","月詠ネオン",
                "Stardustle","Mewblette","Velvetpaw","Luminaeon"
        );
        NekoNameRegistry.register(names);


    }

}
