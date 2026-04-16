package org.cneko.toneko.common.mod.entities.ai.goal;

import lombok.Setter;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;

public class NekoFollowOwnerGoal extends Goal {
    private final NekoEntity nekoEntity;
    private Player owner;
    @Setter
    private double followSpeed;
    private double maxDistanceSq;
    // 寻路冷却计时器
    private int timeToRecalcPath;

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
        this.timeToRecalcPath = 0;
    }

    @Override
    public void tick() {
        if (owner != null) {
            // 每 10 tick重新计算一次路径喵，而不是每秒 20 次喵
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                nekoEntity.getNavigation().moveTo(owner, followSpeed);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        owner = null;
        nekoEntity.getNavigation().stop();
    }

    public void setTarget(Player owner){
        this.owner = owner;
    }


    public void setMaxDistance(double maxDistance){
        this.maxDistanceSq = maxDistance * maxDistance;
    }

}
