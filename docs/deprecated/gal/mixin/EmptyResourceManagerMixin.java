package org.cneko.gal.common.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.cneko.gal.common.util.pack.ExternalPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ResourceManager.Empty.class)
public class EmptyResourceManagerMixin {
    @Inject(at = @At("RETURN"), method = "getResource", cancellable = true)
    public void getResource(ResourceLocation resourceLocation, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (ExternalPack.containsResource(resourceLocation)){
            cir.setReturnValue(Optional.of(new Resource(
                    new ExternalPack(ExternalPack.LOCATION_INFO),
                    ExternalPack.getResourceStatic(PackType.CLIENT_RESOURCES,resourceLocation)
            )));
        }
    }
}
