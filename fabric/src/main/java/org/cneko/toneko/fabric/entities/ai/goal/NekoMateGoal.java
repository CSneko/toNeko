package org.cneko.toneko.fabric.entities.ai.goal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.fabric.entities.NekoEntity;

import java.util.EnumSet;

public class NekoMateGoal extends Goal {
    private final NekoEntity nekoEntity;
    private double followSpeed;
    private double maxDistanceSq;
    private INeko target;

    public NekoMateGoal(NekoEntity nekoEntity, INeko target, double maxDistance, double followSpeed) {
        this.nekoEntity = nekoEntity;
        this.target = target;
        this.followSpeed = followSpeed;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (target == null) {
            return false;
        }
        double distanceSq = nekoEntity.distanceToSqr(target.getEntity());
        return distanceSq < maxDistanceSq;
    }

    @Override
    public void tick() {
        if (target != null) {
            nekoEntity.getNavigation().moveTo(target.getEntity(), followSpeed);
            // 当距离小于1时，开始breed
            if (nekoEntity.distanceToSqr(target.getEntity()) < 1) {
                breed();
            }
        }
    }

    public void breed(){
        if (nekoEntity.level() instanceof ServerLevel sl) {
            nekoEntity.breed(sl, target);
            target.getEntity().sendSystemMessage(Component.translatable("message.toneko.neko.mate.finish").withStyle(ChatFormatting.GREEN));
        }
        this.stop();
    }


    @Override
    public void stop() {
        super.stop();
        target = null;
    }

    public void setTarget(INeko target){
        this.target = target;
    }

    public void setMaxDistance(double maxDistance){
        this.maxDistanceSq = maxDistance * maxDistance;
    }

    public void setFollowSpeed(double followSpeed) {
        this.followSpeed = followSpeed;
    }
}
