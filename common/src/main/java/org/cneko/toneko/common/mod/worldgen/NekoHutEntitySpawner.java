package org.cneko.toneko.common.mod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;

import java.util.HashSet;
import java.util.Set;

/**
 * 猫娘小屋实体生成器。
 * 当 chunk 加载时检测是否存在聚合台方块，若存在则生成 AdventurerNeko。
 * 使用已处理 chunk 集合避免重复生成。
 */
public class NekoHutEntitySpawner {
    private static final Set<ChunkPos> processedChunks = new HashSet<>();

    public static void checkChunk(ServerLevel level, ChunkPos chunkPos) {
        if (processedChunks.contains(chunkPos)) return;

        Block aggregator = ToNekoBlocks.NEKO_AGGREGATOR;
        BlockPos start = chunkPos.getWorldPosition();

        // 扫描 chunk 中是否存在聚合台
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = start.offset(x, y, z);
                    if (level.getBlockState(pos).is(aggregator)) {
                        spawnNeko(level, pos);
                        processedChunks.add(chunkPos);
                        return;
                    }
                }
            }
        }
        // 未找到也标记，避免重复扫描
        processedChunks.add(chunkPos);
    }

    private static void spawnNeko(ServerLevel level, BlockPos aggregatorPos) {
        AdventurerNeko neko = ToNekoEntities.ADVENTURER_NEKO.create(level);
        if (neko != null) {
            neko.setPos(aggregatorPos.getX() + 0.5, aggregatorPos.getY() + 1, aggregatorPos.getZ() + 0.5);
            neko.setPersistenceRequired();
            level.addFreshEntity(neko);
        }
    }
}
