package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.items.NekoMultiToolItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NekoMultiToolEvents {
    // 防止递归：记录正在处理的实体
    private static final Set<LivingEntity> DAMAGE_GUARD = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register(NekoMultiToolEvents::onBlockBreak);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(NekoMultiToolEvents::onDamage);
    }

    // ========================
    //  伤害拦截：用猫娘等级伤害替换原版空手基础伤害
    // ========================

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        // 防止递归
        if (DAMAGE_GUARD.contains(entity)) return true;

        if (!(source.getEntity() instanceof Player player)) return true;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NekoMultiToolItem)) return true;

        // amount = (玩家基础攻击 1.0 + 附魔加成) × 冷却 × 暴击
        // 我们希望：amount - 1.0 + nekoDamage = (nekoDamage + 附魔) × 冷却 × 暴击
        float nekoBase = NekoMultiToolItem.getAttackDamage(player);
        float finalDamage = Math.max(0, amount - 1.0f + nekoBase);

        DAMAGE_GUARD.add(entity);
        boolean result = entity.hurt(source, finalDamage);
        DAMAGE_GUARD.remove(entity);

        if (result) {
            // 熔炼模式：攻击自带火焰附加（80 tick = Fire Aspect I 等级）
            if (NekoMultiToolItem.isSmeltingMode(NekoMultiToolItem.getDigMode(stack))) {
                entity.setRemainingFireTicks(80);
            }
            // 能量消耗
            if (player.isNeko()) {
                float cost = NekoMultiToolItem.getEnergyCostPerBlock(player, stack);
                if (player.getNekoEnergy() >= cost) {
                    player.setNekoEnergy(player.getNekoEnergy() - cost);
                }
            }
        }

        return false; // 取消原版伤害
    }

    private static boolean onBlockBreak(Level level, Player player, BlockPos pos,
                                         BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NekoMultiToolItem)) return true;

        int rangeMode = NekoMultiToolItem.getRangeMode(stack);
        int digMode = NekoMultiToolItem.getDigMode(stack);

        // 计算玩家面朝的方向
        Direction face = getPlayerFacing(player);

        // 计算需要破坏的方块列表
        List<BlockPos> targets = getBreakTargets(pos, face, rangeMode, player, level);

        // 预计算能量消耗和掉落物
        float totalEnergy = 0;
        List<BreakPlan> plans = new ArrayList<>();

        for (BlockPos target : targets) {
            BlockState targetState = level.getBlockState(target);
            if (targetState.isAir()) continue;
            if (targetState.getDestroySpeed(level, target) < 0) continue; // 不可破坏

            float energy = NekoMultiToolItem.getEnergyCostPerBlock(player, stack);
            totalEnergy += energy;

            List<ItemStack> customDrops;
            if (NekoMultiToolItem.isSilkTouchMode(digMode)) {
                customDrops = getSilkTouchDrops(targetState, serverLevel, target);
            } else if (NekoMultiToolItem.isSmeltingMode(digMode)) {
                customDrops = getSmeltedDrops(targetState, serverLevel, target, stack);
            } else {
                customDrops = null; // 默认模式：原版处理
            }
            plans.add(new BreakPlan(target, targetState, customDrops));
        }

        // 检查能量
        if (player.isNeko() && player.getNekoEnergy() < totalEnergy) {
            player.displayClientMessage(
                    Component.translatable("item.toneko.neko_multi_tool.no_energy"), true);
            return false; // 取消
        }

        // 执行破坏
        for (BreakPlan plan : plans) {
            BlockState targetState = level.getBlockState(plan.pos);
            // 双重检查：方块可能已被之前的破坏操作变更
            if (targetState.isAir()) continue;
            if (!targetState.is(plan.state.getBlock())) continue;

            if (plan.customDrops != null) {
                // 自定义掉落（熔炼/精准）
                level.removeBlock(plan.pos, false);
                for (ItemStack drop : plan.customDrops) {
                    Block.popResource(level, plan.pos, drop);
                }
                // 精准采集不产生经验
                if (!NekoMultiToolItem.isSilkTouchMode(digMode)) {
                    plan.state.spawnAfterBreak(serverLevel, plan.pos, stack, true);
                }
            } else {
                // 默认：原版破坏（含时运）
                level.destroyBlock(plan.pos, true, player);
            }
        }

        // 扣除能量
        if (player.isNeko() && totalEnergy > 0) {
            player.setNekoEnergy(player.getNekoEnergy() - totalEnergy);
        }

        // 播放原始方块的破坏效果
        level.levelEvent(2001, pos, Block.getId(state));

        return false; // 已手动处理，取消原版破坏
    }

    // ========================
    //  范围计算
    // ========================

    private static List<BlockPos> getBreakTargets(BlockPos center, Direction face,
                                                   int rangeMode, Player player, Level level) {
        List<BlockPos> result = new ArrayList<>();
        result.add(center);

        if (rangeMode == 1) return result;

        // 实际范围（5×5 需要 Lv15）
        int actualRange = rangeMode;
        if (rangeMode == 5 && (!player.isNeko() || player.getNekoLevel() < 15)) {
            actualRange = 3; // 静默降级
        }
        int half = actualRange / 2;

        // 根据面朝方向确定两个轴
        Direction axis1, axis2;
        if (face.getAxis() == Direction.Axis.Y) {
            // 朝上或朝下 → 水平面
            axis1 = Direction.NORTH;
            axis2 = Direction.EAST;
        } else {
            // 水平方向 → 垂直面
            axis1 = Direction.UP;
            axis2 = face.getClockWise();
        }

        for (int a = -half; a <= half; a++) {
            for (int b = -half; b <= half; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos target = center.relative(axis1, a).relative(axis2, b);
                BlockState targetState = level.getBlockState(target);
                // 只破坏同类型方块（同样的方块）
                BlockState centerState = level.getBlockState(center);
                if (!targetState.isAir() && targetState.is(centerState.getBlock())) {
                    result.add(target);
                }
            }
        }
        return result;
    }

    // ========================
    //  面朝方向检测
    // ========================

    private static Direction getPlayerFacing(Player player) {
        // 使用玩家视线方向判断
        Vec3 look = player.getLookAngle();
        return Direction.getNearest(look.x, look.y, look.z);
    }

    // ========================
    //  掉落物处理
    // ========================

    /** 精准采集掉落：直接掉落方块本身 */
    private static List<ItemStack> getSilkTouchDrops(BlockState state, ServerLevel level, BlockPos pos) {
        List<ItemStack> drops = new ArrayList<>();
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        ItemStack silkDrop = state.getBlock().getCloneItemStack(level, pos, state);
        if (!silkDrop.isEmpty()) {
            // 如果有方块实体，把 NBT 写进去
            if (blockEntity != null) {
                CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
                silkDrop.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
            }
            drops.add(silkDrop);
        }
        return drops;
    }

    /** 熔炼掉落：查询冶炼配方 */
    private static List<ItemStack> getSmeltedDrops(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool) {
        List<ItemStack> drops = new ArrayList<>();

        // 先获取正常掉落（含时运）
        List<ItemStack> normalDrops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, tool);

        for (ItemStack rawDrop : normalDrops) {
            // 查找冶炼配方
            var recipeInput = new SingleRecipeInput(rawDrop);
            var recipe = level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, recipeInput, level);

            if (recipe.isPresent()) {
                ItemStack smelted = recipe.get().value().assemble(recipeInput, level.registryAccess());
                smelted.setCount(smelted.getCount() * rawDrop.getCount());
                drops.add(smelted);
            } else {
                // 没有冶炼配方，直接掉落原物品
                drops.add(rawDrop.copy());
            }
        }
        return drops;
    }

    // ========================
    //  内部数据结构
    // ========================

    private record BreakPlan(BlockPos pos, BlockState state, List<ItemStack> customDrops) {}
}
