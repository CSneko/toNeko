package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 火源检测与高温系数：附近有篝火/熔炉/火/岩浆时返回高温系数，
 * 统一驱动「快速晾干」与「高温增味」两条曲线（贴近现实：高温蒸发水分并挥发气味分子）。
 */
public class FireSourceUtil {
    public static final float FIRE_HEAT_MULTIPLIER = 3.0f;

    /** 该方块状态是否算火源（点燃的篝火/熔炉/高炉/烟熏炉、火方块、岩浆、岩浆锅） */
    public static boolean isFireBlock(BlockState state) {
        if (state.is(Blocks.FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.LAVA_CAULDRON)) return true;
        if (state.getBlock() instanceof CampfireBlock) return state.getValue(CampfireBlock.LIT);
        if (state.getBlock() instanceof AbstractFurnaceBlock) return state.getValue(AbstractFurnaceBlock.LIT);
        return false;
    }

    /** 高温系数：中心半径 radius 格内有火源返回 {@link #FIRE_HEAT_MULTIPLIER}，否则 1.0 */
    public static float heat(Level level, BlockPos center, int radius) {
        if (level == null) return 1.0f;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (isFireBlock(level.getBlockState(pos))) {
                return FIRE_HEAT_MULTIPLIER;
            }
        }
        return 1.0f;
    }
}
