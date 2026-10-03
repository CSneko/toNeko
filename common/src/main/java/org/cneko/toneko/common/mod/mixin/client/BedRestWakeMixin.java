package org.cneko.toneko.common.mod.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply awake bed rest immediately when its synchronized marker arrives. */
@Mixin(ClientPacketListener.class)
public abstract class BedRestWakeMixin {
    @Shadow private ClientLevel level;

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void toneko$finishBedWake(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        if (level.getEntity(packet.id()) instanceof Player player) MushroomBedRest.tick(player);
    }
}
