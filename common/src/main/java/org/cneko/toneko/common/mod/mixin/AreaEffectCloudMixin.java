package org.cneko.toneko.common.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.items.CatnipBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 滞留型猫薄荷水的云也只给玩家挂「猫薄荷香气」。
 *
 * <p>{@code AreaEffectCloud#serverTick} 每 tick 对范围内生物调用 {@code addEffect}，
 * 这里同样只过滤猫薄荷香气，其它效果不受影响。
 */
@Mixin(AreaEffectCloud.class)
public class AreaEffectCloudMixin {

    @WrapOperation(
            method = "serverTick",
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
