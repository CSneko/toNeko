package org.cneko.gal.common.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.cneko.gal.common.util.pack.ExternalPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @Inject(at = @At("RETURN"), method = "getResource", cancellable = true)
    public void getResource(ResourceLocation resourceLocation, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (ExternalPack.containsResource(resourceLocation)){
            cir.setReturnValue(Optional.of(new Resource(
                    new ExternalPack(ExternalPack.LOCATION_INFO),
                    ExternalPack.getResourceStatic(PackType.CLIENT_RESOURCES,resourceLocation)
            )));
        }
    }

    @Inject(at = @At("RETURN"), method = "getNamespaces", cancellable = true)
    public void getNamespaces(CallbackInfoReturnable<Set<String>> cir) {
        var set = new HashSet<>(cir.getReturnValue());
        set.add("gals");
        cir.setReturnValue(set);
    }

    @Inject(at = @At("RETURN"), method = "getResourceStack", cancellable = true)
    public void getResourceStack(ResourceLocation resourceLocation, CallbackInfoReturnable<List<Resource>> cir) {
        if (resourceLocation.getPath().equals("sounds.json") && resourceLocation.getNamespace().equals("gals")){
            if (ExternalPack.containsSoundsJson()){
                List<Resource> resourceList = new ArrayList<>(cir.getReturnValue());
                resourceList.add(new Resource(
                        new ExternalPack(ExternalPack.LOCATION_INFO),
                        ExternalPack.getResourceStatic(PackType.CLIENT_RESOURCES,resourceLocation))
                );
                cir.setReturnValue(resourceList);
            }
        }
    }
}
