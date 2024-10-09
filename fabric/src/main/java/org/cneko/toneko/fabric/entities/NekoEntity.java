package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.fabric.entities.ai.goal.NekoFollowOwnerGoal;
import org.cneko.toneko.fabric.entities.ai.goal.NekoMateGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

public abstract class NekoEntity extends AgeableMob implements GeoEntity, INeko {
    public static double DEFAULT_FIND_RANGE = 16.0D;
    public static float DEFAULT_RIDE_RANGE = 3f;

    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    public NekoMateGoal nekoMateGoal;
    private final AnimatableInstanceCache cache;
    private boolean isSitting = false;

    public static final EntityDataAccessor<String> SKIN_DATA_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);

    public NekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        NekoQuery.getNeko(this.getUUID()).setNeko(true);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN_DATA_ID,"grmmy");
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Skin")) {
            this.setSkin(nbt.getString("Skin"));
        }
        randomize(nbt);
    }

//    @Override
//    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
//        super.addAdditionalSaveData(nbt);
//        nbt.putString("Skin", getSkin());
//    }

    @Override
    public @NotNull CompoundTag saveWithoutId(CompoundTag compound) {
        compound.putString("Skin", getSkin());
        return super.saveWithoutId(compound);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("Skin")) {
            this.setSkin(nbt.getString("Skin"));
        }
    }

//    @Override
//    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
//        super.defineSynchedData(builder);
//        builder.define(SKIN_DATA_ID,this.getSkin());
//    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
        if (dataAccessor.equals(SKIN_DATA_ID)) {
            this.setSkin(getEntityData().get(SKIN_DATA_ID));
        }
        super.onSyncedDataUpdated(dataAccessor);
    }

    public void randomize(CompoundTag nbt){
        // 设置名字（如果没有）
        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
        }

