package org.cneko.toneko.common.mod.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 方块实体注册容器（common 声明，平台模块注册并回填）。
 */
public class ToNekoBlockEntities {
    public static BlockEntityType<ClotheslineBlockEntity> CLOTHESLINE;

    /**
     * 封装 {@link BlockEntityType.Builder#build} 的 null 参数：
     * neoforge 模块编译期解析不到 datafixerupper 的 Type，故把 BlockEntityType 的构造
     * 收敛到 common（此处可解析），平台模块只传方块。
     */
    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> build(
            BlockEntityType.BlockEntitySupplier<T> supplier, net.minecraft.world.level.block.Block... validBlocks) {
        return BlockEntityType.Builder.of(supplier, validBlocks).build(null);
    }
}
