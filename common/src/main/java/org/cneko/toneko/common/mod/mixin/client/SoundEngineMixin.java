package org.cneko.toneko.common.mod.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin{
    @Inject(at = @At("RETURN"), method = "calculatePitch", cancellable = true)
    public void calculatePitch(SoundInstance sound, CallbackInfoReturnable<Float> cir){
        cir.setReturnValue(sound.getPitch());
    }
}
