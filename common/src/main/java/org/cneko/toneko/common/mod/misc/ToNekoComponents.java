package org.cneko.toneko.common.mod.misc;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.cneko.toneko.common.mod.codecs.CountCodecs;
import org.cneko.toneko.common.mod.codecs.Scent;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoComponents {
    public static final DataComponentType<CountCodecs.FloatCountCodec> NEKO_PROGRESS_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "neko_progress"),
            DataComponentType.<CountCodecs.FloatCountCodec>builder().persistent(CountCodecs.FLOAT_COUNT_CODEC).build()
    );

    public static final DataComponentType<ResourceLocation> ITEM_ID_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "item_id"),
            DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC).build()
    );

    // 丝袜丹尼尔值（D 值，厚度），5~120
    public static final DataComponentType<Integer> LEGWEAR_DENIER_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_denier"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)   // 无此配置网络回包时组件被静默丢弃
                    .build()
    );

    // 丝袜袜口高度（0~1，1=髋部/连裤袜顶）
    public static final DataComponentType<Float> LEGWEAR_LENGTH_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_length"),
            DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build()
    );

    // 左腿独立染色（未设置时回退整体 DYED_COLOR）
    public static final DataComponentType<DyedItemColor> LEGWEAR_DYE_LEFT_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_dye_left"),
            DataComponentType.<DyedItemColor>builder()
                    .persistent(DyedItemColor.CODEC)
                    .networkSynchronized(DyedItemColor.STREAM_CODEC)
                    .build()
    );

    // 右腿独立染色（未设置时回退整体 DYED_COLOR）
    public static final DataComponentType<DyedItemColor> LEGWEAR_DYE_RIGHT_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_dye_right"),
            DataComponentType.<DyedItemColor>builder()
                    .persistent(DyedItemColor.CODEC)
                    .networkSynchronized(DyedItemColor.STREAM_CODEC)
                    .build()
    );

    // 丝袜出品人（首写即署名，只写一次；networkSynchronized 必配，否则回包被静默丢弃）
    public static final DataComponentType<String> LEGWEAR_MAKER_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_maker"),
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    // 丝袜气味（intensity 0~100 + wearer 最近穿着者）；networkSynchronized 必配
    public static final DataComponentType<Scent> LEGWEAR_SCENT_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_scent"),
            DataComponentType.<Scent>builder()
                    .persistent(Scent.CODEC)
                    .networkSynchronized(Scent.STREAM_CODEC)
                    .build()
    );

    // 丝袜湿度（0~100，水缸/雨水/游泳沾湿，随时间风干）
    public static final DataComponentType<Integer> LEGWEAR_WET_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "legwear_wet"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    // 变质水：水的变质程度（0~100，0=干净），由丝袜在炼药锅中洗出的气味形成
    public static final DataComponentType<Integer> SPOILED_WATER_SPOILAGE_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "spoiled_water_spoilage"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    // 变质水气味来源（最近穿着者显示名，空串=无）；networkSynchronized 必配
    public static final DataComponentType<String> SPOILED_WATER_WEARER_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "spoiled_water_wearer"),
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    // 晾衣架内容（挂着的腿部服饰；空栈=未挂）
    public static final DataComponentType<ItemStack> CLOTHESLINE_CONTENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "clothesline_content"),
            DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.OPTIONAL_CODEC)
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)
                    .build()
    );
}