//        EntityUtil.randomizeAttributeValue(this, Attributes.SCALE,1,0.8,1.05); // 实体的体型为0.8~1.05间
        EntityUtil.randomizeAttributeValue(this, Attributes.MOVEMENT_SPEED,0.7,0.5,0.6); // 实体速度为0.5~0.6间
    }


    @Override
    public void registerGoals() {
        super.registerGoals();
        // 猫娘会观察玩家
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 猫娘会近战攻击
        this.goalSelector.addGoal(10, new MeleeAttackGoal(this, 1.0D, false));
        // 猫娘需要呼吸才能活呀
        this.goalSelector.addGoal(10, new BreathAirGoal(this));
        // 猫娘会闲逛
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.3, 1));
        // 猫娘会跟主人
        nekoFollowOwnerGoal = new NekoFollowOwnerGoal(this,null,30,this.followLeashSpeed() / 1.5);
        this.goalSelector.addGoal(20,nekoFollowOwnerGoal);
        // 猫娘有繁殖欲望
        nekoMateGoal = new NekoMateGoal(this,null,30,this.followLeashSpeed() / 2);
        this.goalSelector.addGoal(30,nekoMateGoal);
    }

    public NekoQuery.Neko getNeko() {
        return NekoQuery.getNeko(this.getUUID());
    }

    public void followOwner(Player followingOwner,double maxDistance, double followSpeed) {
        if (nekoFollowOwnerGoal !=null) {
            nekoFollowOwnerGoal.setTarget(followingOwner);
            nekoFollowOwnerGoal.setMaxDistance(maxDistance);
            nekoFollowOwnerGoal.setFollowSpeed(followSpeed);
            nekoFollowOwnerGoal.start();
        }

    }
    public void followOwner(Player followingOwner) {
        followOwner(followingOwner, this.getAttributeValue(Attributes.FOLLOW_RANGE),this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }
    public @Nullable NekoFollowOwnerGoal getFollowingOwner() {
        return this.nekoFollowOwnerGoal;
    }

    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    public String getSkin() {
        if (this.entityData == null)
            return this.getRandomSkin();
        return this.entityData.get(SKIN_DATA_ID);
    }
    public void setSkin(String skin) {
        this.entityData.set(SKIN_DATA_ID, skin);
    }
    public String getRandomSkin(){
        return NekoSkinRegistry.getRandomSkin(this.getType());
    }
    // 最喜欢的物品
    public Set<Item> getFavoriteItems(){
        return new HashSet<>();
    }
    // 是否喜欢这个物品
    public boolean isLikedItem(ItemStack stack){
        return this.getFavoriteItems().contains(stack.getItem()) ||
                stack.is(TagKey.create(Registries.ITEM, getTagKeyLocation("liked_items")));
    }

    public boolean giftItem(Player player, int slot){
        return giftItem(player, player.getInventory().getItem(slot));
    }
    // 赠送物品
    public boolean giftItem(Player player, ItemStack stack){
        // 如果是喜欢的物品
        if (this.isLikedItem(stack)){
            // TODO：把物品放到背包里面
            player.getInventory().removeItem(player.getMainHandItem());
            // 播放爱心粒子
            this.level().addParticle(ParticleTypes.HEART,this.getX()+1.8, this.getY(), this.getZ(),1,1,1);
            if (player instanceof ServerPlayer sp){
                // 发送给客户端
                ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART, true, this.getX() + 1.8, this.getY(), this.getZ(), 2, 2, 2, 1, 1);
                sp.connection.send(packet);
                // 随机发送感谢消息
                player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_success",3, Objects.requireNonNull(this.getCustomName()).getString()));
            }
            // 设置玩家为主人
            if (!this.getNeko().hasOwner(player.getUUID())){
                this.getNeko().addOwner(player.getUUID());
            }else {
                // 如果是主人，则添加好感
                this.getNeko().addXp(player.getUUID(), 100);
            }
            return true;
        }else {
            if (player instanceof ServerPlayer) {
                player.sendSystemMessage(randomTranslatabledComponent("message.toneko.neko.gift_fail", 3, Objects.requireNonNull(this.getCustomName()).getString()));
            }
            return false;
        }
    }

    public void tryMating(ServerLevel level, INeko mate) {
        if (this.canMate(mate)) {
            this.nekoMateGoal.setTarget(mate);
            mate.getEntity().sendSystemMessage(Component.translatable("message.toneko.neko.mate.start",this.getName(), mate.getEntity().getName()).withStyle(ChatFormatting.GREEN));
        }
    }
    public boolean canMate(INeko other){
        return other.getNeko().isNeko() || other.allowMateIfNotNeko();
    }

    public void breed(ServerLevel level, INeko mate) {
        // 冒爱心
        level.addParticle(ParticleTypes.HEART, this.getX(), this.getY(), this.getZ(), 1, 1, 1);
        Packet<?> packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART,true, this.getX(), this.getY(), this.getZ(),3,3,3,0.2f,25);
        if (mate instanceof ServerPlayer sp){
            sp.connection.send(packet);
        }
        this.spawnChildFromBreeding(level, mate);
    }
    public void spawnChildFromBreeding(ServerLevel level, INeko mate) {
        NekoEntity child = this.getBreedOffspring(level, mate);
        if (child != null) {
            child.setBaby(true);
            child.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            this.finalizeSpawnChildFromBreeding(level, mate, child);
            level.addFreshEntityWithPassengers(child);

        }
    }
    public void finalizeSpawnChildFromBreeding(ServerLevel level, INeko mate, NekoEntity child) {
        level.broadcastEntityEvent(this, (byte)18);
        child.setAge(-72000);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(level, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
        }
    }
    @Nullable
    public abstract NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent);
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        if (otherParent instanceof INeko neko){
            return this.getBreedOffspring(level, neko);
        }
        return null;
    }

    // 当玩家右键
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // shift+右键打开互动菜单
        if (hand.equals(InteractionHand.MAIN_HAND) && player.isShiftKeyDown() && player instanceof ServerPlayer sp){
            openInteractiveMenu(sp);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public void openInteractiveMenu(ServerPlayer player) {
//        ServerPlayNetworking.send(player, new NekoEntityInteractivePayload(this.getUUID().toString()));
    }

    @Override
    public boolean startRiding(@NotNull Entity vehicle, boolean force) {
        this.isSitting = super.startRiding(vehicle, force);
        return this.isSitting;
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        this.isSitting = false;
    }

//    @Override
//    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
//        controllers.add(new AnimationController<>(this, 20, state -> {
//            // 地上趴着
//            if (this.getPose() == Pose.SWIMMING && !this.isInLiquid()){
//                return state.setAndContinue(DefaultAnimations.CRAWL);
//            }
//            // 在水里
//            if (this.isInLiquid() && this.isEyeInFluid(FluidTags.WATER)){
//                if (state.isMoving()) {
//                    return state.setAndContinue(DefaultAnimations.SWIM);
//                }else {
//                    return state.setAndContinue(DefaultAnimations.CRAWL);
//                }
//            }
//            // 没有移动
//            if (!state.isMoving()){
//                // 是否为sit
//                if (this.isSitting()) return state.setAndContinue(RawAnimation.begin().thenLoop("misc.sit"));
//                return state.setAndContinue(DefaultAnimations.IDLE);
//            }else if (state.isMoving()){
//                // 如果速度较快
//                if (this.getDeltaMovement().length() > 0.2){
//                    return state.setAndContinue(DefaultAnimations.RUN);
//                }
//                return state.setAndContinue(DefaultAnimations.WALK);
//            }
//
//            return PlayState.CONTINUE;
//        }));
//    }

    @Override
    public void move(@NotNull MoverType type, @NotNull Vec3 pos) {
        if (this.canMove()) {
            super.move(type, pos);
        }
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        if (this.canMove()){
            super.moveTo(x, y, z, yRot, xRot);
        }
    }

    public boolean canMove() {
        return this.getPose() != Pose.SWIMMING && !this.isSitting();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!result) return false;
        if (source.getEntity() instanceof Player player){
            hurtByPlayer(player);
        }
        return true;
    }

    public void hurtByPlayer(Player player){
        sendHurtMessageToPlayer(player);
    }

    public void sendHurtMessageToPlayer(Player player){
        if (player instanceof ServerPlayer) {
            int r = random.nextInt(6);
            player.sendSystemMessage(Component.translatable("message.toneko.neko.on_hurt."+r, this.getName()));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> {
            // 地上趴着
            if (this.getPose() == Pose.SWIMMING && !this.isInWater()){
                return state.setAndContinue(DefaultAnimations.CRAWL);
            }
            // 在水里
            if (this.isInWater() && this.isEyeInFluid(FluidTags.WATER)){
                if (state.isMoving()) {
                    return state.setAndContinue(DefaultAnimations.SWIM);
                }else {
                    return state.setAndContinue(DefaultAnimations.CRAWL);
                }
            }
            // 没有移动
            if (!state.isMoving()){
                // 是否为sit
                if (this.isSitting()) return state.setAndContinue(RawAnimation.begin().thenLoop("misc.sit"));
                return state.setAndContinue(DefaultAnimations.IDLE);
            }else if (state.isMoving()){
                // 如果速度较快
                if (this.getDeltaMovement().length() > 0.2){
                    return state.setAndContinue(DefaultAnimations.RUN);
                }
                return state.setAndContinue(DefaultAnimations.WALK);
            }

            return PlayState.CONTINUE;
        }));
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

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        return true;
    }

    public ResourceLocation getTagKeyLocation(String type){
        ResourceLocation r = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        return r.withPath("neko/"+r.getPath()+"/"+type);
    }

    public static AttributeSupplier.Builder createNekoAttributes(){
        return createMobAttributes().add(Attributes.ATTACK_DAMAGE);
    }

    public boolean isSitting() {
        return isSitting;
    }
}
