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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.interactives.NekoEntityInteractivePayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.fabric.entities.ai.goal.NekoFollowOwnerGoal;
import org.cneko.toneko.fabric.entities.ai.goal.NekoMateGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

public abstract class NekoEntity extends PathfinderMob implements GeoEntity, INeko {
    public static double DEFAULT_FIND_RANGE = 16.0D;
    public static float DEFAULT_RIDE_RANGE = 3f;

    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    public NekoMateGoal nekoMateGoal;
    private final AnimatableInstanceCache cache;
    private String skin = "";
    private boolean isSitting = false;
    private int age = 0;
    private boolean isBaby = false;


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
        this.setAge(nbt.getInt("Age"));
        this.setBaby(nbt.getBoolean("Baby"));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        if (!getSkin().isEmpty()) {
            nbt.putString("Skin", getSkin());
        }
        nbt.putInt("Age", this.getAge());
        nbt.putBoolean("Baby", this.isBaby());
        super.addAdditionalSaveData(nbt);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        setSkin(nbt.getString("Skin"));
        if (getSkin().isEmpty()) {
            setSkin(getRandomSkin());
        }
        // 设置名字（如果没有）
        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
        }

        EntityUtil.randomizeAttributeValue(this, Attributes.SCALE,1,0.8,1.05); // 实体的体型为0.8~1.05间
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
        return skin;
    }
    public void setSkin(String skin) {
        this.skin = skin;
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
        return other.getNeko().isNeko();
    }

    public void breed(ServerLevel level, INeko mate) {
        if (this.canMate(mate)) {
            // 冒爱心
            level.addParticle(ParticleTypes.HEART, this.getX(), this.getY(), this.getZ(), 1, 1, 1);
            Packet<?> packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART,true, this.getX(), this.getY(), this.getZ(),1,1,1,0.2f,10);
            if (mate instanceof ServerPlayer sp){
                sp.connection.send(packet);
            }
            this.spawnChildFromBreeding(level, mate);
        }
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
    public void finalizeSpawnChildFromBreeding(ServerLevel level, INeko animal, NekoEntity child) {
        // 成长需要20*60*60ticks
        child.setAge(-72000);
        child.load(new CompoundTag());
    }
    @Nullable
    public abstract NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent);

    public int getAge() {
        return this.age;
    }
    public int setAge(int age) {
        return this.age = age;
    }
    public void addAge(int age) {
        this.age += age;
        if (this.age == 0) {
            this.onGrowUp();
        }
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(baby);
        this.setAge(-72000);
    }

    @Override
    public boolean isBaby() {
        return this.getAge() < 0;
    }

    public void onGrowUp() {
        this.setBaby(false);
    }
    @Override
    public void tick() {
        super.tick();
        if (this.getAge() < 0) {
            this.addAge(1);
        }
    }

    // 当玩家右键
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // shift+右键打开互动菜单
        if (hand.equals(InteractionHand.MAIN_HAND) && player.isShiftKeyDown() && player instanceof ServerPlayer sp){
            ServerPlayNetworking.send(sp, new NekoEntityInteractivePayload(this.getUUID().toString()));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> {
            // 地上趴着
            if (this.getPose() == Pose.SWIMMING && !this.isInLiquid()){
                return state.setAndContinue(DefaultAnimations.CRAWL);
            }
            // 在水里
            if (this.isInLiquid() && this.isEyeInFluid(FluidTags.WATER)){
                return state.setAndContinue(DefaultAnimations.SWIM);
            }
            // 没有移动
            if (!state.isMoving()){
                // 是否为sit
                if (this.isSitting()) return state.setAndContinue(RawAnimation.begin().thenLoop("misc.sit"));
                return state.setAndContinue(DefaultAnimations.IDLE);
            }else if (state.isMoving()){
                return state.setAndContinue(DefaultAnimations.WALK);
            }

            return PlayState.CONTINUE;
        }));
    }

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
        return this.getPose() != Pose.SWIMMING && !this.isSitting() || this.isInLiquid();
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
