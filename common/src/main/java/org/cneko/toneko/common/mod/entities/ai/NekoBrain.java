package org.cneko.toneko.common.mod.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 猫娘 AI 大脑 —— 中央导航协调器。
 *
 * <p>设计理念：分离"决策"（WHAT）与"执行"（HOW）。
 * 所有行为（Goal、仇恨系统、萌属性特质等）通过 submitMove/stopMoving
 * 提交移动意图，由 NekoBrain 在每个 tick 统一仲裁并执行导航过渡。</p>
 *
 * <h3>仲裁规则</h3>
 * <ol>
 *   <li>更高优先级的意图无条件替换低优先级</li>
 *   <li>同优先级：需要当前行为已运行 >= minRunTicks 才允许替换（防抖）</li>
 *   <li>同一 source 的连续相同 target 被去重</li>
 *   <li>切换 target 时不调用 navigation.stop()，直接用 navigation.moveTo(newTarget) 覆盖</li>
 * </ol>
 */
public class NekoBrain {
    private final NekoEntity neko;
    private final PathNavigation navigation;

    /** 当前正在执行的移动目标 */
    @Nullable
    private MoveTarget currentTarget;

    /** 当前意图的运行 tick 计数 */
    private int runTicks;

    /** 同优先级最小运行 tick 数（防抖） */
    private static final int MIN_RUN_TICKS = 10;

    /** 上一个运行意图的 source 和 priority，用于冷却追踪 */
    @Nullable
    private Object lastSource;
    @Nullable
    private BehaviorPriority lastPriority;
    private int cooldownTicks;

    /** 本 tick 收集到的最高优先级意图 */
    @Nullable
    private MoveTarget pendingTarget;

    /** 是否开启完整仲裁模式（Phase 5 之前为 false，透传模式） */
    private boolean fullArbitration = false;

    // ====== 卡住检测 ======
    private Vec3 lastCheckPos = Vec3.ZERO;
    private int stuckTicks;
    private static final int STUCK_CHECK_INTERVAL = 60;    // 3秒检测一次
    private static final double STUCK_DISTANCE = 0.08;     // 位移阈值
    private static final int STUCK_CANCEL_TICKS = 180;      // 9秒真卡住才取消
    private int stuckCheckTimer;

    // ====== baka 干扰 ======
    private int distractionTicks;
    private Vec3 distractionLookTarget = Vec3.ZERO;
    private boolean hasDistraction;

    public NekoBrain(NekoEntity neko) {
        this.neko = neko;
        this.navigation = neko.getNavigation();
    }

    // ============================================================
    // Behavior API —— 行为通过此接口提交意图
    // ============================================================

    /**
     * 请求移动到指定坐标（便捷重载）。
     */
    public void submitMove(double x, double y, double z, double speed, BehaviorPriority priority, Object source) {
        submitMove(new Vec3(x, y, z), speed, priority, source);
    }

    /**
     * 请求移动到指定坐标。
     * @param pos    目标位置
     * @param speed  移动速度倍率
     * @param priority 行为优先级
     * @param source 行为来源（通常是 Goal 实例，用于去重和冷却）
     */
    public void submitMove(Vec3 pos, double speed, BehaviorPriority priority, Object source) {
        MoveTarget intent = new MoveTarget(pos, null, speed, priority, source);
        processIntent(intent);
    }

    /**
     * 请求移动到指定实体。
     * @param target 目标实体
     * @param speed  移动速度倍率
     * @param priority 行为优先级
     * @param source 行为来源
     */
    public void submitMove(LivingEntity target, double speed, BehaviorPriority priority, Object source) {
        MoveTarget intent = new MoveTarget(null, target, speed, priority, source);
        processIntent(intent);
    }

    /**
     * 请求停止移动。仅在 source 拥有当前导航控制权时才真正 stop。
     * @param source 行为来源（必须与当前移动的 source 匹配）
     */
    public void stopMoving(Object source) {
        // 只有当前正在运行的 source 才能停止移动
        // 或者 source 是更高优先级的行为（它要覆盖当前移动）
        if (currentTarget != null) {
            if (currentTarget.source == source
                    || BehaviorPriority.isHigherThan(currentTarget.priority, BehaviorPriority.CRITICAL) == false) {
                // source 匹配，或者 source 的优先级足够覆盖
                if (fullArbitration) {
                    // 完整仲裁：将 pending 清空（让 tick 时自然停止）
                    pendingTarget = null;
                } else {
                    // 透传模式：直接停止
                    navigation.stop();
                }
                currentTarget = null;
                runTicks = 0;
            }
        }
    }

