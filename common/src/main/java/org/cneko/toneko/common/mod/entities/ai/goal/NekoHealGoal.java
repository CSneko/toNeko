package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.List;

public class NekoHealGoal extends Goal {
    private final NekoEntity neko;
    private LivingEntity target;
    private int timeToRecalcPath;

    public NekoHealGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!neko.getMoeTags().contains("gentleness")) return false;
        if (!hasFood()) return false;
        target = findInjuredEntity();
        return target != null;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        if (neko.distanceToSqr(target) < 3.0) {
            // feed the target
            for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
                ItemStack stack = neko.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                    FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
                    if (food != null) {
                        target.heal(food.nutrition());
                        stack.shrink(1);
                        // heart particles
                        neko.level().addParticle(ParticleTypes.HEART,
                                target.getX(), target.getY() + 1, target.getZ(),
                                0, 0.1, 0);
                        break;
                    }
                }
            }
            target = null;
        } else {
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                neko.getNavigation().moveTo(target, 0.6);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive();
    }

    @Override
    public void stop() {
        target = null;
        neko.getNavigation().stop();
    }

    private LivingEntity findInjuredEntity() {
        List<LivingEntity> entities = neko.level().getEntitiesOfClass(LivingEntity.class,
                neko.getBoundingBox().inflate(5), e ->
                        e != neko && e.isAlive() && e.getHealth() < e.getMaxHealth() * 0.5);
        if (entities.isEmpty()) return null;
        return entities.get(0);
    }

    private boolean hasFood() {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                return true;
            }
        }
        return false;
    }
}
