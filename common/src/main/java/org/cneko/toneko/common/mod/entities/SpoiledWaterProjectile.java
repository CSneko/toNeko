package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.abilities.ScentAreaHandler;
import org.cneko.toneko.common.mod.items.SpoiledWaterThrowableItem;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 变质水投掷物：落地/命中后生成一片气味云（ScentAreaHandler）。
 * 喷溅型持续时间短，滞留型持续时间长。
 */
public class SpoiledWaterProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(SpoiledWaterProjectile.class, EntityDataSerializers.ITEM_STACK);

    public SpoiledWaterProjectile(EntityType<? extends SpoiledWaterProjectile> type, Level level) {
        super(type, level);
    }

    public SpoiledWaterProjectile(Level level, LivingEntity shooter) {
        super(ToNekoEntities.SPOILED_WATER_PROJECTILE_ENTITY, shooter, level);
    }

    public SpoiledWaterProjectile(Level level, double x, double y, double z) {
        super(ToNekoEntities.SPOILED_WATER_PROJECTILE_ENTITY, x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
    }

    public void setItem(ItemStack stack) {
        this.getEntityData().set(ITEM, stack.copy());
    }

    public ItemStack getItem() {
        return this.getEntityData().get(ITEM);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FALLING_WATER,
                    this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            createScentArea(result.getLocation().x, result.getLocation().y, result.getLocation().z);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            createScentArea(result.getLocation().x, result.getLocation().y, result.getLocation().z);
        }
    }

    private void createScentArea(double x, double y, double z) {
        ItemStack stack = getItem();
        int spoilage = ScentedWaterUtil.getSpoilage(stack);
        String wearer = ScentedWaterUtil.getWearer(stack);
        boolean lingering = stack.getItem() instanceof SpoiledWaterThrowableItem item && item.isLingering();
        ScentAreaHandler.createArea((ServerLevel) this.level(),
                this.blockPosition(), spoilage, wearer, lingering);
        this.level().playSound(null, x, y, z,
                SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 0.6f, 1.0f);
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Item", getItem().save(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setItem(ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Item")));
    }
}