    /**
     * 提交一个短暂的"走神"干扰（baka 属性专用）。
     * 只在没有任何更高优先级移动时才生效。
     */
    public void submitDistraction(int ticks, Vec3 lookTarget) {
        this.distractionTicks = ticks;
        this.distractionLookTarget = lookTarget;
        this.hasDistraction = true;
    }

    // ============================================================
    // 意图处理逻辑
    // ============================================================

    /**
     * 处理一个移动意图，与当前 pending 意图比较并保留更优者。
     */
    private void processIntent(MoveTarget intent) {
        if (pendingTarget == null) {
            pendingTarget = intent;
            return;
        }

        // 同 source 同 target → 更新 speed，不重复处理
        if (intent.source == pendingTarget.source && intent.isSameDestination(pendingTarget)) {
            pendingTarget = intent; // 更新 speed
            return;
        }

        // 比较优先级，保留更优者
        if (BehaviorPriority.isHigherThan(intent.priority, pendingTarget.priority)) {
            pendingTarget = intent;
        }
        // 同优先级：保留先来的（不替换）
    }

    // ============================================================
    // 每 tick 执行
    // ============================================================

    /**
     * 每个服务端 tick 调用一次。收集意图 → 仲裁 → 执行导航。
     */
    public void tick() {
        if (!fullArbitration) {
            // 透传模式：直接应用 pending 意图
            tickPassthrough();
            return;
        }

        // 完整仲裁模式
        tickArbitrated();
    }

    /**
     * 透传模式：直接执行意图，不做仲裁。
     */
    private void tickPassthrough() {
        if (pendingTarget != null) {
            MoveTarget intent = pendingTarget;
            pendingTarget = null;

            // 不需要切换的情况
            if (currentTarget != null && intent.isSameDestination(currentTarget)) {
                currentTarget = intent; // 更新 speed
                runTicks++;
                return;
            }

            // 执行新的移动
            executeMove(intent);
            currentTarget = intent;
            runTicks = 0;
        }

        // 处理干扰
        tickDistraction();

        // 即使透传模式也追踪运行 tick
        if (currentTarget != null) {
            runTicks++;
        }
    }

