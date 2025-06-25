package org.cneko.toneko.common.mod.entities;

import lombok.Setter;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(AmmunitionEntity.class, EntityDataSerializers.BOOLEAN);
    public Vec3 initialPosition;
    @Setter
    private LivingEntity homingTarget = null;

    public AmmunitionEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        this.initialPosition = this.position();

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BAZOOKA_STACK, ItemStack.EMPTY);
        builder.define(AMMUNITION_STACK, ItemStack.EMPTY);
        builder.define(RETURNING, false);
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
            ItemStack bazookaStack = getBazookaStack();
            Entity shooter = getOwner();

            if (ammoStack.getItem() instanceof BazookaItem.Ammunition ammo) {
                float maxDistance = ammo.getMaxDistance(bazookaStack, ammoStack);

                int loyalty = EnchantmentHelper.getItemEnchantmentLevel(
                        this.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(lookup -> lookup.get(Enchantments.LOYALTY)).get()
                        , bazookaStack);

                if (loyalty > 0 && shooter != null) {
                    // 返还距离和速度
                    float[] returnPercents = {0.6f, 0.4f, 0.2f};
                    float[] speeds = {0.8f, 1.0f, 1.2f};
                    float returnDistance = maxDistance * returnPercents[Math.min(loyalty, 3) - 1];
                    float returnSpeed = speeds[Math.min(loyalty, 3) - 1];

                    double dist = this.position().distanceTo(initialPosition);
                    if (dist >= returnDistance && !this.getEntityData().get(RETURNING)) {
                        // 标记为返还
                        this.getEntityData().set(RETURNING, true);
                    }

                    if (this.getEntityData().get(RETURNING)) {
                        // 朝发射者飞行
                        Vec3 toShooter = shooter.position().add(0, shooter.getBbHeight() * 0.5, 0).subtract(this.position());
                        Vec3 motion = toShooter.normalize().scale(returnSpeed);
                        this.setDeltaMovement(motion);
                        this.hasImpulse = true;

                        // 检查是否击中发射者
                        if (this.getBoundingBox().intersects(shooter.getBoundingBox())) {
                            this.onHitEntity(new EntityHitResult(shooter));
                            this.discard();
                        }
                    } else if (dist >= maxDistance) {
                        handleAirHit();
                        this.discard();
                    }
                } else {
                    if (this.position().distanceTo(initialPosition) >= maxDistance) {
                        handleAirHit();
                        this.discard();
                    }
                }
            }

            // 追踪目标逻辑
            if (homingTarget != null && homingTarget.isAlive()) {
                Vec3 toTarget = homingTarget.position().add(0, homingTarget.getBbHeight() * 0.5, 0).subtract(this.position());
                Vec3 motion = toTarget.normalize().scale(1.5); // 速度可调整
                this.setDeltaMovement(motion);
                this.hasImpulse = true;
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