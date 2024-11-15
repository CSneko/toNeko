package org.cneko.toneko.common.mod.blocks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoBlocks {
    public static final Block CATNIP = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "catnip"),
            new CropBlock(Block.Properties.of().noCollission().sound(SoundType.CROP).mapColor(MapColor.PLANT).randomTicks().instabreak().pushReaction(PushReaction.DESTROY)));

    public static void init() {
    }
}
