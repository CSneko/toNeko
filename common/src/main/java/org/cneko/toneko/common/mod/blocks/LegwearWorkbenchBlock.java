package org.cneko.toneko.common.mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes;
import org.jetbrains.annotations.NotNull;

/**
 * 腿部服饰工作台：放入丝袜（或未来裙装），用 GUI 滑杆免费调节 D 值与袜口高度。
 * 无 BE 模式（与 NekoAggregator 一致）：打开时 new SimpleContainer(1) 交给内嵌 Menu。
 */
public class LegwearWorkbenchBlock extends Block {

    public LegwearWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new LegwearWorkbenchMenu(
                            containerId, playerInventory, new SimpleContainer(1), ContainerLevelAccess.create(level, pos)),
                    this.getName()
            ));
            return InteractionResult.PASS;
        }
    }

    public static class LegwearWorkbenchMenu extends AbstractContainerMenu {
        private final Container container;
        private final ContainerLevelAccess access;

        public LegwearWorkbenchMenu(int containerId, Inventory playerInventory) {
            this(containerId, playerInventory, new SimpleContainer(1), ContainerLevelAccess.NULL);
        }

        public LegwearWorkbenchMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access) {
            super(ToNekoMenuTypes.LEGWEAR_WORKBENCH, containerId);
            checkContainerSize(container, 1);
            this.container = container;
            this.access = access;

            // 槽 0：腿部服饰输入槽（丝袜/未来裙装）
            this.addSlot(new Slot(container, 0, 17, 24) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return LegwearItem.isLegwear(stack);
                }
            });

            // 玩家物品栏
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
                }
            }
            // 快捷栏
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
            }
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack copy = slot.getItem().copy();

            if (index == 0) {
                // 工作台槽 → 玩家背包
                if (!this.moveItemStackTo(slot.getItem(), 1, 37, true)) return ItemStack.EMPTY;
            } else if (index < 37) {
                // 玩家背包 → 工作台槽（mayPlace 会拒绝非腿部服饰）
                if (!this.moveItemStackTo(slot.getItem(), 0, 1, false)) return ItemStack.EMPTY;
            }
            slot.setChanged();
            return copy;
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            // 方块被挖掉/离开太远时关闭 GUI
            return this.access.evaluate((level, pos) ->
                    level.getBlockState(pos).getBlock() instanceof LegwearWorkbenchBlock, true);
        }

        @Override
        public void removed(@NotNull Player player) {
            super.removed(player);
            // 关 GUI 时把槽里物品丢回玩家
            this.access.execute((level, pos) -> this.clearContainer(player, this.container));
        }
    }
}
