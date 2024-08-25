package org.cneko.toneko.common.mod.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFollowOwnerGoal;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class NekoEntity extends PathfinderMob implements GeoEntity,Neko {
    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    private final AnimatableInstanceCache cache;
    private String skin = "";

    public NekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        NekoQuery.getNeko(this.getUUID()).setNeko(true);
        this.cache = GeckoLibUtil.createInstanceCache(this);

    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        String skin = nbt.getString("Skin");
        if(!skin.isEmpty()) {
            this.setSkin(skin);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        if (!getSkin().isEmpty()) {
            nbt.putString("Skin", getSkin());
        }
        super.addAdditionalSaveData(nbt);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        setSkin(nbt.getString("Skin"));
        if (getSkin().isEmpty()){
            setSkin(getRandomSkin());
        }
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
        // 猫娘会闲逛
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.3, 1));
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

    public String getSkin() {
        return skin;
    }
    public void setSkin(String skin) {
        this.skin = skin;
    }
    public String getRandomSkin(){
        return NekoSkinRegistry.getRandomSkin(this.getType());
    }

    public static AttributeSupplier.Builder createNekoAttributes(){
        return createMobAttributes();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 40, state -> {
            // 没有移动，则播放idle动画
            if (!state.isMoving()){
                state.getController().setAnimation(DefaultAnimations.IDLE);
            }else if (state.isMoving()){
                if (this.getDeltaMovement().lengthSqr()< 0.01){
                    state.getController().setAnimation(DefaultAnimations.WALK);
                }else if (this.getDeltaMovement().lengthSqr() >= 0.01){
                    state.getController().setAnimation(DefaultAnimations.RUN);
                }
            }
            return PlayState.CONTINUE;
        }));
//        controllers.add(new AnimationController<>(this, 40, state -> {
//            // 移动但速度较慢
//            if (state.isMoving() && this.getDeltaMovement().lengthSqr() < 0.025) {
//                state.getController().setAnimation(DefaultAnimations.WALK);
//            }
//            return PlayState.CONTINUE;
//        }));
//        controllers.add(new AnimationController<>(this, 40, state -> {
//            // 移动但速度较快
//            if (state.isMoving() && this.getDeltaMovement().lengthSqr() > 0.025) {
//                state.getController().setAnimation(DefaultAnimations.RUN);
//            }
//            return PlayState.CONTINUE;
//        }));
//        controllers.add(new AnimationController<>(this, 80, state -> {
//            // 游泳动画
//            if (this.isInWaterOrBubble()) {
//                state.getController().setAnimation(DefaultAnimations.SWIM);
//            }
//            return PlayState.CONTINUE;
//        }));

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public LivingEntity getEntity() {
        return this;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }
}
