package com.crystalneko.tonekofabric.entity;

import com.crystalneko.tonekofabric.ToNekoFabric;
import com.crystalneko.tonekofabric.api.NekoEntityEvents;
import com.crystalneko.tonekofabric.entity.ai.FollowAndAttackPlayerGoal;
import com.crystalneko.tonekofabric.libs.base;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.crystalneko.tonekofabric.api.NekoEntityEnum.NameStatus;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;
/**
 <h2>代码目录:</h2>
 <ol>
 <li>构造函数
 <ul>
 <li><code>{@link nekoEntity#nekoEntity}</code></li>
 </ul>
 </li>
 <li>实体属性
 <ul>
 <li><code>{@link nekoEntity#getAttributeValue}</code></li>
 </ul>
 </li>
 <li>游戏数据生成
 <ul>
 <li>判断繁殖物品是否有效：<code>{@link nekoEntity#isBreedingItem(ItemStack)}</code></li>
 <li>生成幼体：<code>{@link nekoEntity#createChild(ServerWorld, PassiveEntity)}</code></li>
 <li>幼体长大：<code>{@link nekoEntity#growUp(int, boolean)}</code></li>
 </ul>
 </li>
 <li>AI
 <ul>
 <li>注册初始AI：<code>{@link nekoEntity#initGoals()}</code></li>
 </ul>
 </li>
 <li>玩家操作
 <ul>
 <li>攻击时的操作: <code>{@link nekoEntity#handleAttack(Entity)}</code></li>
 <li>右键点击操作：<code>{@link nekoEntity#interactMob(PlayerEntity, Hand)}</code></li>
 <li>骑行：
 <ul>
 <li>控制骑行：<code>{@link nekoEntity#handleRiderInput(PlayerEntity)}</code></li>
 <li>判断是否可以骑行：<code>{@link nekoEntity#isBeingRidden()}</code></li>
 <li>设置骑手：<code>{@link nekoEntity#setRider(PlayerEntity)}</code></li>
 </ul>
 </li>
 </ul>
 </li>
 <li>仇恨
 <ul>
 <li>增加仇恨值：<code>{@link nekoEntity#increaseHatred(LivingEntity, int)}</code></li>
 <li>减少仇恨值：<code>{@link nekoEntity#decreaseHatred(LivingEntity, int)}</code></li>
 <li>获取仇恨值最高的目标：<code>{@link nekoEntity#getMostHatedTarget()}</code></li>
 </ul>
 </li>
 <li>动画
 <ul>
 <li>GeckoLib：
 <ul>
 <li>注册动画：<code>{@link nekoEntity#registerControllers(AnimatableManager.ControllerRegistrar)}</code></li>
 <li>动画事件：<code>{@link nekoEntity#Anim(AnimationState)}</code></li>
 <li>获取动画缓存：<code>{@link nekoEntity#getAnimatableInstanceCache()}</code></li>
 </ul>
 </li>
 <li>动画执行：
 <ul>
 <li>播放动画：<code>{@link nekoEntity#playAnim(RawAnimation)}</code></li>
 <li>判断是否可以播放某个动画：<code>{@link nekoEntity#canPlayAnim(RawAnimation)}</code></li>
 <li>判断是否可以停止播放动画：<code>{@link nekoEntity#canStopAnim()}</code></li>
 <li>判断是否可以播放跑动画：<code>{@link nekoEntity#canPlayRunAnim()}</code></li>
 <li>判断是否可以播放行走动画：<code>{@link nekoEntity#canPlayWalkAnim()}</code></li>
 <li>设置动画执行器：<code>{@link nekoEntity#setAnimTimer()}</code></li>
 </ul>
 </li>
 </ul>
 </li>
 <li>体积
 <ul>
 <li>设置渲染缩放：<code>{@link nekoEntity#setScale}</code></li>
 <li>获取渲染缩放：<code>{@link nekoEntity#getScale()}</code></li>
 </ul>
 </li>
 <li>相关信息
 <ul>
 <li>获取猫娘名字：<code>{@link nekoEntity#getNekoName()}</code></li>
 <li>设置猫娘名字：<code>{@link nekoEntity#setName(String)}</code></li>
 </ul>
 </li>
 </ol>
 */
