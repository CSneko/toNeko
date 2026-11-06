package org.cneko.toneko.common.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.cneko.toneko.common.mod.items.CatnipBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 喷溅型猫薄荷水只给玩家挂「猫薄荷香气」，不挂给生物。
 *
 * <p>原版 {@code ThrownSplashPotion#onHitAsPotion} 会对范围内所有 {@link LivingEntity} 调用
 * {@code addEffect}，这里把这次调用包一层：只有猫薄荷香气会被过滤，其它药水保持原行为。
 */
@Mixin(ThrownSplashPotion.class)
public class ThrownSplashPotionMixin {

    @WrapOperation(
            method = "onHitAsPotion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean toneko$onlyApplyAromaToPlayers(LivingEntity entity, MobEffectInstance effect, Entity source,
                                                   Operation<Boolean> original) {
        if (CatnipBrewing.isAroma(effect) && !(entity instanceof Player)) {
            return false;
        }
        return original.call(entity, effect, source);
    }
}
