package org.cneko.toneko.common.mod.misc;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
}
