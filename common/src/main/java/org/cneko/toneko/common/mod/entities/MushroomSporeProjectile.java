package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.UUID;

/** A slow purple spore with real travel time and block collision. It cannot strike bystanders. */
public class MushroomSporeProjectile extends ThrowableProjectile {
    public static final DustParticleOptions PURPLE = new DustParticleOptions(0xB479E6, 0.8f);
    private UUID intendedVictim;
    private float damage = MushroomRules.BASE_SPORE_DAMAGE;

    public MushroomSporeProjectile(EntityType<? extends MushroomSporeProjectile> type, Level level) {
        super(type, level);
    }

    public MushroomSporeProjectile(ServerLevel level, MushroomGirlEntity owner, LivingEntity victim) {
        this(ToNekoEntities.MUSHROOM_SPORE, level);
        setOwner(owner);
        intendedVictim = victim.getUUID();
        damage = owner.getSporeDamage();
        setPos(owner.getX(), owner.getEyeY(), owner.getZ());
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected double getDefaultGravity() { return 0.005; }
    @Override protected boolean canHitEntity(Entity entity) {
        return intendedVictim != null && intendedVictim.equals(entity.getUUID()) && super.canHitEntity(entity);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                var trail = position().subtract(getDeltaMovement().scale(i / 3.0));
                level().addParticle(PURPLE, trail.x, trail.y, trail.z, 0, 0.015, 0);
            }
        } else if (tickCount > 60) discard();
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel server && hit.getEntity() instanceof LivingEntity victim
                && intendedVictim != null && intendedVictim.equals(victim.getUUID())) {
            Entity owner = getOwner();
            boolean damaged = victim.hurtServer(server, damageSources().thrown(this, owner), damage);
            if (damaged && owner instanceof MushroomGirlEntity mushroom) mushroom.onDefensiveSporeHit(victim);
            if (ToNekoEffects.HALLUCINATION != null) victim.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.HALLUCINATION), 200, 0), getOwner());
        }
    }

    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(PURPLE, getX(), getY(), getZ(), 9, 0.12, 0.12, 0.12, 0.02);
            discard();
        }
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (intendedVictim != null) out.putString("Victim", intendedVictim.toString());
        out.putFloat("Damage", damage);
    }

    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        try { intendedVictim = UUID.fromString(in.getStringOr("Victim", "")); }
        catch (IllegalArgumentException ignored) { intendedVictim = null; }
        float savedDamage = in.getFloatOr("Damage", MushroomRules.BASE_SPORE_DAMAGE);
        damage = Float.isFinite(savedDamage) ? Math.clamp(savedDamage, MushroomRules.BASE_SPORE_DAMAGE,
                MushroomRules.sporeDamage(MushroomRules.MAX_LEVEL)) : MushroomRules.BASE_SPORE_DAMAGE;
    }
}
