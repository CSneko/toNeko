package org.cneko.toneko.common.mod.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundSource;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.ToNekoSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 晾衣架（方块）：单格、无 GUI。
 * 空手右键 = 取下；拿丝袜右键 = 挂上。
 */
public class ClotheslineBlock extends BaseEntityBlock {
    public static final MapCodec<ClotheslineBlock> CODEC = simpleCodec(ClotheslineBlock::new);

    public ClotheslineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<ClotheslineBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ClotheslineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ClotheslineBlockEntity clothesline) {
                ClotheslineBlockEntity.serverTick(lvl, pos, st, clothesline);
            }
        };
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ClotheslineBlockEntity clothesline && !clothesline.getItem().isEmpty()) {
            ItemStack hanging = clothesline.getItem().copy();
            clothesline.clearItem();
            level.sendBlockUpdated(pos, state, state, 3); // 同步 BE 数据给客户端（渲染挂着的丝袜）
            if (!player.getInventory().add(hanging)) player.drop(hanging, false);
            level.playSound(null, pos, ToNekoSoundEvents.LEGWEAR_RUSTLE, SoundSource.BLOCKS, 0.8f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (LegwearItem.isLegwear(stack)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ClotheslineBlockEntity clothesline && clothesline.getItem().isEmpty()) {
                clothesline.setItem(stack.split(1));
                level.sendBlockUpdated(pos, state, state, 3); // 同步 BE 数据给客户端（渲染挂着的丝袜）
                level.playSound(null, pos, ToNekoSoundEvents.LEGWEAR_RUSTLE, SoundSource.BLOCKS, 0.8f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ClotheslineBlockEntity clothesline && !clothesline.getItem().isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), clothesline.getItem());
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