public class nekoEntity extends AnimalEntity implements GeoEntity {
    private static final TrackedData<NbtCompound> SCALE_DATA;
    private final HashMap<LivingEntity, Integer> hatredMap = new HashMap<>();
    private PlayerEntity rider;
    //准备被骑的状态，0代表正常，1代表准备前置，2代表准备，3代表被骑前置，4代表被骑
    private int ready_ride = 0;
    public final RawAnimation MOVE_ANIM = RawAnimation.begin().then("animation.neko.walk", Animation.LoopType.LOOP);
    public final RawAnimation RUN_ANIM = RawAnimation.begin().then("animation.neko.run", Animation.LoopType.LOOP);
    public final RawAnimation SIT_ANIM = RawAnimation.begin().then("animation.neko.sit", Animation.LoopType.LOOP);
    public final RawAnimation SIT_STAND_ANIM = RawAnimation.begin().then("animation.neko.sit.stand", Animation.LoopType.LOOP);
    public final RawAnimation SIT_LIE_ANIM = RawAnimation.begin().then("animation.neko.sit.lie", Animation.LoopType.LOOP);
    public final RawAnimation LIE_STAND_ANIM = RawAnimation.begin().then("animation.neko.lie.stand", Animation.LoopType.LOOP);
    public final RawAnimation STAY_ANIM = RawAnimation.begin().then("animation.neko.stay", Animation.LoopType.LOOP);
    public final RawAnimation FLY_BEGIN_ANIM = RawAnimation.begin().then("animation.neko.fly.begin",Animation.LoopType.LOOP);
    public final RawAnimation FLY_ANIM = RawAnimation.begin().then("animation.neko.fly",Animation.LoopType.LOOP);
    private final Map<RawAnimation,Boolean> can_play_anim = new HashMap<>();
    private long walkTimer = 0;
    private long runTimer = 0;
    private long AnimTimer = 0;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final Ingredient TAMING_INGREDIENT;

    public Box boundingBox_Baby = new Box(0,0,0,0.5,1,0.5);
    public Box boundingBox = new Box(0,0,0,1,2,1);
    public Vec3d scale = new Vec3d(1,1,1);
    

