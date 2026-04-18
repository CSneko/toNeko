package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 晒太阳基因的AI Goal
 * 晴天时会寻找阳光充足的地方休息
 * 晒太阳时会有恢复效果
 */
public class MobSunBaskingGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private int searchCooldown;
    private int baskingTicks;
    private boolean isBasking;

    public MobSunBaskingGoal(PathfinderMob mob) {
        this.mob = mob;
        this.level = mob.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 每30-60 tick检查一次
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }
        this.searchCooldown = this.mob.getRandom().nextInt(30) + 30;
        
        // 不需要检查基因，因为只有有晒太阳基因的实体才会有这个Goal
        
        // 必须是白天且晴天
        if (!isDaytime() || !isClearWeather()) {
            return false;
        }
        
        // 如果已经在晒太阳，继续晒太阳
        if (this.isBasking && this.baskingTicks < 600) { // 最多晒太阳30秒
            return true;
        }
        
        // 寻找阳光充足的地方
        return findSunnySpot();
    }

    @Override
    public boolean canContinueToUse() {
        // 继续晒太阳的条件
        if (!isDaytime() || !isClearWeather()) {
            return false;
        }
        
        // 如果在移动中，继续移动
        if (!this.mob.getNavigation().isDone()) {
            return true;
        }
        
        // 如果在晒太阳，检查是否应该继续
        if (this.isBasking) {
            // 检查当前位置是否还有阳光
            BlockPos pos = this.mob.blockPosition();
            if (isPositionSunny(pos)) {
                return this.baskingTicks < 600; // 最多晒太阳30秒
            } else {
                // 阳光消失了，停止晒太阳
                this.isBasking = false;
                return false;
            }
        }
        
        return false;
    }

    @Override
    public void start() {
        this.isBasking = false;
        this.baskingTicks = 0;
        // 开始向阳光点移动
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, 0.8);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.isBasking = false;
        this.baskingTicks = 0;
        this.searchCooldown = this.mob.getRandom().nextInt(60) + 60;
    }

    @Override
    public void tick() {
        if (!this.mob.getNavigation().isDone()) {
            // 还在移动中
            return;
        }
        
        // 到达目的地，开始晒太阳
        if (!this.isBasking) {
            this.isBasking = true;
            this.mob.getLookControl().setLookAt(this.wantedX, this.wantedY + 1, this.wantedZ);
        }
        
        // 晒太阳中
        this.baskingTicks++;
        
        // 每2秒恢复一点生命值（如果受伤了）
        if (this.baskingTicks % 40 == 0 && this.mob.getHealth() < this.mob.getMaxHealth()) {
            this.mob.heal(1.0F);
            // 可以添加粒子效果
            this.level.addParticle(ParticleTypes.HEART, this.mob.getX(), this.mob.getY() + 1, this.mob.getZ(), 0, 0, 0);
        }
        
        // 偶尔改变看向的方向（享受阳光）
        if (this.baskingTicks % 100 == 0) {
            Vec3 randomLook = DefaultRandomPos.getPos(this.mob, 5, 3);
            if (randomLook != null) {
                this.mob.getLookControl().setLookAt(randomLook.x, randomLook.y, randomLook.z);
            }
        }
    }

    /**
     * 寻找阳光充足的地方
     */
    private boolean findSunnySpot() {
        Vec3 randomPos = DefaultRandomPos.getPos(this.mob, 20, 10);
        if (randomPos == null) {
            return false;
        }
        
        BlockPos pos = BlockPos.containing(randomPos);
        // 检查是否阳光充足
        if (isPositionSunny(pos)) {
            this.wantedX = randomPos.x;
            this.wantedY = randomPos.y;
            this.wantedZ = randomPos.z;
            return true;
        }
        
        return false;
    }

    /**
     * 检查位置是否阳光充足
     */
    private boolean isPositionSunny(BlockPos pos) {
        // 检查光照等级（白天阳光充足的地方光照等级高）
        int lightLevel = this.level.getMaxLocalRawBrightness(pos);
        // 检查上方没有方块遮挡（直接看到天空）
        boolean canSeeSky = this.level.canSeeSky(pos);
        
        return lightLevel >= 12 && canSeeSky;
    }

    /**
     * 检查是否是白天
     */
    private boolean isDaytime() {
        long dayTime = this.level.getDayTime() % 24000;
        return dayTime >= 0 && dayTime <= 12000; // 白天时间
    }

    /**
     * 检查是否是晴天
     */
    private boolean isClearWeather() {
        // 没有下雨也没有雷暴
        return !this.level.isRaining() && !this.level.isThundering();
    }
}