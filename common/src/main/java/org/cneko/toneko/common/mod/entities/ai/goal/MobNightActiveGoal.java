package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 夜行性基因的AI Goal
 * 晚上更活跃，会寻找黑暗的地方活动
 * 白天会寻找阴影处休息
 */
public class MobNightActiveGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private int searchCooldown;

    public MobNightActiveGoal(PathfinderMob mob) {
        this.mob = mob;
        this.level = mob.level();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // 每20-40 tick检查一次，减少性能开销
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }
        this.searchCooldown = this.mob.getRandom().nextInt(20) + 20;
        
        // 根据时间决定行为
        long dayTime = this.level.getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime <= 23000; // 夜晚时间
        
        if (isNight) {
            // 夜晚：寻找黑暗的地方活动
            return findDarkSpot();
        } else {
            // 白天：寻找阴影处休息
            return findShadedSpot();
        }
    }

    @Override
    public boolean canContinueToUse() {
        // 持续移动直到到达目的地或超时
        return !this.mob.getNavigation().isDone() && this.searchCooldown > 0;
    }

    @Override
    public void start() {
        // 开始向目标点移动
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, 1.0);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.searchCooldown = this.mob.getRandom().nextInt(40) + 40;
    }

    @Override
    public void tick() {
        // 到达目的地后停止
        if (this.mob.distanceToSqr(this.wantedX, this.wantedY, this.wantedZ) < 4.0) {
            this.stop();
        }
    }

    /**
     * 寻找黑暗的地方（光照等级低）
     */
    private boolean findDarkSpot() {
        Vec3 randomPos = DefaultRandomPos.getPos(this.mob, 16, 7);
        if (randomPos == null) {
            return false;
        }
        
        BlockPos pos = BlockPos.containing(randomPos);
        // 检查光照等级，寻找黑暗的地方（光照等级 <= 4）
        if (this.level.getMaxLocalRawBrightness(pos) <= 4) {
            this.wantedX = randomPos.x;
            this.wantedY = randomPos.y;
            this.wantedZ = randomPos.z;
            return true;
        }
        
        return false;
    }

    /**
     * 寻找阴影处（有方块遮挡阳光）
     */
    private boolean findShadedSpot() {
        Vec3 randomPos = DefaultRandomPos.getPos(this.mob, 12, 5);
        if (randomPos == null) {
            return false;
        }
        
        BlockPos pos = BlockPos.containing(randomPos);
        // 检查是否在阴影中（上方有方块遮挡）
        BlockPos abovePos = pos.above();
        if (!this.level.isEmptyBlock(abovePos)) {
            this.wantedX = randomPos.x;
            this.wantedY = randomPos.y;
            this.wantedZ = randomPos.z;
            return true;
        }
        
        return false;
    }

}