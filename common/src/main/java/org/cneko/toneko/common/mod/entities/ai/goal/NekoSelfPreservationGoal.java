package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;

import java.util.EnumSet;

public class NekoSelfPreservationGoal extends Goal {
    private final NekoEntity neko;
    private int cooldown;
    private int eatTicks;

    public NekoSelfPreservationGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        if (neko.isSitting()) return false;
        return neko.getHealth() < neko.getMaxHealth() * 0.5 && hasFood();
    }

    @Override
    public void start() {
        eatTicks = 60;
        // move to a safe spot
        Vec3 target = neko.position().add(neko.getRandom().nextDouble() * 4 - 2, 0, neko.getRandom().nextDouble() * 4 - 2);
        neko.getNekoBrain().submitMove(target.x, target.y, target.z, neko.getAttributeValue(Attributes.MOVEMENT_SPEED), BehaviorPriority.NORMAL, this);
    }

    @Override
    public void tick() {
        if (neko.getNavigation().isDone() && hasFood() && eatTicks-- > 0) {
            // arrived at safe spot, eat
            for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
                ItemStack stack = neko.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                    FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
                    if (food != null) {
                        neko.heal(food.nutrition());
                        stack.shrink(1);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        cooldown = 100;
        eatTicks = 0;
        neko.getNekoBrain().stopMoving(this);
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

    @Override
    public boolean canContinueToUse() {
        return eatTicks > 0 && neko.getHealth() < neko.getMaxHealth();
    }
}
