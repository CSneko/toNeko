package org.cneko.toneko.common.mod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import org.cneko.toneko.common.mod.util.ResourceLocationUtil;

public class ToNekoStructures {
    // 26.1.2：STRUCTURE_PROCESSOR 注册表存 StructureProcessorType（codec() 返回 MapCodec）
    public static StructureProcessorType<NekoHutProcessor> NEKO_HUT_PROCESSOR;

    public static void init() {
        NEKO_HUT_PROCESSOR = Registry.register(
                BuiltInRegistries.STRUCTURE_PROCESSOR,
                ResourceLocationUtil.toNekoLoc("neko_hut_processor"),
                () -> NekoHutProcessor.CODEC
        );
    }
}
