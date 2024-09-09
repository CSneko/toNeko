package org.cneko.toneko.fabric.entities.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NekoFollowOwnerGoal extends TargetGoal {
    private final NekoEntity nekoEntity;
    private final Player owner;
    private final double minDistanceSq;
    private final double maxDistanceSq;
    public NekoFollowOwnerGoal(NekoEntity nekoEntity, Player owner ,double minDistance, double maxDistance){
        super(nekoEntity,true);
        this.nekoEntity = nekoEntity;
        this.owner = owner;
        this.minDistanceSq = minDistance * minDistance;
        this.maxDistanceSq = maxDistance * maxDistance;
    }

    @Override
    public boolean canUse() {
        return owner != null && nekoEntity.getNeko().hasOwner(owner.getUUID());
    }

    @Override
    public void start() {
        super.start();
        // 设置目标
        nekoEntity.setTarget(owner);
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity potentialTarget, @NotNull TargetingConditions targetPredicate) {
        return false;
    }

    @Override
    public void stop() {
        // 清空目标玩家
        nekoEntity.setTarget(null);
    }
}
