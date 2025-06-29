package org.cneko.toneko.common.mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes;
import org.jetbrains.annotations.NotNull;

public class NekoAggregator extends Block {
    public static final int INPUT_SLOTS = 9; // 3x3

    public NekoAggregator(Properties properties) {
        super(properties);
    }

    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(new SimpleMenuProvider((containerId, playerInventory, playerEntity) -> new NekoAggregatorMenu(containerId, playerInventory), this.getName()));
            return InteractionResult.PASS;
        }
    }

    public static class NekoAggregatorMenu extends AbstractContainerMenu {
        private final Container container;
        private final ContainerData data;
        private final BlockPos pos;

        public NekoAggregatorMenu(int containerId, Inventory playerInventory) {
            this(containerId, playerInventory, new SimpleContainer(10), new SimpleContainerData(2), BlockPos.ZERO);
        }

        public NekoAggregatorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos pos) {
            super(ToNekoMenuTypes.NEKO_AGGREGATOR, containerId);
            this.container = container;
            this.data = data;
            this.pos = pos;

            // 添加输入槽 (3x3 网格)
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    this.addSlot(new Slot(container, col + row * 3, 30 + col * 18, 17 + row * 18));
                }
            }

            // 添加输出槽
            this.addSlot(new Slot(container, 9, 124, 35) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // 输出槽不能手动放置物品
                }
            });

            // 添加玩家物品栏
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
                }
            }

            // 添加快捷栏
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
            }

            this.addDataSlots(data);
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
            ItemStack itemstack = ItemStack.EMPTY;
            Slot slot = this.slots.get(index);

            if (slot.hasItem()) {
                ItemStack slotStack = slot.getItem();
                itemstack = slotStack.copy();

                // 处理输出槽转移
                if (index == 9) { // 输出槽索引
                    if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                        return ItemStack.EMPTY;
                    }
                    slot.onQuickCraft(slotStack, itemstack);
                }
                // 处理玩家物品栏 → 输入槽
                else if (index >= 10 && index < 46) { // 玩家物品栏
                    if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 处理输入槽 → 玩家物品栏
                else if (index >= 0 && index < 9) { // 输入槽
                    if (!this.moveItemStackTo(slotStack, 10, 46, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (slotStack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                if (slotStack.getCount() == itemstack.getCount()) {
                    return ItemStack.EMPTY;
                }

                slot.onTake(player, slotStack);
            }

            return itemstack;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            // 玩家必须在方块附近（4.5格内）
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }

        // 获取能量数据（用于GUI显示）
        public double getEnergy() {
            return data.get(0) + data.get(1) / 100.0;
        }
    }
}