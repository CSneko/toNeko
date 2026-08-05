package org.cneko.toneko.common.mod.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import org.cneko.toneko.common.mod.util.ResourceLocationUtil;

public class ToNekoStructures {
    public static StructureProcessorType<NekoHutProcessor> NEKO_HUT_PROCESSOR;

    public static void init() {
        NEKO_HUT_PROCESSOR = Registry.register(
                BuiltInRegistries.STRUCTURE_PROCESSOR,
                ResourceLocationUtil.toNekoLoc("neko_hut_processor"),
                () -> NekoHutProcessor.CODEC
        );
    }
}
