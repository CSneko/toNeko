package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.enchantment.effects.SpawnParticlesEffect;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.Random;

public class NekoMateGoal extends Goal {
    private final NekoEntity nekoEntity;
    private double followSpeed;
    private double maxDistanceSq;
    public INeko target;
    public short mating = 0;

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
            // 当距离小于1时，开始贴贴
            if (nekoEntity.distanceToSqr(target.getEntity()) < 1) {
                if (mating >= 30) {
                    breed();
                }
                mating++;
            }
        }
    }

    public void breed(){
        if (nekoEntity.level() instanceof ServerLevel sl) {
            nekoEntity.breed(sl, target);
            //target.getEntity().sendSystemMessage(Component.translatable("message.toneko.neko.mate.finish").withStyle(ChatFormatting.GREEN));
            // 显示爱心粒子（加随机）
            RandomSource random = sl.random;
            sl.getLevel().addParticle(
                    ParticleTypes.HEART,
                    nekoEntity.getX() + random.nextDouble() * 0.5 - 0.25,
                    nekoEntity.getY() + random.nextDouble() * 0.5 - 0.25,
                    nekoEntity.getZ() + random.nextDouble() * 0.5 - 0.25,
                    0,
                    2.0,
                    0
            );
        }
        this.stop();
        nekoEntity.afterMate();
    }


    @Override
    public void stop() {
        super.stop();
        target = null;
        mating = 0;
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
