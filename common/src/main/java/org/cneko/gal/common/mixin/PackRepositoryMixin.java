package org.cneko.gal.common.mixin;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.cneko.gal.common.util.pack.ExternalPackSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onPackRepositoryConstruct(RepositorySource[] initialSources, CallbackInfo ci) {
        if (!(this.sources instanceof LinkedHashSet)) {
            this.sources = new LinkedHashSet<>(this.sources);
        }

        this.sources.add(ExternalPackSource.INSTANCE);
        // System.out.println("GAL: Injected ExternalPackSource into PackRepository.");
    }
}
