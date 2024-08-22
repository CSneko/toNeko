package org.cneko.toneko.common.mod.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFollowOwnerGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NekoEntity extends PathfinderMob implements GeoEntity,Neko {
    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    private final AnimatableInstanceCache cache;

    public NekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        NekoQuery.getNeko(this.getUUID()).setNeko(true);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }


    @Override
    public void registerGoals() {
        super.registerGoals();
        // 猫娘会观察玩家
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 猫娘会近战攻击
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, false));
        // 猫娘需要呼吸才能活呀
        this.goalSelector.addGoal(10, new BreathAirGoal(this));
    }

    public NekoQuery.Neko getNeko() {
        return NekoQuery.getNeko(this.getUUID());
    }

    public void flowingOwner(Player flowingOwner, double minDistance, double maxDistance) {
        if (nekoFollowOwnerGoal != null){
            this.goalSelector.removeGoal(nekoFollowOwnerGoal);
        }
        if (flowingOwner != null) {
            nekoFollowOwnerGoal = new NekoFollowOwnerGoal(this, flowingOwner, minDistance, maxDistance);
            this.goalSelector.addGoal(20, nekoFollowOwnerGoal);
        }
    }
    public void setFlowingOwner(Player flowingOwner) {
        flowingOwner(flowingOwner, 0.5D, 30.0D);
    }
    public NekoFollowOwnerGoal getFlowingOwner() {
        return this.nekoFollowOwnerGoal;
    }

    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public LivingEntity getEntity() {
        return this;
    }
}
