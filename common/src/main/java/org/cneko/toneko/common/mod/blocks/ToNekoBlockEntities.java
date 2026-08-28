package org.cneko.toneko.common.mod.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块实体注册容器（common 声明，平台模块注册并回填）。
 * 26.1.2：BlockEntityType 构造器与 BlockEntitySupplier 均为私有，
 * 需通过 FabricBlockEntityTypeBuilder 创建。
 */
public class ToNekoBlockEntities {
    public static BlockEntityType<ClotheslineBlockEntity> CLOTHESLINE;

    /** 与 26.1.2 的 BlockEntitySupplier 等价的函数式接口（原版该接口为私有） */
    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    public static <T extends BlockEntity> BlockEntityType<T> build(
            BlockEntitySupplier<T> supplier, net.minecraft.world.level.block.Block... validBlocks) {
        return FabricBlockEntityTypeBuilder.create(supplier::create, validBlocks).build();
    }
}
