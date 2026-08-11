package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 幽灵猫娘专属跟随：持续跟随好感度（xp）最高的在线主人；
 * 第一主人离线时自动降级跟随好感度次高的在线主人，全部离线则停止（交给乱飞 goal 飘动等待）。
 * <p>
 * 保持位置：水平 3 格（保持幽灵相对主人的水平方向）+ 垂直始终比主人高 3 格。
 * 距离超过 12 格或跨维度时直接传送到期望位置（原版宠物模式，主人传送走也跟着传）。
 * <p>
 * 移动实现：幽灵悬空时 GroundPathNavigation 的 canUpdatePath() 恒为 false，
 * navigation.moveTo 完全无效，因此本 goal 绕过导航，每 tick 直接 setDeltaMovement 驱动——
 * goal tick（serverAiStep）先于 LivingEntity.travel（aiStep）执行，travel 的 moveRelative(0)
 * 不改变 deltaMovement、move(SELF, delta) 完成位移、摩擦衰减在位移之后，故每 tick 重设
 * deltaMovement 即得到精确位移，且世界系速度不受实体朝向（yaw）影响。
 */
public class GhostFollowOwnerGoal extends Goal {
    private final GhostNekoEntity ghost;
    /** 当前跟随的主人，每 tick 重新选择 */
    @Nullable private ServerPlayer owner;

    /** 水平保持距离（格） */
    private static final double HORIZONTAL_DIST = 3.0;
    /** 垂直高于主人（格） */
    private static final double VERTICAL_OFFSET = 3.0;
    /** 传送触发距离（原版宠物 12²） */
    private static final double TELEPORT_DIST_SQ = 144.0;
    /** 到位悬停阈值（格） */
    private static final double ARRIVE_DIST = 0.5;
    /** 最大速度（格/tick，约 5 格/秒，快于玩家疾跑） */
    private static final double MAX_SPEED = 0.25;
    /** 幽灵恰在主人正上方（水平距离小于此值）时随机选择水平方向 */
    private static final double RANDOM_DIR_EPSILON = 0.1;

    public GhostFollowOwnerGoal(GhostNekoEntity ghost) {
        this.ghost = ghost;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (ghost.level().isClientSide) return false;
        if (ghost.isSitting() || ghost.isVehicle() || ghost.isLeashed()) return false;
        return selectOwner() != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (ghost.isSitting() || ghost.isVehicle() || ghost.isLeashed()) return false;
        if (this.owner == null || !this.owner.isAlive() || this.owner.isSpectator()) return false;
        MinecraftServer server = ghost.level().getServer();
        if (server == null) return false;
        // 引用相等校验：下线/重连会产生新的 ServerPlayer 引用，此时停止，下轮循环重新选择
        return server.getPlayerList().getPlayer(this.owner.getUUID()) == this.owner;
    }

    @Override
    public void tick() {
        // 1. 每 tick 重新选择第一主人（xp 最高、在线、存活、非旁观者）
        this.owner = selectOwner();
        if (this.owner == null) return;

        // 2. 期望位置：水平保持相对方向 3 格 + 垂直高 3 格
        Vec3 desired = computeDesiredPosition();

        // 3. 跨维度：先于距离判断，直接传送（主人下界/末地也跟着传）
        if (!ghost.level().dimension().equals(this.owner.level().dimension())) {
            teleportCrossDimension(desired);
            return; // ★ 此后本 tick 不得再触碰 ghost（旧实体已 setRemoved）
        }

        // 4. 同维度但距离过远（>12 格，含主人 /tp 走）：直接传送到期望位置
        if (ghost.distanceToSqr(this.owner) >= TELEPORT_DIST_SQ) {
            teleportSameDimension(desired);
            return;
        }

        // 5. 近距离：飞行移动到期望位置 / 到位悬停
        moveTowards(desired);
    }

    @Override
    public void stop() {
        this.owner = null;
        ghost.setDeltaMovement(Vec3.ZERO); // 停掉惯性，交给乱飞 goal 原地飘动等待
    }

    /** 跟随目标需要每 tick 更新（传送响应与到位控制不能隔 2 tick 发虚） */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** 选择好感度最高的在线主人（第一主人离线时自动降级到次高）；全部不满足返回 null */
    private @Nullable ServerPlayer selectOwner() {
        MinecraftServer server = ghost.level().getServer();
        if (server == null) return null;
        return ghost.getOwners().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<UUID, INeko.Owner> e) -> e.getValue().getXp())
                        .reversed()
                        .thenComparing(e -> e.getKey().toString())) // 平局时确定性选择
                .map(e -> server.getPlayerList().getPlayer(e.getKey())) // 跨维度正确：全局在线表
                .filter(p -> p != null && p.isAlive() && !p.isSpectator())
                .findFirst()
                .orElse(null);
    }

    /** 期望位置：幽灵在主人哪边就维持在那边水平 3 格、比主人高 3 格 */
    private Vec3 computeDesiredPosition() {
        double dx = ghost.getX() - owner.getX();
        double dz = ghost.getZ() - owner.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        if (hd < RANDOM_DIR_EPSILON) {
            // 恰在主人正上方：随机一个水平方向
            double angle = ghost.getRandom().nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= hd;
            dz /= hd;
        }
        return new Vec3(owner.getX() + dx * HORIZONTAL_DIST,
                owner.getY() + VERTICAL_OFFSET,
                owner.getZ() + dz * HORIZONTAL_DIST);
    }

    /** 近距离飞行：远离期望点时按距离比例减速逼近，到位后悬停面朝主人 */
    private void moveTowards(Vec3 desired) {
        double dist = ghost.position().distanceTo(desired);
        if (dist < ARRIVE_DIST) {
            ghost.setDeltaMovement(Vec3.ZERO);
            ghost.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);
            return;
        }
        double speed = Mth.clamp(dist * 0.5, 0.06, MAX_SPEED);
        Vec3 dir = desired.subtract(ghost.position()).normalize();
        ghost.setDeltaMovement(dir.scale(speed));
        ghost.getLookControl().setLookAt(desired.x, desired.y, desired.z, 30.0F, 30.0F);
    }

    /** 同维度传送：直接移动到期望位置（= 原版宠物 maybeTeleportTo 做法） */
    private void teleportSameDimension(Vec3 desired) {
        ghost.moveTo(desired.x, desired.y, desired.z, ghost.getYRot(), ghost.getXRot());
        ghost.setDeltaMovement(Vec3.ZERO);
        ghost.getNavigation().stop(); // 清残留路径状态
    }

    /** 跨维度传送：teleportTo 内部复制实体并保留全部 NBT（AI 记忆/主人/好感度/生前类型），
     *  不触发维度事件；新实体构造时自动注册新 goal 实例接管跟随 */
    private void teleportCrossDimension(Vec3 desired) {
        if (this.owner.level() instanceof ServerLevel target) {
            ghost.teleportTo(target, desired.x, desired.y, desired.z,
                    Set.of(), ghost.getYRot(), ghost.getXRot());
        }
    }
}
