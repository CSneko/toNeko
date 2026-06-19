package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;

import java.util.EnumSet;
import java.util.UUID;

public class NekoYandereDefenseGoal extends Goal {
    private final NekoEntity neko;
    private LivingEntity target;
    private int timeToRecalcPath;

    public NekoYandereDefenseGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!neko.getMoeTags().contains("yandere")) return false;
        if (neko.getOwners().isEmpty()) return false;

        UUID ownerId = neko.getOwners().keySet().iterator().next();
        Player owner = neko.level().getPlayerByUUID(ownerId);
        if (owner == null || neko.distanceToSqr(owner) > 144) return false; // 12 blocks

        if (owner.getLastDamageSource() != null && owner.getLastDamageSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker != neko) {
                target = attacker;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        if (neko.distanceToSqr(target) < 3.0) {
            neko.doHurtTarget(target);
            target = null; // one hit then stop
        } else {
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                neko.getNekoBrain().submitMove(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2, BehaviorPriority.CRITICAL, this);
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
        neko.getNekoBrain().stopMoving(this);
    }
}
