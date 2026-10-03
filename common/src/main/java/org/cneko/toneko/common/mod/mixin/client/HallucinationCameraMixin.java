package org.cneko.toneko.common.mod.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.cneko.toneko.common.mod.client.events.HallucinationOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class HallucinationCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void toneko$hallucinationSway(float partialTick, CallbackInfo ci) {
        if (!HallucinationOverlay.active()) return;
        var player = Minecraft.getInstance().player;
        double time = player.tickCount + partialTick;
        // Camera-only oscillation keeps the player's server-side aim and movement intact.
        setRotation(yRot() + (float) Math.sin(time * 0.06) * 0.35f,
                xRot() + (float) Math.cos(time * 0.045) * 0.22f);
    }
}