    // --------------------------------------------------------构造函数--------------------------------------------------
    public nekoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.6D);
        this.speed = 0.6F;
        //设置碰撞箱
        if (this.isBaby()){
            //如果是baby,则为成体的一半
            this.setBoundingBox(this.boundingBox_Baby);
        }else {
            this.setBoundingBox(this.boundingBox);
        }
        try {
            NbtCompound scaleNbt = this.getDataTracker().get(SCALE_DATA);
            if(scaleNbt != null){
                this.setScale(scaleNbt.getDouble("x"),scaleNbt.getDouble("y"),scaleNbt.getDouble("z"));
            }
        }catch (NullPointerException e){
            this.setScale(1,1,1);
        }


    }


    @Override @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        if(entityNbt != null){
            NbtCompound scaleNbt = entityNbt.getCompound("scale");
            if (!scaleNbt.toString().equalsIgnoreCase("{}")) {
                this.setScale(scaleNbt.getDouble("x"),scaleNbt.getDouble("y"),scaleNbt.getDouble("z"));
            }
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SCALE_DATA, new NbtCompound());
    }
    // ---------------------------------------------------------属性-------------------------------------------------
    @Override
    public double getAttributeValue(EntityAttribute attribute) {
        if(attribute == EntityAttributes.GENERIC_MAX_HEALTH){
            // 最大生命值
            return 15.0;
        }
        return this.getAttributes().getValue(attribute);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        NbtCompound scaleNbt = new NbtCompound();
        scaleNbt.putDouble("x",this.getScale().getX());
        scaleNbt.putDouble("y",this.getScale().getY());
        scaleNbt.putDouble("z",this.getScale().getZ());
        nbt.put("scale",scaleNbt);
    }
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if(!nbt.getCompound("scale").toString().equalsIgnoreCase("{}")) {
            NbtCompound scaleNbt = nbt.getCompound("scale");
            this.setScale(new Vec3d(scaleNbt.getDouble("x"), scaleNbt.getDouble("y"), scaleNbt.getDouble("z")));
        }
    }

    // --------------------------------------------------------繁殖-----------------------------------------------------
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.CAKE || stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }
    @Override
    public AnimalEntity createChild(ServerWorld world, PassiveEntity entity) {
        nekoEntity child = new nekoEntity(ToNekoFabric.NEKO,world);
        child.age = -48000;
        return child;
    }
    @Override
    public void growUp(int age, boolean overGrow){
        super.growUp(age, overGrow);
        //当生物长大时，设置体积为成体体积
        this.setBoundingBox(this.boundingBox);
        //调用监听事件
        NekoEntityEvents.GROW_UP.invoker().onGrowUp(this, age, overGrow);
    }

    // ---------------------------------------------------------AI----------------------------------------------------
    //注册AI
    @Override
    protected void initGoals() {
        //漫游目标
        TemptGoal temptGoal = new TemptGoal(this, 0.8, TAMING_INGREDIENT, true);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, temptGoal);
        this.goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(3, new AnimalMateGoal(this, 0.8));
        this.goalSelector.add(10,new EscapeDangerGoal(this, this.speed*0.5));
    }

    // -----------------------------------------------------玩家操作----------------------------------------------------
    // 玩家攻击实体时
    @Override
    public boolean handleAttack(Entity entity){
        // 注册监听事件
        boolean result = NekoEntityEvents.ON_ATTACK.invoker().onAttack(this, entity);
        if(result){
            return true;
        }
        if (entity instanceof PlayerEntity player){
            // 添加仇恨
            this.increaseHatred(player,100);
            return false;
        }
        return super.handleAttack(entity);
    }

    // 玩家右键点击
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ActionResult result = NekoEntityEvents.ON_INTERACT.invoker().onInteract(this, player, hand);
        if(result != ActionResult.PASS){
            return result;
        }
        //获取玩家主手物品
        ItemStack itemStack = player.getMainHandStack();
        Item item = itemStack.getItem();
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
        return super.interactMob(player, hand);
    }
    // -------------------------------------------------------骑行------------------------------------------------
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

    // ----------------------------------------------------------仇恨------------------------------------------------
    //增加仇恨值
    public void increaseHatred(LivingEntity target, int amount) {
        nekoEntity neko = this;
        int currentHatred = hatredMap.getOrDefault(target, 0);
        if(target instanceof PlayerEntity targetPlayer){
            //让实体尝试跟随目标
            neko.goalSelector.add(1,new FollowAndAttackPlayerGoal(neko,targetPlayer,this.speed*2,0.1F,100.0F,2.0F));
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
        // -------------------------------------------------------geckoLib------------------------------------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.walk", 34, this::Anim));
        controllerRegistrar.add(new AnimationController<>(this, "animation.neko.run", 20, this::Anim));
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
                FLY_ANIM,FLY_BEGIN_ANIM,SIT_ANIM,SIT_LIE_ANIM,SIT_STAND_ANIM,LIE_STAND_ANIM,STAY_ANIM,MOVE_ANIM,RUN_ANIM
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
    // -----------------------------------------------------------动画执行-----------------------------------------------
    //播放动画
    public void playAnim(RawAnimation rawAnimation){
        can_play_anim.put(rawAnimation,true);
    }
    //能否播放动画(动画是否在播放列表内)
    private Boolean canPlayAnim(RawAnimation rawAnimation){
        return can_play_anim.get(rawAnimation) != null && can_play_anim.get(rawAnimation);
    }
    //能否能停止播放动画
    public boolean canStopAnim(){
        long currentTimestamp = System.currentTimeMillis();
        // 如果时间差小于等于5秒，则返回false,代表不能停止动画。
        return AnimTimer == 0 || currentTimestamp - AnimTimer > 5000;

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
    // 设置动画计时器
    public void setAnimTimer(){
        AnimTimer = System.currentTimeMillis();
    }

    // ----------------------------------------------------------设置大小---------------------------------------------
    //设置渲染缩放
    public void setScale(Vec3d scale){
        this.scale = scale; // 设置缩放比例
        // 创建一个新的NbtCompound用于保存数据
        NbtCompound nbt = new NbtCompound();
        // 创建一个嵌套的NbtCompound用于存储scale.x, scale.y, scale.z
        NbtCompound scaleNbt = new NbtCompound();
        scaleNbt.putDouble("x", scale.x);
        scaleNbt.putDouble("y", scale.y);
        scaleNbt.putDouble("z", scale.z);
        nbt.put("scale", scaleNbt);
        this.dataTracker.set(SCALE_DATA, nbt);
    }
    public void setScale(double x, double y, double z){
        this.setScale(new Vec3d(x,y,z));
    }
    //获取渲染缩放
    public Vec3d getScale() {
        return this.scale;
    }

    // ------------------------------------------------------相关信息------------------------------------------------
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
    public NameStatus setName(String name){
        String worldName = base.getWorldName(this.getWorld());
        String uuid = this.getUuid().toString();
        //判断是否已经设置名称
        if(sqlite.checkValueExists(worldName + "NekoEnt", "uuid",uuid)){
            //已经设置过名称了，不允许再次设置
            return NameStatus.ALREADY_SET;
        }else {
            if(sqlite.checkValueExists(worldName + "Nekos","neko",name)){
                //名称已经被占用
                return NameStatus.USED;
            }
            //还没设置过名称，创建名称
            sqlite.saveData(worldName + "NekoEnt", "uuid",uuid);
            sqlite.saveDataWhere(worldName + "NekoEnt", "name","uuid",uuid,name);
            return NameStatus.SUCCESS;
        }
    }

    //--------------------------------------------------------------杂项------------------------------------------------
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
        // 执行监听事件
        NekoEntityEvents.TICK.invoker().onTick(this);
    }
    static {
        TAMING_INGREDIENT = Ingredient.ofItems(Items.END_ROD,Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE);
        SCALE_DATA = DataTracker.registerData(nekoEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    }

}
