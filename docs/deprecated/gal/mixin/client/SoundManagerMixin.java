package org.cneko.gal.common.mixin.client;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

import static org.cneko.gal.common.util.pack.ExternalPack.NAMESPACED_SOUNDS_JSON;
import static org.cneko.gal.common.util.pack.ExternalPack.soundsJsonLock;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Final
    @Shadow
    private Map<ResourceLocation, WeighedSoundEvents> registry;

    @Inject(method = "apply*", at = @At("HEAD"))
    private void onApply(SoundManager.Preparations object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        // 确保动态声音事件被注册
        soundsJsonLock.readLock().lock();
        try {
            NAMESPACED_SOUNDS_JSON.forEach((namespace, soundsJson) -> {
                soundsJson.entrySet().forEach(entry -> {
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, entry.getKey());
                    if (!registry.containsKey(location)) {
                        // 创建一个空的WeighedSoundEvents，它会被SoundManager填充
                        registry.put(location, new WeighedSoundEvents(location, null));
                    }
                });
            });
        } finally {
            soundsJsonLock.readLock().unlock();
        }
    }
}