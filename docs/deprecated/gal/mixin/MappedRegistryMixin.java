package org.cneko.gal.common.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin {
    @Inject(method = "validateWrite()V", at = @At("HEAD"),cancellable = true)
    public void validateWrite(CallbackInfo ci) {
        ci.cancel();
    }
    @Inject(method = "validateWrite(Lnet/minecraft/resources/ResourceKey;)V", at = @At("HEAD"),cancellable = true)
    public void validateWrite(ResourceKey<?> key, CallbackInfo ci) {
        ci.cancel();
    }
}
