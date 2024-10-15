package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;

public class NekoFollowOwnerGoal extends Goal {
    private final NekoEntity nekoEntity;
    private Player owner;
    private double followSpeed;
    private double maxDistanceSq;

    public NekoFollowOwnerGoal(NekoEntity nekoEntity, Player owner, double maxDistance, double followSpeed) {
        this.nekoEntity = nekoEntity;
        this.owner = owner;
        this.followSpeed = followSpeed;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (owner == null) {
            return false;
        }
        double distanceSq = nekoEntity.distanceToSqr(owner);
        return distanceSq < maxDistanceSq;
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
        super.stop();
        owner = null;
    }

    public void setTarget(Player owner){
        this.owner = owner;
    }


    public void setMaxDistance(double maxDistance){
        this.maxDistanceSq = maxDistance * maxDistance;
    }

    public void setFollowSpeed(double followSpeed) {
        this.followSpeed = followSpeed;
    }
}