    /**
     * 完整仲裁模式：防抖 + 优先级比较 + 冷却检查。
     */
    private void tickArbitrated() {
        if (pendingTarget != null) {
            MoveTarget intent = pendingTarget;
            pendingTarget = null;

            boolean shouldSwitch = false;

            if (currentTarget == null) {
                // 当前无移动，直接接受
                shouldSwitch = true;
            } else if (BehaviorPriority.isHigherThan(intent.priority, currentTarget.priority)) {
                // 更高优先级：无条件替换
                shouldSwitch = true;
            } else if (intent.priority == currentTarget.priority && intent.source == currentTarget.source) {
                // 同一个 source 更新 target → 接受
                shouldSwitch = true;
            } else if (intent.priority == currentTarget.priority && runTicks >= MIN_RUN_TICKS) {
                // 同优先级不同 source：需要当前行为已运行足够时间
                shouldSwitch = true;
            } else if (intent.priority == currentTarget.priority && intent.source != currentTarget.source) {
                // 同优先级但运行时不足 → 检查 source 是否相同（防抖）
                // 如果 intent.source 和上一轮的 source 相同 → 可能是被错误打断后重试 → 拒绝
                if (intent.source == lastSource && cooldownTicks > 0) {
                    // 拒绝频繁切换
                    shouldSwitch = false;
                }
            }

            if (shouldSwitch) {
                // 不调用 navigation.stop()，直接覆盖路径
                executeMove(intent);
                lastSource = currentTarget != null ? currentTarget.source : null;
                lastPriority = currentTarget != null ? currentTarget.priority : null;
                currentTarget = intent;
                runTicks = 0;
                cooldownTicks = 5;
            }
        }

        // 更新计数
        if (currentTarget != null) {
            runTicks++;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        // 处理干扰 + 卡住检测
        tickDistraction();
        tickStuckDetection();
    }

    /**
     * 实际执行移动：调用 navigation.moveTo()
     */
    private void executeMove(MoveTarget intent) {
        if (intent.entity != null) {
            navigation.moveTo(intent.entity, intent.speed);
        } else if (intent.pos != null) {
            navigation.moveTo(intent.pos.x, intent.pos.y, intent.pos.z, intent.speed);
        }
    }

    // ============================================================
    // 干扰处理（baka 散步）
    // ============================================================

    private void tickDistraction() {
        if (!hasDistraction) return;

        if (distractionTicks > 0) {
            distractionTicks--;
            // 仅在 IDLE 或没有更高优先级移动时执行干扰
            if (currentTarget == null || currentTarget.priority == BehaviorPriority.IDLE) {
                navigation.stop();
                if (distractionLookTarget != Vec3.ZERO) {
                    neko.getLookControl().setLookAt(
                            distractionLookTarget.x,
                            distractionLookTarget.y,
                            distractionLookTarget.z
                    );
                }
                currentTarget = null;
                runTicks = 0;
            }
        } else {
            hasDistraction = false;
            distractionLookTarget = Vec3.ZERO;
        }
    }

    // ============================================================
    // 卡住检测（替代原版 stuck detection）
    // ============================================================

    private void tickStuckDetection() {
        if (currentTarget == null) {
            stuckTicks = 0;
            stuckCheckTimer = 0;
            return;
        }

        stuckCheckTimer++;
        if (stuckCheckTimer < STUCK_CHECK_INTERVAL) return;
        stuckCheckTimer = 0;

        Vec3 currentPos = neko.position();
        if (lastCheckPos.distanceToSqr(currentPos) < STUCK_DISTANCE * STUCK_DISTANCE) {
            stuckTicks += STUCK_CHECK_INTERVAL;

            // 在空中：等待落地，不计为卡住
            if (!neko.onGround() && !neko.isInWater()) {
                stuckTicks = 0;
                return;
            }

            // 在水中：每 60 tick 重新寻路（原版逻辑保留）
            if (neko.isInWater() && stuckTicks >= 60) {
                stuckTicks = 0;
                if (currentTarget != null) {
                    executeMove(currentTarget);
                }
                return;
            }

            // 真正卡住（地面 9 秒不动）
            if (stuckTicks >= STUCK_CANCEL_TICKS) {
                navigation.stop();
                currentTarget = null;
                runTicks = 0;
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
            lastCheckPos = currentPos;
        }
    }

    // ============================================================
    // 查询 API
    // ============================================================

    public boolean isMoving() {
        return currentTarget != null && !navigation.isDone();
    }

    public boolean isMovingTowards(LivingEntity entity) {
        return currentTarget != null && currentTarget.entity == entity;
    }

    @Nullable
    public MoveTarget getCurrentTarget() {
        return currentTarget;
    }

    @Nullable
    public BehaviorPriority getCurrentPriority() {
        return currentTarget != null ? currentTarget.priority : null;
    }

    /**
     * 开启完整仲裁模式（Phase 5 调用）。
     */
    public void enableFullArbitration() {
        this.fullArbitration = true;
    }

    public boolean isFullArbitration() {
        return fullArbitration;
    }

    /**
     * 强制清空所有状态（实体被移除/重置时调用）。
     */
    public void reset() {
        currentTarget = null;
        pendingTarget = null;
        runTicks = 0;
        cooldownTicks = 0;
        stuckTicks = 0;
        stuckCheckTimer = 0;
        lastSource = null;
        lastPriority = null;
        hasDistraction = false;
        distractionTicks = 0;
        distractionLookTarget = Vec3.ZERO;
    }

    // ============================================================
    // MoveTarget —— 移动意图数据类
    // ============================================================

    public static class MoveTarget {
        @Nullable
        public final Vec3 pos;
        @Nullable
        public final LivingEntity entity;
        public final double speed;
        public final BehaviorPriority priority;
        public final Object source;

        MoveTarget(@Nullable Vec3 pos, @Nullable LivingEntity entity, double speed,
                   BehaviorPriority priority, Object source) {
            this.pos = pos;
            this.entity = entity;
            this.speed = speed;
            this.priority = priority;
            this.source = source;
        }

        /** 判断是否与另一个意图目标相同 */
        boolean isSameDestination(MoveTarget other) {
            if (other == null) return false;
            if (this.entity != null && other.entity != null) {
                return this.entity.equals(other.entity);
            }
            if (this.pos != null && other.pos != null) {
                return this.pos.distanceToSqr(other.pos) < 0.01;
            }
            return false;
        }

        @Override
        public String toString() {
            return "MoveTarget{" +
                    (entity != null ? "entity=" + entity : "pos=" + pos) +
                    ", speed=" + speed +
                    ", priority=" + priority +
                    '}';
        }
    }
}
