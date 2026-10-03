package org.cneko.toneko.common.mod.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 1100)
public abstract class BedRestCameraMixin {
    @Shadow public abstract Entity entity();
    @Shadow public abstract boolean isDetached();
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract Vec3 position();
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void toneko$awakeBedView(float partialTick, CallbackInfo ci) {
        if (isDetached() || !(entity() instanceof Player player)
                || MushroomBedRest.bed(player).isEmpty()) return;
        var direction = MushroomBedRest.direction(player);
        if (direction == null) return;
        // Camera.update builds its view matrices and culling frustum AFTER alignment.
        // Changing this at update's tail leaves the frustum facing the player's mouse aim.
        // Native sleep already adds this lift. Use world up for awake rest so looking
        // up/down doesn't orbit the camera through the player's body or mattress.
        if (!player.isSleeping()) setPosition(position().add(0, 0.3, 0));
        setRotation(direction.toYRot() - 180f, player.getViewXRot(partialTick));
    }
}
