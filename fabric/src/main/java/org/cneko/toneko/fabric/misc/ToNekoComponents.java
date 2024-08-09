package org.cneko.toneko.fabric.misc;

import org.cneko.toneko.fabric.codecs.CountCodecs;

import static org.cneko.toneko.common.Bootstrap.MODID;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ToNekoComponents {
    public static final DataComponentType<CountCodecs.FloatCountCodec> NEKO_PROGRESS_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "neko_progress"),
            DataComponentType.<CountCodecs.FloatCountCodec>builder().persistent(CountCodecs.FLOAT_COUNT_CODEC).build()
    );
}
