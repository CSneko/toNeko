package org.cneko.toneko.common.mod.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void toneko$restoreFrom(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        INeko newNeko = (INeko) this;

        newNeko.setNeko(oldPlayer.isNeko());
        newNeko.setNekoLevel(oldPlayer.getNekoLevel());
        newNeko.setNekoEnergy(oldPlayer.getNekoEnergy());
        newNeko.setNickName(oldPlayer.getNickName());

        newNeko.getOwners().clear();
        newNeko.getOwners().putAll(oldPlayer.getOwners());

        newNeko.getBlockedWords().clear();
        newNeko.getBlockedWords().addAll(oldPlayer.getBlockedWords());

        newNeko.getQuirks().clear();
        newNeko.getQuirks().addAll(oldPlayer.getQuirks());
    }
}
