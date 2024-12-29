package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;

public class NekoFlyingAroundGoal extends Goal {

    private final NekoEntity entity;
    private Vec3 target;
    private static final double SPEED = 0.1;
    private int stuckCounter = 0;

    public NekoFlyingAroundGoal(NekoEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target != null) {
            if (this.entity.distanceToSqr(this.target) < 1) {
                this.target = null;
                stuckCounter = 0; // 重置计数器
            } else {
                // 计算移动方向向量
                Vec3 direction = new Vec3(
                        this.target.x - this.entity.getX(),
                        this.target.y - this.entity.getY(),
                        this.target.z - this.entity.getZ()
                ).normalize().scale(SPEED);

                // 调用 travel 方法处理移动
                this.entity.travel(direction);

                // 调整朝向
                this.entity.getLookControl().setLookAt(this.target);

                stuckCounter++;
                if (stuckCounter > 100) { // 如果超过100 tick没有到达目标
                    this.target = null;
                    stuckCounter = 0;
                }
            }
        } else {
            if (this.entity.getRandom().nextDouble() < 0.1) {
                this.start();
            }
        }
    }



    @Override
    public void start() {
        // 随机飞行
        double range = 10.0;
        double targetX = this.entity.getX() + (this.entity.getRandom().nextDouble() - 0.5) * range;
        double targetY = this.entity.getY() + (this.entity.getRandom().nextDouble() - 0.5) * range *0.4;
        double targetZ = this.entity.getZ() + (this.entity.getRandom().nextDouble() - 0.5) * range;
        this.target = new Vec3(targetX, targetY, targetZ);
    }

    public void travel(Vec3 travelVector) {
        if (entity.isControlledByLocalInstance()) {
            if (entity.isInWater()) {
                entity.moveRelative(0.02F, travelVector);
                entity.move(MoverType.SELF, entity.getDeltaMovement());
                entity.setDeltaMovement(entity.getDeltaMovement().scale(0.800000011920929));
            } else if (entity.isInLava()) {
                entity.moveRelative(0.02F, travelVector);
                entity.move(MoverType.SELF, entity.getDeltaMovement());
                entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
            } else {
                entity.moveRelative(entity.getSpeed(), travelVector);
                entity.move(MoverType.SELF, entity.getDeltaMovement());
                entity.setDeltaMovement(entity.getDeltaMovement().scale(0.9100000262260437));
            }
        }

        entity.calculateEntityAnimation(false);
    }

}
