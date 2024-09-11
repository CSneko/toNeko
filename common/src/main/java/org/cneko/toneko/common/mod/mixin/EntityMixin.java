package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoEntity;

@SuppressWarnings("UnreachableCode")
@Mixin(Entity.class)
public abstract class EntityMixin{

    @Inject(at = @At("HEAD"), method = "setPose", cancellable = true)
    public void setPose(Pose pose, CallbackInfo info){
        Entity entity = (Entity) (Object) this;
        // 如果实体存在设置的姿态，则取消设置姿态
        if (EntityPoseManager.contains(entity)){
            if(EntityPoseManager.getNullablePose(entity) != pose) {
                info.cancel();
            }
        }
    }
}
