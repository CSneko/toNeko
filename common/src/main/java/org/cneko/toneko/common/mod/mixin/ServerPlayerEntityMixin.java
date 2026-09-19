package org.cneko.toneko.common.mod.mixin;

import net.minecraft.server.level.ServerPlayer;
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

        newNeko.setNeko(((INeko) oldPlayer).isNeko());
        newNeko.setNekoAge(((INeko) oldPlayer).getNekoAge());
        newNeko.setNekoLevelFactorData(((INeko) oldPlayer).getNekoLevelFactorData());
        newNeko.setNekoEnergy(((INeko) oldPlayer).getNekoEnergy());
        newNeko.setNickName(((INeko) oldPlayer).getNickName());

        newNeko.getOwners().clear();
        newNeko.getOwners().putAll(((INeko) oldPlayer).getOwners());

        newNeko.getBlockedWords().clear();
        newNeko.getBlockedWords().addAll(((INeko) oldPlayer).getBlockedWords());

        newNeko.getQuirks().clear();
        newNeko.getQuirks().addAll(((INeko) oldPlayer).getQuirks());

        newNeko.getVisitedBiomes().clear();
        newNeko.getVisitedBiomes().addAll(((INeko) oldPlayer).getVisitedBiomes());

        // 手册发放标记也要跟着走，否则死亡/换维度后会被当成“新玩家”再送一本
        newNeko.setReceivedGuideBook(((INeko) oldPlayer).hasReceivedGuideBook());
    }

}
