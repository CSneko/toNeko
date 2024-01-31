package com.crystalneko.tonekofabric.entity.ai;

import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

public class FollowAndAttackPlayerGoal extends Goal {
    private final AnimalEntity mobEntity;
    private final PlayerEntity targetPlayer;
    int waiting = 0;
    private final double speed;
    private final double minDistanceSq;
    private final double maxDistanceSq;

    public FollowAndAttackPlayerGoal(AnimalEntity mobEntity, PlayerEntity targetPlayer, double speed, double minDistance, double maxDistance) {
        this.mobEntity = mobEntity;
        this.targetPlayer = targetPlayer;
        this.speed = speed;
        this.minDistanceSq = minDistance * minDistance;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return true; // 总是可以开始
    }

    @Override
    public void start() {
        // 设置实体的目标为玩家
        mobEntity.setTarget(targetPlayer);
    }

    @Override
    public boolean shouldContinue() {
        // 检查目标玩家是否存活并且距离实体在指定范围内
        if (targetPlayer == null || targetPlayer.isDead()) {
            return false;
        }
        double distanceSq = mobEntity.squaredDistanceTo(targetPlayer);
        return distanceSq >= minDistanceSq && distanceSq <= maxDistanceSq;
    }

    @Override
    public void tick() {
        //判断能否执行
        if(waiting > 40) {
            if (targetPlayer != null &&targetPlayer.isAlive()) {
                // 如果攻击目标还活着，就判断距离目标的位置
                double distance = mobEntity.distanceTo(targetPlayer);
                if (distance <= 0.4) {
                    targetPlayer.damage(mobEntity.getDamageSources().generic(),2.0F);
                }
            }
        }else {
            waiting++ ;
        }
        // 更新实体的位置，使其朝向目标玩家并攻击
        mobEntity.getLookControl().lookAt(targetPlayer, 10.0F, mobEntity.getMaxLookPitchChange());
        mobEntity.getNavigation().startMovingTo(targetPlayer, speed);

    }

    @Override
    public void stop() {
        // 清空目标玩家
        mobEntity.setTarget(null);
    }
}
