package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import org.cneko.toneko.common.mod.entities.NekoEntity;

public class NekoFlyingAroundGoal extends Goal {

    private final NekoEntity entity;

    public NekoFlyingAroundGoal(NekoEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        // 随机飞行
        double targetX = this.entity.getX() + (this.entity.getRandom().nextDouble() - 0.5) * 10;
        double targetY = this.entity.getY() + this.entity.getRandom().nextDouble() * 5;
        double targetZ = this.entity.getZ() + (this.entity.getRandom().nextDouble() - 0.5) * 10;
        this.entity.getNavigation().moveTo(targetX, targetY, targetZ, 0.5);
    }
}
