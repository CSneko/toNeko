package org.cneko.toneko.common.mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class BedRestBodyMixin {
    // The first-person entity filter normally draws the camera player only during
    // native sleep. Keep drawing their body while they choose to remain in bed.
    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private boolean toneko$showRestingBody(LivingEntity entity, Operation<Boolean> original) {
        return original.call(entity) || entity instanceof Player player
                && MushroomBedRest.restingBed(player).isPresent();
    }
}
