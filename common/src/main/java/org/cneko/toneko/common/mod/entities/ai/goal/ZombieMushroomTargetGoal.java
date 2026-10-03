package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;
import org.cneko.toneko.common.util.ConfigUtil;

/** Prefer visible mushroom girls, using vanilla range and target validity checks. */
public final class ZombieMushroomTargetGoal extends NearestAttackableTargetGoal<MushroomGirlEntity> {
    public ZombieMushroomTargetGoal(Zombie zombie) {
        super(zombie, MushroomGirlEntity.class, 0, true, false, null);
    }

    @Override public boolean canUse() {
        return ConfigUtil.isMushroomZombieTargetingEnabled() && super.canUse();
    }

    @Override public boolean canContinueToUse() {
        return ConfigUtil.isMushroomZombieTargetingEnabled() && super.canContinueToUse();
    }
}
