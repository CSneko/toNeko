package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;

public class NekoFlyingAroundGoal extends Goal {

    private final NekoEntity entity;
    private Vec3 target;
    private static final double SPEED = 0.1;

    public NekoFlyingAroundGoal(NekoEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target != null){
            if (this.entity.distanceToSqr(this.target) < 1){
                this.target = null;
            } else {
                this.entity.getLookControl().setLookAt(this.target);
                this.entity.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, SPEED);
            }
        }else {
            // %0.1的几率再次设置目标
            if (this.entity.getRandom().nextDouble() < 0.1){
                this.start();
            }
        }
    }

    @Override
    public void start() {
        // 随机飞行
        double targetX = this.entity.getX() + (this.entity.getRandom().nextDouble() - 0.5) * 5;
        double targetY = this.entity.getY() + this.entity.getRandom().nextDouble() * 5;
        double targetZ = this.entity.getZ() + (this.entity.getRandom().nextDouble() - 0.5) * 5;
        this.target = new Vec3(targetX, targetY, targetZ);
    }
}
