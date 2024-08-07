package org.cneko.toneko.fabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.fabric.api.PlayerPoseAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnreachableCode")
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(at = @At("HEAD"), method = "setPose", cancellable = true)
    public void setPose(Pose pose, CallbackInfo info){
        if((Object) this instanceof Player player) {
            // 如果玩家存在设置的姿态，则取消设置姿态
            if (PlayerPoseAPI.contains(player)){
                if(PlayerPoseAPI.getPose(player) != pose) {
                    info.cancel();
                }
            }
        }
    }
}
