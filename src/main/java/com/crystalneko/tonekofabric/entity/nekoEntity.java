package com.crystalneko.tonekofabric.entity;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;


public class nekoEntity extends AnimalEntity implements GeoEntity {
    private long walkTimer = 0;
    private long runTimer = 0;
    protected static final RawAnimation MOVE_ANIM = RawAnimation.begin().then("animation.neko.walk", Animation.LoopType.LOOP);
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().then("animation.neko.run", Animation.LoopType.LOOP);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final Ingredient TAMING_INGREDIENT;

    public nekoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.5D);
    }

    @Override
    public AnimalEntity createChild(ServerWorld world, PassiveEntity entity) {
        return this;
    }


    //移动目标
    @Override
    protected void initGoals() {
        //漫游目标，来自于CatEntity
        TemptGoal temptGoal = new TemptGoal(this, 0.4, TAMING_INGREDIENT, true);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 0.8));
        this.goalSelector.add(4, temptGoal);
        this.goalSelector.add(9, new AttackGoal(this));
        this.goalSelector.add(11, new WanderAroundFarGoal(this, 0.3, 1.0000001E-5F));
        this.goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
    }
    static {
        TAMING_INGREDIENT = Ingredient.ofItems(Items.TROPICAL_FISH,Items.END_ROD);
    }






    //------------------------------------------------------------动画-----------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.walk", 34, this::moveAnim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.run", 20, this::moveAnim));
    }
    protected <E extends nekoEntity> PlayState moveAnim(final AnimationState<E> event) {
        if (event.isMoving()) {
            if(this.speed <= 0.6F) {
                //如果可以播放动画
                if(canPlayWalkAnim()){
                    event.getController().setAnimation(MOVE_ANIM);
                }
            }else {
                if(canPlayRunAnim()){
                    event.getController().setAnimation(RUN_ANIM);
                }
            }
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    //判断行走动画执行时间是否已到
    public boolean canPlayWalkAnim() {
        long currentTimestamp = System.currentTimeMillis();
        // 如果上一次调用的时间未初始化或者距离当前时间超过了1.7083秒，则重新初始化时间戳并返回true，代表可以播放动画
        if (walkTimer == 0 || currentTimestamp - walkTimer > 1708) {
            walkTimer = currentTimestamp;
            return true;
        }
        // 如果时间差小于等于1.7083秒，则返回false,代表不能播放动画。
        return false;
    }
    //判断能否播放跑步动画
    public boolean canPlayRunAnim() {
        long currentTimestamp = System.currentTimeMillis();
        // 如果上一次调用的时间未初始化或者距离当前时间超过了1秒，则重新初始化时间戳并返回true，代表可以播放动画
        if (runTimer == 0 || currentTimestamp - runTimer > 1000) {
            runTimer = currentTimestamp;
            return true;
        }
        // 如果时间差小于等于1秒，则返回false,代表不能播放动画。
        return false;
    }

    //--------------------------------------------------------------杂项------------------------------------------------

}
