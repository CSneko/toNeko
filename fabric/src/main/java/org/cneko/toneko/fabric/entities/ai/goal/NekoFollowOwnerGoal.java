package org.cneko.toneko.fabric.entities.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.fabric.entities.NekoEntity;

import java.util.EnumSet;

public class NekoFollowOwnerGoal extends Goal {
    private final NekoEntity nekoEntity;
    private final Player owner;
    private final double followSpeed;
    private final double minDistanceSq;
    private final double maxDistanceSq;

    public NekoFollowOwnerGoal(NekoEntity nekoEntity, Player owner, double minDistance, double maxDistance, double followSpeed) {
        this.nekoEntity = nekoEntity;
        this.owner = owner;
        this.followSpeed = followSpeed;
        this.minDistanceSq = minDistance * minDistance;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (owner == null || !nekoEntity.getNeko().hasOwner(owner.getUUID())) {
            return false;
        }
        double distanceSq = nekoEntity.distanceToSqr(owner);
        return distanceSq > maxDistanceSq || distanceSq < minDistanceSq;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void tick() {
        if (owner != null) {
            nekoEntity.getNavigation().moveTo(owner, followSpeed);
        }
    }

    @Override
    public void stop() {
        nekoEntity.getNavigation().stop();
    }
}
