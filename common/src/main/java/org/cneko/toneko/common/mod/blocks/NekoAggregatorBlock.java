package org.cneko.toneko.common.mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorInput;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;
import org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes;
import org.cneko.toneko.common.mod.recipes.ToNekoRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NekoAggregatorBlock extends Block {
    public static final int INPUT_SLOTS = 9; // 3x3

    public NekoAggregatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new NekoAggregatorMenu(containerId, playerInventory, new SimpleContainer(10), new SimpleContainerData(2), ContainerLevelAccess.create(level, pos)),
                    this.getName()
            ));
            return InteractionResult.PASS;
        }
    }

    public static class NekoAggregatorMenu extends AbstractContainerMenu {
        public static final int INPUT_SLOTS = 9; // 3x3
        /** 容器数据槽下标：当前配方所需能量（服务端计算并同步到客户端） */
        public static final int DATA_REQUIRED_ENERGY = 0;

        private final Container container;
        private final ContainerData data;
        private final ContainerLevelAccess access;
        public final Player player;


        public NekoAggregatorMenu(int containerId, Inventory playerInventory) {
            this(containerId, playerInventory, new SimpleContainer(10), new SimpleContainerData(2), ContainerLevelAccess.NULL);
        }

        public NekoAggregatorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, ContainerLevelAccess access) {
            super(ToNekoMenuTypes.NEKO_AGGREGATOR, containerId);
            checkContainerSize(container, 10);
            checkContainerDataCount(data, 2);
            this.container = container;
            this.data = data;
            this.access = access;
            this.player = playerInventory.player;


            // 添加输入槽 (3x3 网格)
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {

                    this.addSlot(new InputSlot(this, container, col + row * 3, 30 + col * 18, 17 + row * 18));
                }
            }

            // 添加输出槽
            this.addSlot(new Slot(container, 9, 124, 35) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // 输出槽不能手动放置物品
                }

                @Override
                public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                    // 当玩家从这个槽取出物品时（无论是拖动还是Shift-点击），此方法被调用
                    access.execute((level, blockPos) -> {
                        // 查找匹配的配方
                        Optional<RecipeHolder<NekoAggregatorRecipe>> recipeOptional = findMatchingRecipe(level);
                        if (recipeOptional.isPresent()) {
                            NekoAggregatorRecipe recipe = recipeOptional.get().value();
                            // 检查能量（再次检查以防万一）
                            if (((INeko) player).getNekoEnergy() >= recipe.energy) {
                                // 消耗原料和能量
                                consumeInputs(); // 调用消耗方法
                                ((INeko) player).setNekoEnergy((float) (((INeko) player).getNekoEnergy() - recipe.energy));
                                onInputChanged();
                            }
                        }
                    });

                    super.onTake(player, stack);
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

        public void onInputChanged() {
            this.access.execute((level, blockPos) -> {
                // 确保在服务器端执行配方检查
                if (!level.isClientSide()) {
                    updateResult(level);
                }
            });
        }

        private void updateResult(Level level) {
            // 获取输出槽
            Slot resultSlot = this.slots.get(9);

            // 创建配方输入
            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < INPUT_SLOTS; i++) {
                inputs.add(this.container.getItem(i).copy());
            }
            // 临时诊断日志（仅输入变化时触发）
            org.slf4j.Logger diag = org.slf4j.LoggerFactory.getLogger("ToNekoAggregatorDebug");
            int nonEmpty = 0;
            for (ItemStack s : inputs) if (!s.isEmpty()) nonEmpty++;
            diag.info("updateResult: nonEmptySlots={} energy={}", nonEmpty, ((INeko) player).getNekoEnergy());
            NekoAggregatorInput recipeInput = NekoAggregatorInput.of(3, 3, inputs, 0); // 这里的 energy 只是占位符

            // 查找配方
            Optional<RecipeHolder<NekoAggregatorRecipe>> recipeHolder = findMatchingRecipe(level);
            diag.info("recipe found = {} (input w={} h={})",
                    recipeHolder.map(h -> h.id().identifier().toString()).orElse("NONE"),
                    recipeInput.width(), recipeInput.height());
            // 探针：直接按 id 查配方表，判定配方是否被加载
            try {
                var probeKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        net.minecraft.resources.Identifier.parse("toneko:neko_aggregator/resource/neko_ingot"));
                var rm = (net.minecraft.world.item.crafting.RecipeManager) level.recipeAccess();
                var probe = rm.byKey(probeKey);
                var all = rm.getRecipes();
                long ours = 0;
                for (var h : all) if (h.value().getType() == ToNekoRecipes.NEKO_AGGREGATOR) ours++;
                diag.info("probe neko_ingot={} totalRecipes={} oursType={} serializerReg={}",
                        probe.isPresent(), all.size(), ours,
                        net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER
                                .containsKey(net.minecraft.resources.Identifier.parse("toneko:neko_aggregator")));
                // 实时绑定状态
                var bellKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.parse("toneko:neko_bell"));
                Boolean bellBound = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(bellKey)
                        .map(java.util.function.Function.identity()).map(h -> h.areComponentsBound()).orElse(null);
                diag.info("neko_bell bound={} at craft time", bellBound);
                // 实时解析：用服务器资源管理器 + 服务器注册表上下文重演配方加载
                var server = ((net.minecraft.server.level.ServerLevel) level).getServer();
                var res = server.getResourceManager()
                        .getResource(net.minecraft.resources.Identifier.parse("toneko:recipe/neko_aggregator/resource/neko_ingot.json"));
                if (res.isEmpty()) {
                    diag.info("LIVE: recipe file NOT in server resource manager");
                } else {
                    try (var in = res.get().open()) {
                        var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in));
                        var ops = server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
                        var dr = net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops, json);
                        diag.info("LIVE parse: {}", dr.result().isPresent() ? "OK" : "FAILED");
                        dr.resultOrPartial(msg -> diag.info("LIVE parse error: {}", msg));
                    }
                }
            } catch (Throwable t) {
                diag.info("probe failed: {}", t.toString());
            }

            if (recipeHolder.isPresent()) {
                NekoAggregatorRecipe recipe = recipeHolder.get().value();
                // 所需能量同步到客户端（ContainerData 随 broadcastChanges 自动同步）
                this.data.set(DATA_REQUIRED_ENERGY, (int) recipe.energy);
                // 检查能量是否足够
                if (((INeko) player).getNekoEnergy() >= recipe.energy) {
                    // 合成并设置结果
                    ItemStack resultStack = recipe.assemble(recipeInput);
                    resultSlot.set(resultStack);
                } else {
                    // 能量不足，清空结果槽
                    resultSlot.set(ItemStack.EMPTY);
                }
            } else {
                // 没有匹配的配方，清空结果槽 + 能量归零
                this.data.set(DATA_REQUIRED_ENERGY, 0);
                resultSlot.set(ItemStack.EMPTY);
            }

            // 强制同步到客户端
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 9, resultSlot.getItem()));
            }
        }

        @Override
        public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
            ItemStack itemstack = ItemStack.EMPTY;
            Slot slot = this.slots.get(index);

            if (slot.hasItem()) {
                ItemStack slotStack = slot.getItem();
                itemstack = slotStack.copy();

                // 当从输出槽 Shift-点击时
                if (index == 9) {
                    // 1. 检查条件（仅服务端；客户端无完整配方表，预测放行、服务端兜底）
                    if (!player.level().isClientSide()) {
                        Optional<RecipeHolder<NekoAggregatorRecipe>> recipeOptional = this.findMatchingRecipe(player.level());
                        if (recipeOptional.isEmpty() || ((INeko) player).getNekoEnergy() < recipeOptional.get().value().energy) {
                            return ItemStack.EMPTY; // 如果不满足条件，阻止移动
                        }
                    }

                    // 2. 移动物品
                    if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                        return ItemStack.EMPTY;
                    }

                    // 3. onQuickCraft 会负责调用 onTake，让 onTake 去处理消耗
                    slot.onQuickCraft(slotStack, itemstack);
                }
                // 从玩家背包 -> 输入槽
                else if (index >= 10 && index < 46) {
                    if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 从输入槽 -> 玩家背包
                else if (index < 9) {
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

        private void consumeInputs() {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                // 只对非空槽位执行 shrink 操作
                if (!this.container.getItem(i).isEmpty()) {
                    this.container.getItem(i).shrink(1);
                }
            }
        }

        private Optional<RecipeHolder<NekoAggregatorRecipe>> findMatchingRecipe(Level level) {
            if (level == null || level.isClientSide()) return Optional.empty(); // 客户端无完整配方表（26.1.2）

            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < INPUT_SLOTS; i++) {
                // 同样在这里使用 .copy()
                inputs.add(this.container.getItem(i).copy());
            }
            NekoAggregatorInput input = NekoAggregatorInput.of(3, 3, inputs, 0);

            return ((net.minecraft.world.item.crafting.RecipeManager) level.recipeAccess()).getRecipeFor(ToNekoRecipes.NEKO_AGGREGATOR, input, level);
        }

        /** 客户端读取服务端同步的所需能量（容器数据槽） */
        public int getRequiredEnergy() {
            return this.data.get(DATA_REQUIRED_ENERGY);
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void removed(@NotNull Player player) {
            super.removed(player);
            this.access.execute((level, blockPos) -> {
                clearContainer(player, this.container);
            });
        }

        private class InputSlot extends Slot {
            final NekoAggregatorMenu menu;

            public InputSlot(NekoAggregatorMenu menu, Container container, int index, int x, int y) {
                super(container, index, x, y);
                this.menu = menu;
            }

            @Override
            public void setChanged() {
                // 当这个槽位的内容发生任何变化时（放入、取出、合并），这个方法会被调用
                super.setChanged();
                // 直接调用更新逻辑
                this.menu.onInputChanged();
            }
        }

    }
}