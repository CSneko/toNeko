package org.cneko.toneko.common.mod.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.items.BazookaItem;
import org.cneko.toneko.common.mod.misc.ToNekoSoundEvents;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AmmunitionEntity extends ThrowableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<ItemStack> BAZOOKA_STACK = SynchedEntityData.defineId(AmmunitionEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> AMMUNITION_STACK = SynchedEntityData.defineId(AmmunitionEntity.class, EntityDataSerializers.ITEM_STACK);
    public Vec3 initialPosition;

    public AmmunitionEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        this.initialPosition = this.position();

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BAZOOKA_STACK, ItemStack.EMPTY);
        builder.define(AMMUNITION_STACK, ItemStack.EMPTY);
    }

    public void setBazookaStack(ItemStack stack) {
        this.getEntityData().set(BAZOOKA_STACK, stack.copy());
    }

    public ItemStack getBazookaStack() {
        return this.getEntityData().get(BAZOOKA_STACK);
    }

    public void setAmmunitionStack(ItemStack stack) {
        this.getEntityData().set(AMMUNITION_STACK, stack.copy());
    }

    public ItemStack getAmmunitionStack() {
        return this.getEntityData().get(AMMUNITION_STACK);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Bazooka", getBazookaStack().save(this.registryAccess()));
        tag.put("Ammunition", getAmmunitionStack().save(this.registryAccess()));
        tag.putDouble("InitialX", initialPosition.x);
        tag.putDouble("InitialY", initialPosition.y);
        tag.putDouble("InitialZ", initialPosition.z);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBazookaStack(ItemStack.parseOptional(registryAccess(),tag.getCompound("Bazooka")));
        setAmmunitionStack(ItemStack.parseOptional(registryAccess(),tag.getCompound("Ammunition")));
        this.initialPosition = new Vec3(
                tag.getDouble("InitialX"),
                tag.getDouble("InitialY"),
                tag.getDouble("InitialZ")
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.initialPosition == null) this.initialPosition = this.position();

            ItemStack ammoStack = getAmmunitionStack();
            if (ammoStack.getItem() instanceof BazookaItem.Ammunition ammo) {
                // 检查最大距离
                float maxDistance = ammo.getMaxDistance(getBazookaStack(), ammoStack);
                if (this.position().distanceTo(initialPosition) >= maxDistance) {
                    handleAirHit();
                    this.discard();
                }
            }
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (reason == RemovalReason.DISCARDED){
            if (!level().isClientSide) {
                // 清除时播放喵叫
                this.level().playSound(
                        this,
                        this.blockPosition(),
                        ToNekoSoundEvents.BAZOOKA_MEOW,
                        SoundSource.AMBIENT,
                        1.0f,
                        1.0f
                );
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity shooter = getOwner();
        ItemStack ammoStack = getAmmunitionStack();

        if (shooter instanceof LivingEntity livingShooter &&
                result.getEntity() instanceof LivingEntity target &&
                ammoStack.getItem() instanceof BazookaItem.Ammunition ammo) {

            ammo.hitOnEntity(livingShooter, target, getBazookaStack(), ammoStack);


            // 如果有害，则产生仇恨
            if (ammo.isHarmful(getBazookaStack(), ammoStack)){
                target.setLastHurtByMob(livingShooter);
            }
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        Entity shooter = getOwner();
        ItemStack ammoStack = getAmmunitionStack();

        if (shooter instanceof LivingEntity livingShooter &&
                ammoStack.getItem() instanceof BazookaItem.Ammunition ammo) {

            ammo.hitOnBlock(livingShooter, result.getBlockPos(), getBazookaStack(), ammoStack);
        }

        this.discard();
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        super.onHit(hitResult);
        // 处理碰撞逻辑
        if (!this.level().isClientSide) {
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                this.onHitEntity((EntityHitResult) hitResult);
            } else if (hitResult.getType() == HitResult.Type.BLOCK) {
                this.onHitBlock((BlockHitResult) hitResult);
            }
            this.discard();
        }
    }

    private void handleAirHit() {
        Entity shooter = getOwner();
        ItemStack ammoStack = getAmmunitionStack();

        if (shooter instanceof LivingEntity livingShooter &&
                ammoStack.getItem() instanceof BazookaItem.Ammunition ammo) {

            ammo.hitOnAir(livingShooter, this.blockPosition(), getBazookaStack(), ammoStack);
        }
    }

    // 发射时设置初始位置
    public void shootWithInitialPos(double x, double y, double z, float speed, float inaccuracy) {
        this.initialPosition = this.position();

        // 计算方向向量（与原版 Arrow 一致）
        Vec3 direction = new Vec3(x, y, z).normalize();

        // 调用父类 shoot 方法设置基础物理参数
        super.shoot(
                direction.x,
                direction.y,
                direction.z,
                speed,    // 速度标量
                inaccuracy // 散布系数
        );
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}