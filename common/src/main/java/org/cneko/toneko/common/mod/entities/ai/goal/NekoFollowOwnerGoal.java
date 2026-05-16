package org.cneko.toneko.common.mod.entities.ai.goal;

import lombok.Setter;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.List;

public class NekoFollowOwnerGoal extends Goal {
    private final NekoEntity nekoEntity;
    private Player owner;
    @Setter
    private double followSpeed;
    private double maxDistanceSq;
    private int timeToRecalcPath;
    private int tsundereTicks;

    public NekoFollowOwnerGoal(NekoEntity nekoEntity, Player owner, double maxDistance, double followSpeed) {
        this.nekoEntity = nekoEntity;
        this.owner = owner;
        this.followSpeed = followSpeed;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (owner == null) return false;
        List<String> tags = nekoEntity.getMoeTags();
        // tsundere: 8% chance to refuse ("Hmph!")
        if (tags.contains("tsundere") && nekoEntity.getRandom().nextFloat() < 0.08f) {
            tsundereTicks = 20; // don't re-check for 1 second
            return false;
        }
        // shizukana: 30% chance to stay still
        if (tags.contains("shizukana") && nekoEntity.getRandom().nextFloat() < 0.3f) {
            return false;
        }
        double distSq = nekoEntity.distanceToSqr(owner);
        return distSq < getEffectiveMaxDistanceSq();
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void tick() {
        if (owner != null && --this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            nekoEntity.getNavigation().moveTo(owner, getEffectiveSpeed());
        }
    }

    @Override
    public void stop() {
        owner = null;
        nekoEntity.getNavigation().stop();
    }

    public void setTarget(Player owner) {
        this.owner = owner;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistanceSq = maxDistance * maxDistance;
    }

    private double getEffectiveSpeed() {
        List<String> tags = nekoEntity.getMoeTags();
        double speed = followSpeed;
        if (tags.contains("yandere")) speed *= 1.5;
        if (tags.contains("yowaki")) speed *= 0.9;
        if (tags.contains("shizukana")) speed *= 0.7;
        if (tags.contains("yuri") && owner != null && owner.isNeko()) speed *= 1.2;
        return speed;
    }

    private double getEffectiveMaxDistanceSq() {
        List<String> tags = nekoEntity.getMoeTags();
        double dist = Math.sqrt(maxDistanceSq);
        if (tags.contains("yandere")) dist = 40;
        else if (tags.contains("yowaki")) dist = 15;
        if (tags.contains("yuri") && owner != null && owner.isNeko()) dist = Math.min(dist, 20);
        return dist * dist;
    }
}
