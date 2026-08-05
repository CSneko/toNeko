package org.cneko.toneko.common.mod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.jetbrains.annotations.Nullable;

/**
 * 猫娘小屋结构处理器：检测聚合台方块并生成 AdventurerNeko。
 */
public class NekoHutProcessor extends StructureProcessor {
    public static final NekoHutProcessor INSTANCE = new NekoHutProcessor();
    public static final MapCodec<NekoHutProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected StructureProcessorType<?> getType() {
        return ToNekoStructures.NEKO_HUT_PROCESSOR;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader worldView, BlockPos pos, BlockPos blockPos2, StructureTemplate.StructureBlockInfo structureBlockInfo, StructureTemplate.StructureBlockInfo worldInfo, StructurePlaceSettings structurePlaceSettings) {

        Block block = worldInfo.state().getBlock();
        if (block == ToNekoBlocks.NEKO_AGGREGATOR && worldView instanceof WorldGenLevel genLevel) {
            spawnNeko(genLevel, pos);
        }
        return worldInfo;
    }

    private void spawnNeko(WorldGenLevel level, BlockPos aggregatorPos) {
        EntityType<AdventurerNeko> type = ToNekoEntities.ADVENTURER_NEKO;
        AdventurerNeko neko = type.create(((ServerLevelAccessor) level).getLevel());
        if (neko != null) {
            neko.setPos(aggregatorPos.getX() + 0.5, aggregatorPos.getY() + 1, aggregatorPos.getZ() + 0.5);
            neko.setPersistenceRequired();
            ((ServerLevelAccessor) level).addFreshEntity(neko);
        }
    }
}
