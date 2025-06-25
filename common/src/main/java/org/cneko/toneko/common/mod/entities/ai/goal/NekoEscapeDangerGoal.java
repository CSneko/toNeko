package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class NekoEscapeDangerGoal extends Goal {
    private final Mob mob;
    private int dangerTick = 0;

    public NekoEscapeDangerGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // 检查是否处于危险状态（如着火、在熔岩、掉落、爆炸、在水中等）
        return mob.isOnFire()
                || mob.isInLava()
                || mob.isInWater()
                || mob.fallDistance > 4.0F
                || (mob.getLastDamageSource() != null && mob.getLastDamageSource().getEntity() == null);
    }

    @Override
    public void start() {
        dangerTick = 40; // 逃跑2秒
        Vec3 target;
        if (mob.isInWater()) {
            // 在水中时，目标点为当前位置正上方2格
            target = mob.position().add(0, 2, 0);
        } else {
            // 否则随机逃跑
            target = mob.position().add(mob.getRandom().nextDouble() * 8 - 4, 0, mob.getRandom().nextDouble() * 8 - 4);
        }
        mob.getNavigation().moveTo(target.x, target.y, target.z, 1.2);
    }

    @Override
    public boolean canContinueToUse() {
        return dangerTick-- > 0 && !mob.getNavigation().isDone();
    }
}