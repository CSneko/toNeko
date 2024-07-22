package org.cneko.toneko.fabric.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.cneko.toneko.fabric.events.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnreachableCode")
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(at = @At("HEAD"), method = "setPose", cancellable = true)
    public void setPose(EntityPose pose, CallbackInfo info){
        if((Object) this instanceof PlayerEntity player) {
            // 如果玩家是lying状态且pose不是SLEEPING,则取消执行
            if (PlayerTickEvent.lyingPlayers.contains(player)) {
                if (pose != EntityPose.SLEEPING) {
                    info.cancel();
                }
            }
        }
    }
}
