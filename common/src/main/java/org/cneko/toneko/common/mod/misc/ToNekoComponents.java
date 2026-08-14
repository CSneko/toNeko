package org.cneko.toneko.common.mod.misc;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.DyedItemColor;
import org.cneko.toneko.common.mod.codecs.CountCodecs;

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
}
