package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
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
import org.cneko.toneko.common.mod.entities.ai.goal.NekoPickupItemGoal;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.packets.interactives.NekoEntityInteractivePayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFollowOwnerGoal;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoMateGoal;
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

import java.util.Objects;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

public abstract class NekoEntity extends AgeableMob implements GeoEntity, INeko {
    public static double DEFAULT_FIND_RANGE = 16.0D;
    public static float DEFAULT_RIDE_RANGE = 3f;

    public NekoFollowOwnerGoal nekoFollowOwnerGoal;
    public NekoMateGoal nekoMateGoal;
    private final AnimatableInstanceCache cache;
    private boolean isSitting = false;
    private String skin;
    final NekoInventory inventory = new NekoInventory(this);

    public static final EntityDataAccessor<String> SKIN_DATA_ID = SynchedEntityData.defineId(NekoEntity.class, EntityDataSerializers.STRING);

    public NekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        NekoQuery.getNeko(this.getUUID()).setNeko(true);
        this.cache = GeckoLibUtil.createInstanceCache(this);
        randomize();
        this.skin = getSkin();
    }


    public void randomize(){
        // 设置名字（如果没有）
        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal(NekoNameRegistry.getRandomName()));
        }

        EntityUtil.randomizeAttributeValue(this, Attributes.SCALE,1,0.8,1.05); // 实体的体型为0.8~1.05间
        EntityUtil.randomizeAttributeValue(this, Attributes.MOVEMENT_SPEED,0.7,0.5,0.6); // 实体速度为0.5~0.6间
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_DATA_ID, "grmmy");
    }


    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Skin", this.getSkin());
        compound.put("Inventory", this.inventory.save(new ListTag()));
        compound.putInt("SelectedItemSlot", this.inventory.selected);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setSkin(compound.getString("Skin"));
        ListTag listTag = compound.getList("Inventory", 10);
        this.inventory.load(listTag);
        this.inventory.selected = compound.getInt("SelectedItemSlot");
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
        // 会尝试捡起附近的物品
        this.goalSelector.addGoal(5, new NekoPickupItemGoal(this));
        // 会被拿着喜欢物品的玩家吸引
        this.goalSelector.addGoal(10, new TemptGoal(this, 0.5D, this::isFavoriteItem,false));
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

    public String getSkin(){
        if (this.level().isClientSide()) {
            String s = this.entityData.get(SKIN_DATA_ID);
            if (s.isEmpty()){
                this.entityData.packDirty();
                s = this.getDefaultSkin();
            }
            return s;
        }
        if (this.skin == null || skin.isEmpty()) {
            skin = this.getRandomSkin();
            this.setSkin(skin);
            return skin;
        }
        return skin;
    }
    public void setSkin(String skin) {
        this.skin = skin;
        //noinspection ConstantValue
        if (this.entityData != null) {
            this.entityData.set(SKIN_DATA_ID, skin);
        }
    }
    public String getRandomSkin(){
        return NekoSkinRegistry.getRandomSkin(this.getType());
    }
    public String getDefaultSkin(){
        return "aquarter";
    }

    // 最喜欢的物品
    public boolean isFavoriteItem(ItemStack stack){
        Item item = stack.getItem();
        return item.equals(ToNekoItems.CATNIP);
    }
    // 是否喜欢这个物品
    public boolean isLikedItem(ItemStack stack){
        return isFavoriteItem(stack);
    }
    // 是否需要这个物品
    public boolean isNeededItem(ItemStack stack){
        return isLikedItem(stack);
    }

    public boolean giftItem(Player player, int slot){
        return giftItem(player, player.getInventory().getItem(slot));
    }
    // 赠送物品
    public boolean giftItem(Player player, ItemStack stack){
        this.drop(stack, true);
        // 如果是喜欢的物品
        if (this.isLikedItem(stack)){
            player.getInventory().removeItem(player.getMainHandItem());
            this.getInventory().add(stack);
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

    public NekoInventory getInventory() {
        return this.inventory;
    }

    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inventory.getSelected();
        } else if (slot == EquipmentSlot.OFFHAND) {
            return this.inventory.offhand.getFirst();
        } else {
            return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
        }
    }

    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        this.verifyEquippedItem(stack);
        if (slot == EquipmentSlot.MAINHAND) {
            this.onEquipItem(slot, this.inventory.items.set(this.inventory.selected, stack), stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.onEquipItem(slot, this.inventory.offhand.set(0, stack), stack);
        } else if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            this.onEquipItem(slot, this.inventory.armor.set(slot.getIndex(), stack), stack);
        }

    }

    public @NotNull ItemStack getItemInHand() {
        return this.inventory.getSelected();
    }

    public void selectItem(ItemStack stack) {
        this.inventory.selected = this.inventory.findSlotMatchingItem(stack);
    }

    public boolean addItem(ItemStack stack) {
        return this.inventory.add(stack);
    }
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return this.inventory.armor;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        Level world = this.level();
        if (world instanceof ServerLevel) {
//            ServerLevel serverLevel = (ServerLevel) world;
//            this.dropAllDeathLoot(serverLevel, damageSource);
//            this.getInventory().dropAll();
            Entity entity = damageSource.getEntity();
            if (entity != null){
                // 发送消息，告诉玩家过于罪恶了（我才不会告诉你是因为有Bug呢）
                entity.sendSystemMessage(Component.translatable("message.toneko.neko.die.no_item_drop"));
            }
        }
    }

    @Nullable
    public ItemEntity drop(ItemStack itemStack, boolean includeThrowerName) {
        return this.drop(itemStack, false, includeThrowerName);
    }

    @Nullable
    public ItemEntity drop(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName) {
        if (droppedItem.isEmpty()) {
            return null;
        } else {
            if (this.level().isClientSide) {
                this.swing(InteractionHand.MAIN_HAND);
            }

            double d = this.getEyeY() - 0.30000001192092896;
            ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), d, this.getZ(), droppedItem);
            itemEntity.setPickUpDelay(40);
            if (includeThrowerName) {
                itemEntity.setThrower(this);
            }

            float f;
            float g;
            if (dropAround) {
                f = this.random.nextFloat() * 0.5F;
                g = this.random.nextFloat() * 6.2831855F;
                itemEntity.setDeltaMovement(-Mth.sin(g) * f, 0.20000000298023224, Mth.cos(g) * f);
            } else {
                g = Mth.sin(this.getXRot() * 0.017453292F);
                float h = Mth.cos(this.getXRot() * 0.017453292F);
                float i = Mth.sin(this.getYRot() * 0.017453292F);
                float j = Mth.cos(this.getYRot() * 0.017453292F);
                float k = this.random.nextFloat() * 6.2831855F;
                float l = 0.02F * this.random.nextFloat();
                itemEntity.setDeltaMovement((double)(-i * h * 0.3F) + Math.cos(k) * (double)l, -g * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F, (double)(j * h * 0.3F) + Math.sin(k) * (double)l);
            }

            return itemEntity;
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
        ServerPlayNetworking.send(player, new NekoEntityInteractivePayload(this.getUUID().toString()));
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
    public void tick() {
        super.tick();
        this.inventory.tick();
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
    public boolean isNeko() {
        return true;
    }

    @Override
    public boolean checkSpawnRules(@NotNull LevelAccessor level, @NotNull MobSpawnType reason) {
        return true;
    }


    public static AttributeSupplier.Builder createNekoAttributes(){
        return createMobAttributes().add(Attributes.ATTACK_DAMAGE);
    }

    public boolean isSitting() {
        return isSitting;
    }
}
