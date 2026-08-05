package org.cneko.toneko.common.mod.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.ModMeta;
import org.cneko.toneko.common.mod.entities.SeatEntity;
import org.cneko.toneko.common.mod.items.ShengDengItem;
import org.cneko.toneko.common.mod.util.ITickable;

import java.util.*;

/**
 * 广东省省凳。
 *
 * 核心设计：一个格子可以塞 6 张凳（经典一叠 6 张）。
 * STACK = 格内数量 - 1（0~5，即 1~6 张），塞满 6 张才开新格。
 * 格高随数量增加：1 张 16px，6 张 31px（≈2 格），完美体现"省凳省空间"。
 * 交互：右键坐下，潜行右键拔凳（空手/手持其他物品均如此）。
 */
public class ShengDengBlock extends Block {
    /** 格内数量 - 1：0~5 表示 1~6 张 */
    public static final IntegerProperty STACK = IntegerProperty.create("stack", 0, 5);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class, DyeColor.RED, DyeColor.BLUE);

    /** 每格可塞的最大张数 */
    public static final int CELL_MAX = 6;

    /**
     * 格高（像素）：索引 = STACK，值 = 该格总高度。
     * 单张凳 = 1 格高（16px），每叠一张 +3px（腿插进下方凳面缝隙），
     * 6 张 ≈ 2 格 —— 完美体现"省凳省空间"。
     */
    private static final int[] CELL_HEIGHT_PX = {16, 19, 22, 25, 28, 31};

    public static final MapCodec<ShengDengBlock> CODEC = simpleCodec(ShengDengBlock::new);

    // ==================== 拔凳系统 ====================

    private static final Map<BlockPos, PullingState> PULLING_STATES = new HashMap<>();
    private static final Set<BlockPos> INTERNAL_REMOVAL = new HashSet<>();
    private static boolean tickTaskRegistered = false;

    private static class PullingState {
        final UUID playerUUID;
        float tension;
        int lastWobbleTick;
        int wobbleCount;
        int lastActionTick;   // 上次有效发力（点击/摇晃）的 tick，用于节奏冷却

        double lastWobbleX;
        double lastWobbleZ;
        boolean movingPositiveX;
        boolean shakeMode;

        PullingState(UUID playerUUID, int tick, boolean shakeMode) {
            this.playerUUID = playerUUID;
            this.tension = 0;
            this.lastWobbleTick = tick;
            this.wobbleCount = 0;
            this.lastActionTick = tick;
            this.shakeMode = shakeMode;
        }
    }

    /**
     * 节奏判定：手速越快越好，但乱点会"打滑"。
     * 与上次发力的间隔：
     *   < 5 ticks  → 0.6x（太急，没使上劲）
     *   5~15 ticks → 1.5x（沉稳发力，节奏完美）
     *   > 15 ticks → 1.0x（正常）
     */
    private static float rhythmMultiplier(Player player, PullingState state) {
        int dt = player.tickCount - state.lastActionTick;
        if (dt < 5) return 0.6f;
        if (dt <= 15) return 1.5f;
        return 1.0f;
    }

    /** 摇晃检测：方向反转的最小移动距离 */
    private static final double WOBBLE_DISTANCE = 0.12;

    /** 超时 tick 数（1 秒） */
    private static final int RELEASE_TIMEOUT = 20;

    /** 距离太远自动取消 */
    private static final double CANCEL_DISTANCE = 4.0;

    /** 纯点击权重上限 */
    private static final int CLICK_ONLY_WEIGHT = 2;

    // ==================== 柱子工具方法 ====================

    /** 该格内有多少张凳 */
    public static int countInCell(BlockState state) {
        return state.getValue(STACK) + 1;
    }

    /** 该格高度（像素） */
    public static int cellHeightPx(BlockState state) {
        return CELL_HEIGHT_PX[state.getValue(STACK)];
    }

    /** 统计上方所有格子里的张数总和 */
    public static int countStoolsAbove(Level level, BlockPos pos) {
        int count = 0;
        BlockPos cursor = pos.above();
        while (level.getBlockState(cursor).getBlock() instanceof ShengDengBlock) {
            count += countInCell(level.getBlockState(cursor));
            cursor = cursor.above();
        }
        return count;
    }

    /** 从 base 所在柱子往上找顶格（base 必须是省凳格） */
    public static BlockPos findColumnTop(Level level, BlockPos base) {
        BlockPos top = base;
        while (level.getBlockState(top.above()).getBlock() instanceof ShengDengBlock) {
            top = top.above();
        }
        return top;
    }

    /**
     * 把 n 张凳塞进 base 所在的柱子（base 可以是省凳格或空气）。
     * 先塞满顶格，再往上开新格。空间不够返回 false（不部分放置）。
     * @param placeState 任意省凳 BlockState，用于开新格（继承其属性模板）
     */
    public static boolean insertStools(Level level, BlockPos base, int n, DyeColor color, BlockState placeState) {
        if (level.isClientSide) return false;
        if (n <= 0) return false;

        // 找柱顶
        BlockPos top = base;
        BlockState topState = level.getBlockState(top);
        boolean isExisting = topState.getBlock() instanceof ShengDengBlock;
        if (isExisting) {
            top = findColumnTop(level, base);
            topState = level.getBlockState(top);
        }

        // 计算顶格还能塞几张
        int remaining = n;
        int putInTop = 0;
        if (isExisting) {
            putInTop = Math.min(CELL_MAX - countInCell(topState), remaining);
            remaining -= putInTop;
        }

        // 第一个要放置的格子：已有柱 = 顶格上方；新柱 = base 格本身
        BlockPos firstCell = isExisting ? top.above() : top;

        // 空间检查
        int needCells = (remaining + CELL_MAX - 1) / CELL_MAX;
        BlockPos cursor = firstCell;
        for (int i = 0; i < needCells; i++) {
            if (!level.getBlockState(cursor).canBeReplaced()) return false;
            cursor = cursor.above();
        }

        // 执行放置
        if (putInTop > 0) {
            level.setBlock(top, topState.setValue(STACK, topState.getValue(STACK) + putInTop), Block.UPDATE_ALL);
            n -= putInTop;
        }
        cursor = firstCell;
        while (n > 0) {
            int chunk = Math.min(CELL_MAX, n);
            level.setBlock(cursor, placeState.setValue(STACK, chunk - 1).setValue(COLOR, color), Block.UPDATE_ALL);
            n -= chunk;
            cursor = cursor.above();
        }
        return true;
    }

    /** 该格所在柱子的总高度（格）：从柱底到该格顶面，用于判定"凳子有多高" */
    public static double getColumnHeight(Level level, BlockPos pos) {
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).getBlock() instanceof ShengDengBlock) {
            bottom = bottom.below();
        }
        return getCellTopY(level, pos) - bottom.getY();
    }

    /** 计算该格顶面的世界高度（用于坐下） */
    public static double getCellTopY(Level level, BlockPos pos) {
        // 每格都放置在整数 Y 上，模型/碰撞箱从格子自己的 BlockPos 起算，
        // 顶面 = 该格 Y + 该格高度，与下方格子无关。不能把各格高度连续累加：
        // 格高 > 16px 时（stack≥1）会把下方格子"超高"的部分重复计入，导致顶面虚高。
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof ShengDengBlock)) return pos.getY() + 0.5;
        return pos.getY() + cellHeightPx(s) / 16.0;
    }

    // ==================== 难度系统（基于上方重量 = 张数） ====================

    /**
     * 难度曲线：两头难，中间稍缓，越往下指数暴增。
     * w=0（顶部）   — 特别紧（被下面吸住）
     * w=1~2         — 最轻松的区间
     * w=3+          — 指数递增（重量压的）
     */
    private static float wobbleGainForWeight(int weightAbove) {
        if (weightAbove == 0) return 10f;
        if (weightAbove <= 2) return 20f;
        return Math.max(2f, 22f * (float) Math.pow(0.85, weightAbove));
    }

    private static float minTensionForWeight(int weightAbove) {
        if (weightAbove == 0) return 50f;
        if (weightAbove <= 2) return 30f;
        return Math.min(75f, 30f + weightAbove * 3.5f);
    }

    // ==================== 构造与方块状态 ====================

    public ShengDengBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(STACK, 0)
                .setValue(COLOR, DyeColor.RED));
        ensureTickTask();
    }

    private static void ensureTickTask() {
        if (!tickTaskRegistered) {
            tickTaskRegistered = true;
            TickTasks.add(new ITickable() {
                @Override
                public void addTick(int tick) {
                    tickPullingStates();
                }

                @Override
                public void addRemoveTask() {}

                @Override
                public boolean isRemoved() { return false; }
            });
        }
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STACK, COLOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return Block.box(0.0, 0.0, 0.0, 16.0, cellHeightPx(state), 16.0);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                           BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return Block.box(0.0, 0.0, 0.0, 16.0, cellHeightPx(state), 16.0);
    }

    // ==================== 拔凳 tick ====================

    private static void tickPullingStates() {
        if (PULLING_STATES.isEmpty()) return;

        List<BlockPos> toRemove = new ArrayList<>();
        List<Map.Entry<BlockPos, PullingState>> toRelease = new ArrayList<>();

        for (var entry : PULLING_STATES.entrySet()) {
            BlockPos pos = entry.getKey();
            PullingState state = entry.getValue();

            Player player = findPlayerDirect(state.playerUUID);
            if (player == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > CANCEL_DISTANCE * CANCEL_DISTANCE) {
                toRemove.add(pos);
                continue;
            }

            // === 摇晃模式：检测 A/D 左右摇晃 ===
            if (state.shakeMode) {
                double dx = player.getX() - state.lastWobbleX;
                double dz = player.getZ() - state.lastWobbleZ;
                double horizDist = Math.abs(dx) + Math.abs(dz);

                if (horizDist > WOBBLE_DISTANCE) {
                    boolean nowPositive = (Math.abs(dx) >= Math.abs(dz)) ? (dx > 0) : (dz > 0);
                    if (nowPositive != state.movingPositiveX || state.wobbleCount == 0) {
                        float rhythmBonus = rhythmMultiplier(player, state);
                        state.movingPositiveX = nowPositive;
                        state.lastWobbleX = player.getX();
                        state.lastWobbleZ = player.getZ();
                        state.lastWobbleTick = player.tickCount;
                        state.lastActionTick = player.tickCount;
                        state.wobbleCount++;

                        int weightAbove = countStoolsAbove(player.level(), pos);
                        float gain = wobbleGainForWeight(weightAbove) * rhythmBonus;
                        gain += toolBonus(player);
                        if (isNearWater(player.level(), pos)) gain += 10f;
                        state.tension = Math.min(100f, state.tension + gain);

                        float pitch = 0.8f + state.tension * 0.008f;
                        player.level().playSound(null, pos, SoundEvents.BAMBOO_WOOD_HIT,
                                SoundSource.BLOCKS, 0.5f, pitch);
                        applyPullSlowdown(player);
                        showPullProgress(player, state, rhythmBonus);
                    } else {
                        state.lastWobbleX = player.getX();
                        state.lastWobbleZ = player.getZ();
                    }
                }
            }

            // === 超时释放：收集，不在迭代中执行（避免 ConcurrentModificationException） ===
            if (player.tickCount - state.lastWobbleTick >= RELEASE_TIMEOUT) {
                toRelease.add(entry);
            }
        }

        // 迭代结束后再释放（releaseStool 内部会修改 PULLING_STATES，此时安全）
        for (var entry : toRelease) {
            Player player = findPlayerDirect(entry.getValue().playerUUID);
            if (player != null) {
                releaseStool(entry.getKey(), entry.getValue(), player);
            }
        }

        for (BlockPos pos : toRemove) {
            PULLING_STATES.remove(pos);
        }
        for (var entry : toRelease) {
            PULLING_STATES.remove(entry.getKey());
        }
    }

    /** 拔凳期间给玩家减速：被凳子"钉"住的感觉 */
    private static void applyPullSlowdown(Player player) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                60, 0, false, false, true));
    }

    private static float toolBonus(Player player) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof AxeItem) return 20f;
        if (held.getItem() == Items.STICK || held.getItem() == Items.BAMBOO) return 10f;
        return 0f;
    }

    private static Player findPlayerDirect(UUID uuid) {
        net.minecraft.server.MinecraftServer server = ModMeta.INSTANCE.getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }

    // ==================== 拔凳入口 ====================

    private void startPull(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return;

        int weightAbove = countStoolsAbove(level, pos);

        // 最底格（下方没有省凳格）且上方有格子压着 → 不能拔
        boolean bottomCell = !(level.getBlockState(pos.below()).getBlock() instanceof ShengDengBlock);
        if (bottomCell && weightAbove > 0) {
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.bottom_locked"), true);
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BREAK,
                    SoundSource.BLOCKS, 0.3f, 0.5f);
            return;
        }

        boolean needShake = weightAbove > CLICK_ONLY_WEIGHT;
        PullingState ps = PULLING_STATES.computeIfAbsent(pos,
                k -> new PullingState(player.getUUID(), player.tickCount, needShake));

        if (!ps.playerUUID.equals(player.getUUID())) {
            ps = new PullingState(player.getUUID(), player.tickCount, needShake);
            PULLING_STATES.put(pos, ps);
        }

        if (!needShake) {
            // ===== 轻量：纯点击（手速 + 节奏） =====
            float rhythmBonus = rhythmMultiplier(player, ps);
            ps.lastActionTick = player.tickCount;

            float gain = wobbleGainForWeight(weightAbove) * rhythmBonus;
            gain += toolBonus(player);
            if (isNearWater(level, pos)) gain += 10f;

            ps.tension = Math.min(100f, ps.tension + gain);
            ps.lastWobbleTick = player.tickCount;
            ps.wobbleCount++;

            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_HIT,
                    SoundSource.BLOCKS, 0.5f, 0.9f + ps.tension * 0.005f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                        2, 0.15, 0.05, 0.15, 0.0);
            }
            // 被凳子"钉"住：拔凳期间减速
            applyPullSlowdown(player);
            showPullProgress(player, ps, rhythmBonus);
        } else {
            // ===== 重载：摇晃模式 =====
            if (!ps.shakeMode) ps.shakeMode = true;
            if (ps.wobbleCount == 0) {
                ps.lastWobbleX = player.getX();
                ps.lastWobbleZ = player.getZ();
                ps.lastWobbleTick = player.tickCount;
            }
            // 进度条自带 A/D 提示
        }
    }

    // ==================== 释放处理 ====================

    private static void releaseStool(BlockPos pos, PullingState state, Player player) {
        Level level = player.level();
        BlockState blockState = level.getBlockState(pos);

        if (!(blockState.getBlock() instanceof ShengDengBlock)) return;

        int weightAbove = countStoolsAbove(level, pos);
        float minTension = minTensionForWeight(weightAbove);

        if (state.tension < minTension) {
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BREAK,
                    SoundSource.BLOCKS, 0.3f, 0.5f);
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.pull_weak"), true);
            return;
        }

        // ---- 拔出来了！张力决定拔出张数，从柱顶往下抽 ----
        int columnTotal = countInCell(blockState) + weightAbove;
        int pullCount = pullCountForTension(state.tension, columnTotal);
        removeFromColumnTop(level, pos, pullCount);
        dropStackedStool(level, pos, pullCount);

        float tension = state.tension;
        if (tension < 70f) {
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_PLACE,
                    SoundSource.BLOCKS, 0.5f, 1.8f);
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.pull_gentle"), true);
        } else if (tension < 80f) {
            knockbackPlayer(player, pos, 0.6);
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BREAK,
                    SoundSource.BLOCKS, 0.7f, 1.5f);
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.pull_normal"), true);
        } else if (tension < 90f) {
            knockbackPlayer(player, pos, 1.2);
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BREAK,
                    SoundSource.BLOCKS, 1.0f, 1.2f);
            spawnPopParticles(level, pos);
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.pull_strong"), true);
        } else {
            knockbackPlayer(player, pos, 2.0);
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BREAK,
                    SoundSource.BLOCKS, 1.2f, 0.9f);
            spawnPopParticles(level, pos);
            spawnPopParticles(level, pos);
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.pull_chaos"), true);
        }

        PULLING_STATES.remove(pos);
    }

    /** 张力对应的拔出张数：<70 拔 1，70~79 拔 2，80~89 拔 3，90+ 拔 4（不超过柱内总数） */
    private static int pullCountForTension(float tension, int columnTotal) {
        int count;
        if (tension < 70f) count = 1;
        else if (tension < 80f) count = 2;
        else if (tension < 90f) count = 3;
        else count = 4;
        return Math.min(count, columnTotal);
    }

    /**
     * 从柱子顶部拔掉 count 张凳：从顶格往下逐格扣减，整格扣空则移除该格。
     * 始终从顶部拔，不会造成上方格子悬空。
     */
    private static void removeFromColumnTop(Level level, BlockPos pos, int count) {
        BlockPos top = findColumnTop(level, pos);
        int remaining = count;
        while (remaining > 0) {
            BlockState s = level.getBlockState(top);
            if (!(s.getBlock() instanceof ShengDengBlock)) break;
            int inCell = countInCell(s);
            if (remaining >= inCell) {
                // 整格扣空，移除格子
                INTERNAL_REMOVAL.add(top);
                PULLING_STATES.remove(top);
                level.removeBlock(top, false);
                INTERNAL_REMOVAL.remove(top);
                remaining -= inCell;
                top = top.below();
            } else {
                // 格内部分扣减
                level.setBlock(top, s.setValue(STACK, s.getValue(STACK) - remaining), Block.UPDATE_ALL);
                remaining = 0;
            }
        }
    }

    private static void knockbackPlayer(Player player, BlockPos pos, double strength) {
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.01) {
            dx = player.getRandom().nextDouble() - 0.5;
            dz = player.getRandom().nextDouble() - 0.5;
            dist = Math.sqrt(dx * dx + dz * dz);
        }
        player.setDeltaMovement(player.getDeltaMovement().add(
                dx / dist * strength,
                0.25,
                dz / dist * strength
        ));
        player.hurtMarked = true;
    }

    private static void spawnPopParticles(Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    10, 0.4, 0.3, 0.4, 0.1);
        }
    }

    private static boolean isNearWater(Level level, BlockPos pos) {
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            if (level.getFluidState(neighbor).is(Fluids.WATER)) return true;
        }
        return false;
    }

    private static void showPullProgress(Player player, PullingState state, float rhythmBonus) {
        int filled = (int) (state.tension / 10);
        StringBuilder bar = new StringBuilder();
        if (state.shakeMode) {
            bar.append("§b§l◄A D► §r");
        }
        bar.append("§e[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "§c█" : "§7░");
        }
        bar.append("§e] §f").append((int) state.tension).append("%");
        // 节奏反馈
        if (rhythmBonus < 0.9f) {
            bar.append(" §c急了！");
        } else if (rhythmBonus > 1.2f) {
            bar.append(" §a稳！");
        }
        player.displayClientMessage(Component.literal(bar.toString()), true);
    }

    // ==================== 交互逻辑 ====================

    // ---- 空手右键：右键坐，潜行右键拔 ----
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!player.isPassenger()) {
            if (player.isShiftKeyDown()) {
                startPull(level, pos, state, player);
            } else {
                SeatEntity.sitOnBlock(level, pos, player);
                player.displayClientMessage(Component.translatable("block.toneko.sheng_deng.sit"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ---- 手持物品右键 ----
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        Item item = stack.getItem();

        // 染料
        if (item instanceof DyeItem dyeItem) {
            DyeColor dyeColor = dyeItem.getDyeColor();
            if ((dyeColor == DyeColor.RED || dyeColor == DyeColor.BLUE)
                    && state.getValue(COLOR) != dyeColor) {
                level.setBlock(pos, state.setValue(COLOR, dyeColor), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.5f, 1.0f);
                if (!player.isCreative()) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
        }

        // 木棍/竹子 → 打鼓
        if (item == Items.STICK || item == Items.BAMBOO) {
            playDrum(level, pos);
            return ItemInteractionResult.SUCCESS;
        }

        // 省凳 → 整叠塞进柱子
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShengDengBlock) {
            int toPlace = ShengDengItem.getStackCount(stack);
            DyeColor color = state.getValue(COLOR);
            if (insertStools(level, pos, toPlace, color, state)) {
                level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_PLACE,
                        SoundSource.BLOCKS, 0.7f, 1.5f);
                if (!player.isCreative()) stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(
                        Component.translatable("item.toneko.sheng_deng.no_space"), true);
                return ItemInteractionResult.FAIL;
            }
        }

        // 手持任意其他物品：右键坐，潜行右键拔
        if (!player.isPassenger()) {
            if (player.isShiftKeyDown()) {
                startPull(level, pos, state, player);
            } else {
                SeatEntity.sitOnBlock(level, pos, player);
                player.displayClientMessage(Component.translatable("block.toneko.sheng_deng.sit"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ---- 破坏：整根柱子掉落为一个整体 ----
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !movedByPiston && !state.is(newState.getBlock())) {
            PULLING_STATES.remove(pos);

            if (!INTERNAL_REMOVAL.contains(pos)) {
                int total = countAndRemoveWholeColumn(level, pos, countInCell(state));
                dropStackedStool(level, pos, total);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** @param targetCount 目标格（pos）的张数 —— pos 可能已被替换成空气，需要调用者传入 */
    private static int countAndRemoveWholeColumn(Level level, BlockPos pos, int targetCount) {
        return countAndRemoveColumn(level, pos, true, true, targetCount);
    }

    /** 统计并移除连续的凳子柱，返回总张数 */
    private static int countAndRemoveColumn(Level level, BlockPos pos, boolean goUp, boolean goDown, int targetCount) {
        Set<BlockPos> column = new LinkedHashSet<>();
        column.add(pos);

        if (goUp) {
            BlockPos cursor = pos.above();
            while (level.getBlockState(cursor).getBlock() instanceof ShengDengBlock) {
                column.add(cursor);
                cursor = cursor.above();
            }
        }
        if (goDown) {
            BlockPos cursor = pos.below();
            while (level.getBlockState(cursor).getBlock() instanceof ShengDengBlock) {
                column.add(cursor);
                cursor = cursor.below();
            }
        }

        // 总张数 = 目标格（调用者传入）+ 其它格子实读
        int total = targetCount;
        for (BlockPos p : column) {
            if (p.equals(pos)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof ShengDengBlock) {
                total += countInCell(s);
            }
        }

        // 标记为内部移除，阻止 onRemove 重复掉落
        INTERNAL_REMOVAL.addAll(column);
        for (BlockPos p : column) {
            PULLING_STATES.remove(p);
            level.removeBlock(p, false);
        }
        INTERNAL_REMOVAL.removeAll(column);
        return total;
    }

    private static void dropStackedStool(Level level, BlockPos pos, int count) {
        ItemStack drop = ShengDengItem.createStackedItem(count);
        net.minecraft.world.entity.item.ItemEntity itemEntity =
                new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        level.addFreshEntity(itemEntity);
    }

    // ---- 打鼓 ----
    private void playDrum(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_HIT,
                SoundSource.BLOCKS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    3, 0.3, 0.1, 0.3, 0.0);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    2, 0.1, 0.05, 0.1, 0.1);
        }
    }
}
