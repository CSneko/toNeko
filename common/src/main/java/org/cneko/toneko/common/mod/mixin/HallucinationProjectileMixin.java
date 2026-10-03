package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.cneko.toneko.common.mod.effects.HallucinationEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Projectile.class)
public abstract class HallucinationProjectileMixin {
    @ModifyVariable(method = "shoot", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float toneko$disorientedAim(float inaccuracy) {
        var owner = ((Projectile) (Object) this).getOwner();
        return owner instanceof LivingEntity living ? HallucinationEffect.projectileInaccuracy(living, inaccuracy) : inaccuracy;
    }
}
