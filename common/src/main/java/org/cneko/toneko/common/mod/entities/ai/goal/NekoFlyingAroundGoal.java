package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;

public class NekoFlyingAroundGoal extends Goal {

    private final NekoEntity entity;
    private Vec3 target;
    private static final double SPEED = 0.1;
    private int stuckCounter = 0;
    private int cooldown;

    public NekoFlyingAroundGoal(NekoEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return entity.getRandom().nextFloat() < 0.1f;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && stuckCounter <= 100;
    }

    @Override
    public void start() {
        double range = 10.0;
        double targetX = this.entity.getX() + (this.entity.getRandom().nextDouble() - 0.5) * range;
        double targetY = this.entity.getY() + (this.entity.getRandom().nextDouble() - 0.5) * range * 0.4;
        double targetZ = this.entity.getZ() + (this.entity.getRandom().nextDouble() - 0.5) * range;
        this.target = new Vec3(targetX, targetY, targetZ);
        stuckCounter = 0;
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        if (this.entity.distanceToSqr(this.target) < 1) {
            this.target = null;
            stuckCounter = 0;
        } else {
            Vec3 direction = new Vec3(
                    this.target.x - this.entity.getX(),
                    this.target.y - this.entity.getY(),
                    this.target.z - this.entity.getZ()
            ).normalize().scale(SPEED);

            this.entity.travel(direction);
            this.entity.getLookControl().setLookAt(this.target);

            stuckCounter++;
        }
    }

    @Override
    public void stop() {
        target = null;
        cooldown = 60;
    }
}
