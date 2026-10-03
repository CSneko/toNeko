package org.cneko.toneko.common.mod.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.util.NekoIds;

public class CoffeeCropBlock extends CropBlock {
    public static final MapCodec<CoffeeCropBlock> CODEC = simpleCodec(p -> new CoffeeCropBlock());

    public CoffeeCropBlock() {
        super(NekoIds.blockProps("coffee_crop").mapColor(MapColor.PLANT).noCollision()
                .randomTicks().instabreak().sound(SoundType.CROP));
    }

    @Override public MapCodec<? extends CropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return ToNekoItems.COFFEE_BEANS; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(2, 0, 2, 14, 2 + getAge(state) * 2, 14);
    }
}
