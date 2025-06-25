package org.cneko.toneko.common.mod.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
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

    @Inject(method = "stopRiding" , at = @At("HEAD"),cancellable = true)
    private void toneko$stopRiding(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof MoufletNekoBoss boss) {
            if (!boss.allowDismount(player)){
                // 需要消耗100能量挣脱
                if (player.getNekoEnergy() >= 100) {
                    player.setNekoEnergy(player.getNekoEnergy() - 100);
                } else {
                    // 能量不足，取消下车
                    ci.cancel();
                }
            }
        }
    }
}
