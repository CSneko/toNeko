package org.cneko.toneko.fabric.misc;

import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.codecs.CountCodecs;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoComponents {
    public static final ComponentType<CountCodecs.FloatCountCodec> NEKO_PROGRESS_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "neko_progress"),
            ComponentType.<CountCodecs.FloatCountCodec>builder().codec(CountCodecs.FLOAT_COUNT_CODEC).build()
    );
}
