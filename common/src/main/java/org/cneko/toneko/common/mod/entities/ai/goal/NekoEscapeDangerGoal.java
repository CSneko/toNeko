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
        // 检查是否处于危险状态（如着火、在熔岩、掉落、爆炸等）
        return mob.isOnFire() || mob.isInLava() || mob.fallDistance > 4.0F || mob.getLastDamageSource() != null && mob.getLastDamageSource().getEntity() == null;
    }

    @Override
    public void start() {
        dangerTick = 40; // 逃跑2秒
        Vec3 away = mob.position().add(mob.getRandom().nextDouble() * 8 - 4, 0, mob.getRandom().nextDouble() * 8 - 4);
        mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
    }

    @Override
    public boolean canContinueToUse() {
        return dangerTick-- > 0 && !mob.getNavigation().isDone();
    }
}