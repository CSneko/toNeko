package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.entity.ai.goal.Goal;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class NekoCropGatheringGoal extends Goal {
    private final NekoEntity neko;
    private final Level level;
    // 操作冷却计时器，防止操作太频繁
    private int cooldownTicks = 0;

    // 目标相关变量
    private BlockPos targetPos = null;     // 目标作物（或上方）的坐标
    private TargetType targetType = null;    // 目标类型（收割或种植）

    // 扫描范围：10格内寻找耕地；操作范围：2格内直接操作
    private final double SCAN_RADIUS = 10.0;
    private final double OPERATION_RADIUS = 2.0;
    // 搜索箱子的半径（用于归还操作）
    private final int CHEST_SEARCH_RADIUS = 8;

    private enum TargetType {
        HARVEST, PLANT
    }

    public NekoCropGatheringGoal(NekoEntity neko) {
        this.neko = neko;
        this.level = neko.level();
        // 设置该 goal 可同时运行的条件
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 当猫娘拥有至少1单位动力时才启动该行为
        return this.neko.getGatheringPower() > 0;
    }

    @Override
    public void start() {
        // 开始时重置冷却计时器与目标
        this.cooldownTicks = 0;
        this.targetPos = null;
        this.targetType = null;
    }

    @Override
    public void stop() {
        // 停止时清除目标
        this.targetPos = null;
        this.targetType = null;
    }

    @Override
    public void tick() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }

        // 当只剩下最后1单位动力时，执行归还操作
        if (this.neko.getGatheringPower() == 1) {
            depositCropsIntoNearbyChest();
            this.neko.consumeGatheringPower(1);
            this.cooldownTicks = 20;
            return;
        }

        // 如果已有目标，则先处理目标
        if (targetPos != null) {
            double distanceSq = this.neko.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
            if (distanceSq <= OPERATION_RADIUS * OPERATION_RADIUS) {
                // 到达目标范围，执行对应操作
                boolean success = false;
                if (targetType == TargetType.HARVEST) {
                    success = attemptHarvestAt(targetPos);
                } else if (targetType == TargetType.PLANT) {
                    success = attemptPlantingAt(targetPos);
                }
                // 清除目标，不论操作是否成功（下次循环可重新扫描）
                targetPos = null;
                targetType = null;
                if (success) {
                    this.neko.consumeGatheringPower(1);
                    this.cooldownTicks = 20;
                    return;
                }
                // 若操作失败，则继续后续流程
            } else {
                // 目标还未到达，指示猫娘朝目标移动
                this.neko.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 0.3);
                this.cooldownTicks = 10;
                return;
            }
        }

        // 没有目标时，扫描10格范围内的耕地，寻找可收割或可种植的位置
        BlockPos foundTarget = null;
        TargetType foundType = null;
        // 定义扫描范围（以实体为中心的包围盒）
        AABB scanBox = new AABB(
                this.neko.getX() - SCAN_RADIUS, this.neko.getY() - SCAN_RADIUS, this.neko.getZ() - SCAN_RADIUS,
                this.neko.getX() + SCAN_RADIUS, this.neko.getY() + SCAN_RADIUS, this.neko.getZ() + SCAN_RADIUS
        );
        // 遍历扫描范围内所有位置
        for (BlockPos pos : BlockPos.betweenClosed(
                (int) scanBox.minX, (int) scanBox.minY, (int) scanBox.minZ,
                (int) scanBox.maxX, (int) scanBox.maxY, (int) scanBox.maxZ
        )) {
            // 判断该位置是否为耕地
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() == Blocks.FARMLAND) {
                // 目标位置定义为耕地上方的方块
                BlockPos above = pos.above();
                BlockState aboveState = level.getBlockState(above);
                // 优先判断是否存在成熟作物（可收割）
                if (aboveState.getBlock() instanceof CropBlock crop && crop.isMaxAge(aboveState)) {
                    foundTarget = above;
                    foundType = TargetType.HARVEST;
                    break; // 找到目标即退出扫描
                }
                // 如果上方为空且猫娘有可种植的种子，则可种植
                if (level.isEmptyBlock(above) && hasSeedInInventory()) {
                    foundTarget = above;
                    foundType = TargetType.PLANT;
                    break;
                }
            }
        }

        if (foundTarget != null) {
            double distanceSq = this.neko.distanceToSqr(foundTarget.getX() + 0.5, foundTarget.getY() + 0.5, foundTarget.getZ() + 0.5);
            if (distanceSq <= OPERATION_RADIUS * OPERATION_RADIUS) {
                // 目标就在附近，直接执行操作
                boolean success;
                if (foundType == TargetType.HARVEST) {
                    success = attemptHarvestAt(foundTarget);
                } else {
                    success = attemptPlantingAt(foundTarget);
                }
                if (success) {
                    this.neko.consumeGatheringPower(1);
                    this.cooldownTicks = 20;
                    return;
                }
            } else {
                // 目标较远，则设置目标，后续 tick 中朝目标移动
                this.targetPos = foundTarget;
                this.targetType = foundType;
                // 同时指示猫娘开始移动
                this.neko.getNavigation().moveTo(foundTarget.getX() + 0.5, foundTarget.getY() + 0.5, foundTarget.getZ() + 0.5, 0.3);
                this.cooldownTicks = 10;
                return;
            }
        }

        // 如果扫描后没有找到可操作的耕地，则尝试归还物品到附近箱子
        depositCropsIntoNearbyChest();
        this.neko.consumeGatheringPower(1);
        this.cooldownTicks = 20;
    }

    /**
     * 尝试在指定坐标处执行收割操作。
     * 这里 target 应为作物所在位置（耕地上方）
     * 改为通过 loot 表获取产物
     */
    private boolean attemptHarvestAt(BlockPos target) {
        BlockState state = level.getBlockState(target);
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            // 重置作物为初始生长阶段
            level.setBlock(target, crop.getStateForAge(0), 3);

            // 通过 loot 表获取掉落产物
            if (level instanceof ServerLevel serverLevel) {
                LootParams.Builder builder = (new LootParams.Builder(serverLevel)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(target))
                        .withParameter(LootContextParams.BLOCK_STATE, state)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, neko)
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
                List<ItemStack> drops = state.getDrops(builder);
                for (ItemStack drop : drops) {
                    this.neko.addItem(drop);
                }
            }
            // 播放粒子或破坏效果
            level.levelEvent(2001, target, Block.getId(state));
            return true;
        }
        return false;
    }

    /**
     * 尝试在指定坐标处执行种植操作。
     * 这里 target 应为耕地上方的位置
     */
    private boolean attemptPlantingAt(BlockPos target) {
        // 对应的耕地位置为目标下方
        BlockPos farmlandPos = target.below();
        BlockState farmlandState = level.getBlockState(farmlandPos);
        if (farmlandState.getBlock() == Blocks.FARMLAND && level.isEmptyBlock(target)) {
            ItemNameBlockItem seedItem = getSeedFromInventory();
            if (seedItem != null && seedItem.getBlock() instanceof CropBlock cropBlock) {
                // 利用种子对应的作物状态进行种植
                BlockState cropState = cropBlock.defaultBlockState();
                level.setBlock(target, cropState, 3);
                removeSeedFromInventory(seedItem);
                return true;
            }
        }
        return false;
    }

    /**
     * 检查猫娘库存中是否含有可种植的种子
     */
    private boolean hasSeedInInventory() {
        return getSeedFromInventory() != null;
    }

    /**
     * 获取猫娘库存中第一个可种植的种子（支持所有 SeedsItem）
     */
    private ItemNameBlockItem getSeedFromInventory() {
        for (ItemStack stack : this.neko.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemNameBlockItem i && i.getBlock() instanceof CropBlock) {
                return i;
            }
        }
        return null;
    }

    /**
     * 从猫娘库存中移除一个指定种子
     */
    private void removeSeedFromInventory(ItemNameBlockItem seedItem) {
        for (int i = 0; i < this.neko.getInventory().items.size(); i++) {
            ItemStack stack = this.neko.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.getItem() == seedItem) {
                stack.shrink(1);
                if (stack.getCount() <= 0) {
                    this.neko.getInventory().items.set(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    /**
     * 将猫娘采集到的农作物物品存入附近的箱子中。
     * @return 如果找到了箱子并存入成功返回 true，否则返回 false。
     */
    private boolean depositCropsIntoNearbyChest() {
        BlockPos nekoPos = this.neko.blockPosition();
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        Optional<ChestBlockEntity> chestOpt = findNearbyChest(serverLevel, nekoPos, CHEST_SEARCH_RADIUS);
        if (chestOpt.isPresent()) {
            ChestBlockEntity chest = chestOpt.get();
            // 遍历猫娘库存中的每个物品
            for (int i = 0; i < this.neko.getInventory().items.size(); i++) {
                ItemStack depositStack = this.neko.getInventory().items.get(i);
                if (!depositStack.isEmpty()) {
                    // 先尝试与箱子中已有的同类物品进行合并
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        ItemStack chestStack = chest.getItem(slot);
                        // 判断箱子里该槽位是否有物品，并且是否与要存入的物品相同（包括NBT数据）
                        if (!chestStack.isEmpty() && ItemStack.isSameItemSameComponents(chestStack, depositStack)) {
                            int maxStack = depositStack.getMaxStackSize(); // 最大堆叠数量
                            int availableSpace = maxStack - chestStack.getCount();
                            if (availableSpace > 0) {
                                int depositCount = depositStack.getCount();
                                // 如果箱子中剩余空间可以容纳所有待存入的物品，则直接合并
                                if (depositCount <= availableSpace) {
                                    chestStack.grow(depositCount);
                                    depositStack.setCount(0);
                                } else {
                                    // 否则，先将箱子槽位堆满，再减少待存入物品数量
                                    chestStack.setCount(maxStack);
                                    depositStack.shrink(availableSpace);
                                }
                                // 更新箱子中该槽位的物品
                                chest.setItem(slot, chestStack);
                                // 如果待存入的物品已全部合并，则退出循环
                                if (depositStack.isEmpty()) {
                                    break;
                                }
                            }
                        }
                    }
                    // 如果合并后仍有剩余物品，则寻找空槽位放入
                    if (!depositStack.isEmpty()) {
                        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                            ItemStack chestStack = chest.getItem(slot);
                            if (chestStack.isEmpty()) {
                                // 直接复制一份待存入的物品到空槽位
                                chest.setItem(slot, depositStack.copy());
                                // 清空猫娘对应的库存槽位
                                depositStack.setCount(0);
                                break;
                            }
                        }
                    }
                    // 更新猫娘库存中的该物品槽位
                    this.neko.getInventory().items.set(i, depositStack);
                }
            }
            return true;
        }
        return false;
    }


    /**
     * 在指定范围内搜索最近的箱子（ChestBlockEntity）
     */
    private Optional<ChestBlockEntity> findNearbyChest(ServerLevel level, BlockPos center, int radius) {
        ChestBlockEntity closestChest = null;
        double minDistanceSq = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof ChestBlockEntity chest) {
                        double distanceSq = center.distSqr(pos);
                        if (distanceSq < minDistanceSq) {
                            minDistanceSq = distanceSq;
                            closestChest = chest;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(closestChest);
    }
}
