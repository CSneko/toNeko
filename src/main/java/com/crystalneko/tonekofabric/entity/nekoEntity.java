package com.crystalneko.tonekofabric.entity;

import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.ToNekoFabric;
import com.crystalneko.tonekofabric.entity.ai.FollowAndAttackPlayerGoal;
import com.crystalneko.tonekofabric.libs.base;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class nekoEntity extends AnimalEntity implements GeoEntity {
    enum status{
        SUCCESS,FAILED,ALREADY_SET,USED,UNKNOWN
    }
    private HashMap<LivingEntity, Integer> hatredMap = new HashMap<>();
    private PlayerEntity hugging;
    private PlayerEntity rider;
    //准备被骑的状态，0代表正常，1代表准备前置，2代表准备，3代表被骑前置，4代表被骑
    private int ready_ride = 0;
    public final RawAnimation MOVE_ANIM = RawAnimation.begin().then("animation.neko.walk", Animation.LoopType.LOOP);
    public final RawAnimation RUN_ANIM = RawAnimation.begin().then("animation.neko.run", Animation.LoopType.LOOP);
    public final RawAnimation HUG_ANIM = RawAnimation.begin().then("animation.neko.hug", Animation.LoopType.LOOP);
    public final RawAnimation SIT_ANIM = RawAnimation.begin().then("animation.neko.sit", Animation.LoopType.LOOP);
    public final RawAnimation SIT_STAND_ANIM = RawAnimation.begin().then("animation.neko.sit.stand", Animation.LoopType.LOOP);
    public final RawAnimation SIT_LIE_ANIM = RawAnimation.begin().then("animation.neko.sit.lie", Animation.LoopType.LOOP);
    public final RawAnimation LIE_STAND_ANIM = RawAnimation.begin().then("animation.neko.lie.stand", Animation.LoopType.LOOP);
    public final RawAnimation STAY_ANIM = RawAnimation.begin().then("animation.neko.stay", Animation.LoopType.LOOP);
    public final RawAnimation FLY_BEGIN_ANIM = RawAnimation.begin().then("animation.neko.fly.begin",Animation.LoopType.LOOP);
    public final RawAnimation FLY_ANIM = RawAnimation.begin().then("animation.neko.fly",Animation.LoopType.LOOP);
    private Map<RawAnimation,Boolean> can_play_anim = new HashMap<>();
    private long walkTimer = 0;
    private long runTimer = 0;
    private long AnimTimer = 0;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final Ingredient TAMING_INGREDIENT;
    public static Map<PlayerEntity,Integer> playerFuckLoli = new HashMap<>();
    private boolean playerCanInteract = false;

    public nekoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.6D);
        //设置碰撞箱
        if (this.isBaby()){
            //如果是baby,则为成体的一半
            this.setBoundingBox(new Box(0, 0, 0, 1, 2, 1));
        }else {
            this.setBoundingBox(new Box(0, 0, 0, 2, 4, 2));
        }
    }



    public status setName(String name){
        String worldName = base.getWorldName(this.getWorld());
        String uuid = this.getUuid().toString();
        //判断是否已经设置名称
        if(sqlite.checkValueExists(worldName + "NekoEnt", "uuid",uuid)){
            //已经设置过名称了，不允许再次设置
            return status.ALREADY_SET;
        }else {
            if(sqlite.checkValueExists(worldName + "Nekos","neko",name)){
                //名称已经被占用
                return status.USED;
            }
            //还没设置过名称，创建名称
            sqlite.saveData(worldName + "NekoEnt", "uuid",uuid);
            sqlite.saveDataWhere(worldName + "NekoEnt", "name","uuid",uuid,name);
            return status.SUCCESS;
        }

    }

    public String getNekoName(){
        String worldName = base.getWorldName(this.getWorld());
        //获取uuid
        String uuid = this.getUuid().toString();
        //判断名称是否存在
        if(sqlite.checkValueExists(worldName + "NekoEnt", "uuid",uuid)){
            //返回名称
            return sqlite.getColumnValue(worldName + "NekoEnt", "name","uuid",uuid);
        }else {
            return "unnamed";
        }
    }

    @Override
    public AnimalEntity createChild(ServerWorld world, PassiveEntity entity) {
        nekoEntity child = new nekoEntity(ToNekoFabric.NEKO,world);
        child.age = -48000;
        return child;
    }
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.CAKE || stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }
    @Override
    public void growUp(int age, boolean overGrow){
        super.growUp(age, overGrow);
        //当生物长大时，设置体积为成体体积
        this.setBoundingBox(new Box(0, 0, 0, 2, 4, 2));
    }


    //移动目标
    @Override
    protected void initGoals() {
        //漫游目标
        TemptGoal temptGoal = new TemptGoal(this, 0.8, TAMING_INGREDIENT, true);
        this.goalSelector.add(1, new SwimGoal(this));
        //this.goalSelector.add(1, new EscapeDangerGoal(this, 0.8));
        this.goalSelector.add(2, temptGoal);
        //this.goalSelector.add(9, new AttackGoal(this));
        this.goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(3, new AnimalMateGoal(this, 0.8));
    }
    @Override
    public void tick(){
        super.tick();
        //骑行时的逻辑
        if (this.rider != null && this.rider.isAlive()) {
            //如果生物被骑着
            if(this.hasPassengers()) {
                // 更新骑乘实体的位置和行为
                this.rider.setPosition(this.getX(), this.getY() + this.getHeight(), this.getZ());
                //给予10tick的漂浮效果
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10));
                handleRiderInput(rider);
            }else {
                //否则清除骑行
                rider = null;
                if(ready_ride > 0) {
                    ready_ride = 0;
                }
            }
        }
    }
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        //获取玩家主手物品
        ItemStack itemStack = player.getMainHandStack();
        Item item = itemStack.getItem();
        //获取物品的id
        Identifier itemId = Registries.ITEM.getId(item);
        //如果可以骑行且玩家主手物品为末地烛
        if (!this.getWorld().isClient && !this.isBeingRidden() && item == Items.END_ROD)  {
            //如果已经准备好被骑
            if(ready_ride == 2){
                setRider(player);
                player.startRiding(this);
            }else if(ready_ride == 1){
                //播放准备动画
                playAnim(FLY_BEGIN_ANIM);
                ready_ride = 2;
            }else if(ready_ride == 0){
                playAnim(FLY_BEGIN_ANIM);
                ready_ride = 1;
            }
            return ActionResult.SUCCESS;
        }
        //如果物品为Better_end_rod的normal_rod
        if(itemId.getPath().equalsIgnoreCase("normal_rod")){
            better_end_rod_interact(player,hand);
        }


        return super.interactMob(player, hand);
    }

    private void handleRiderInput(PlayerEntity rider) {
        // 处理骑乘者的输入
        float forwardMovement = rider.forwardSpeed;
        float sidewaysMovement = rider.sidewaysSpeed;

        // 获取骑乘者的视角
        Vec2f riderRotation = rider.getRotationClient();
        double riderYaw = riderRotation.y;

        // 将视角转换为世界坐标系下的方向向量
        Vec3d forwardVector = this.getRotationVector();
        forwardVector.rotateY((float) Math.toRadians(-riderYaw));
        forwardVector = forwardVector.normalize();

        // 根据方向向量和输入调整生物的位置
        this.move(MovementType.SELF, forwardVector.multiply(forwardMovement).add(new Vec3d(sidewaysMovement, 0, 0)));

    }
    //可否被骑行
    public boolean isBeingRidden() {
        return this.rider != null;
    }
    //设置骑行对象
    public void setRider(PlayerEntity entity) {
        this.rider = entity;
    }

    //对玩家进行拥抱
    public void hug(PlayerEntity player){
        //执行拥抱
        this.playAnim(HUG_ANIM);
    }

    //增加仇恨值
    public void increaseHatred(LivingEntity target, int amount) {
        nekoEntity neko = this;
        int currentHatred = hatredMap.getOrDefault(target, 0);
        if(target instanceof PlayerEntity){
            //让实体尝试跟随目标
            PlayerEntity targetPlayer = (PlayerEntity) target;
            neko.goalSelector.add(1,new FollowAndAttackPlayerGoal(neko,targetPlayer,0.8D,0.1F,100.0F));
        }
        hatredMap.put(target, currentHatred + amount);
    }

    //减少仇恨值
    public void decreaseHatred(LivingEntity target, int amount) {
        int currentHatred = hatredMap.getOrDefault(target, 0);
        int newHatred = Math.max(0, currentHatred - amount);
        if (newHatred == 0) {
            hatredMap.remove(target);
        } else {
            hatredMap.put(target, newHatred);
        }
    }

    //获取当前仇恨值最高的目标
    public LivingEntity getMostHatedTarget() {
        LivingEntity mostHatedTarget = null;
        int maxHatred = 0;
        for (LivingEntity target : hatredMap.keySet()) {
            int hatred = hatredMap.get(target);
            if (hatred > maxHatred) {
                mostHatedTarget = target;
                maxHatred = hatred;
            }
        }
        return mostHatedTarget;
    }


    //------------------------------------------------------------动画-----------------------------------------------
    //播放动画
    public void playAnim(RawAnimation rawAnimation){
        can_play_anim.put(rawAnimation,true);
    }
    //能否播放动画(动画是否在播放列表内)
    private Boolean canPlayAnim(RawAnimation rawAnimation){
        return can_play_anim.get(rawAnimation) != null && can_play_anim.get(rawAnimation);
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
    public boolean canStopAnim(){
        long currentTimestamp = System.currentTimeMillis();
        // 如果时间差小于等于5秒，则返回false,代表不能停止动画。
        return AnimTimer == 0 || currentTimestamp - AnimTimer > 5000;

    }
    public void setAnimTimer(){
        AnimTimer = System.currentTimeMillis();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.walk", 34, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.run", 20, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.hug", 100, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.sit", 20, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.sit.stand", 20, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.sit.lie", 20, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.lie.sit", 20, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.stay", 60, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.fly.begin", 10, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.fly", 10, this::Anim));
    }
    protected <E extends nekoEntity> PlayState Anim(final AnimationState<E> event) {
        boolean isStay = true;
        if (event.isMoving()) {
            isStay = false;
            if(this.getMovementSpeed() <= 0.6F) {
                //如果可以播放动画
                if(canPlayWalkAnim()){
                    event.getController().setAnimation(MOVE_ANIM);
                }
            }else {
                if(canPlayRunAnim()){
                    event.getController().setAnimation(RUN_ANIM);
                }
            }
        }
        if (this.rider != null) {
            isStay = false;
            //播放飞行动画
            event.getController().setAnimation(FLY_ANIM);
        }
        RawAnimation[] animations = new RawAnimation[]{
                FLY_ANIM,FLY_BEGIN_ANIM,HUG_ANIM,SIT_ANIM,SIT_LIE_ANIM,SIT_STAND_ANIM,LIE_STAND_ANIM,STAY_ANIM,MOVE_ANIM,RUN_ANIM
        };
        int i =0;
        while(i < animations.length) {
            if(canPlayAnim(animations[i])){
                //如果可以播放动画就播放动画，并将可否播放设置为false
                event.getController().setAnimation(animations[i]);
                can_play_anim.put(animations[i],false);
                setAnimTimer();
                isStay = false;
            }
            i ++;
        }
        if(isStay && canStopAnim()){
            //停止播放动画
            playAnim(STAY_ANIM);
        }
        return PlayState.CONTINUE;
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }


    //--------------------------------------------------------------杂项------------------------------------------------
    static {
        TAMING_INGREDIENT = Ingredient.ofItems(Items.TROPICAL_FISH,Items.END_ROD,Items.IRON_HOE,Items.IRON_INGOT);
    }
    public void better_end_rod_interact(PlayerEntity player,Hand hand){
        //每点击两次算一次
        if(playerCanInteract){
            playerCanInteract = false;
            return;
        }
        playerCanInteract = true;
        if(this.isBaby()){
            int time = 0;
            if(playerFuckLoli.containsKey(player)){
                //如果保存过次数，则获取保存的次数
                time = playerFuckLoli.get(player);
            }
            switch (time){
                case 0:
                case 1:
                case 2:
                case 4:
                case 5:
                    player.sendMessage(Text.translatable("entity.neko.loli.end_rod.use."+time));
                    playerFuckLoli.put(player,time +1);
                    break;
                case 3:
                    player.sendMessage(Text.translatable("entity.neko.loli.end_rod.use.3"));
                    for (int i = 0;i<3;) {
                        player.sendMessage(Text.translatable("entity.neko.loli.end_rod.use.sys.3"), true);
                        i++;
                    }
                    playerFuckLoli.put(player,time +1);
                    break;
                case 6:
                    //踢出玩家
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.networkHandler.disconnect(Text.translatable("entity.neko.loli.end_rod.use.kick"));
                    }
                    playerFuckLoli.put(player,0);
                    break;
                default:
                    playerFuckLoli.put(player,0);
            }

        }else {
            //添加仇恨
            increaseHatred(player, 100);
            World world = player.getWorld();
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            //播放被伤害音频
            world.playSound(this, this.getBlockPos(), SoundEvent.of(new Identifier("toneko", "entity.neko.hurt_0")),
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);
            //末地烛粒子效果
            int i = 0;
            while (i < 10) {
                world.addParticle(ParticleTypes.FALLING_HONEY, x, y, z, 0.1D, 0.1D, 0.1D);
                world.addParticle(ParticleTypes.DRIPPING_HONEY, x, y, z, 0.1D, 0.1D, 0.1D);
                world.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.1D, 0.1D, 0.1D);
                world.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, 0.1D, 0.1D, 0.1D);
                i++;
            }
        }
    }
    /*@Override
    protected SoundEvent getAmbientSound() {
        int randomNum = new Random().nextInt(5);
        return SoundEvent.of(new Identifier("toneko","entity.neko.stay_"+randomNum));
    }*/

}
