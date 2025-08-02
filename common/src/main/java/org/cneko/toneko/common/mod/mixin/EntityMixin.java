package org.cneko.toneko.common.mod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.animatable.GeoEntity;

@SuppressWarnings("UnreachableCode")
@Mixin(Entity.class)
public abstract class EntityMixin{

    @Unique
    public int toneko$slowTick = 0;

    @Inject(at = @At("HEAD"), method = "setPose", cancellable = true)
    public void setPose(Pose pose, CallbackInfo info){
        Entity entity = (Entity) (Object) this;
        // 如果实体存在设置的姿态，则取消设置姿态
        if (!entity.level().isClientSide) {
            if (EntityPoseManager.contains(entity)) {
                var entityPose = EntityPoseManager.getPose(entity);
                if (entityPose != pose) {
                    entity.setPose(entityPose);
                    info.cancel();
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "getPose", cancellable = true)
    public void getPose(CallbackInfoReturnable<Pose> cir){
        Entity entity = (Entity) (Object) this;
        if (!entity.level().isClientSide) {
            if (EntityPoseManager.contains(entity)) {
                cir.setReturnValue(EntityPoseManager.getPose(entity));
            }
        }else {
            if (ClientEntityPoseManager.contains(entity)) {
                cir.setReturnValue(ClientEntityPoseManager.getPose(entity));
            }
        }

    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info){
        toneko$slowTick++;
        if (toneko$slowTick >= 2) {
            toneko$slowTick = 0;
            var entity = (Entity)(Object)this;
            if (!entity.level().isClientSide) {
                // 如果周围有其它玩家，则发送给周围的所有玩家
                var pose = EntityPoseManager.getPose(entity);
                boolean status;
                if (pose == null){
                    status = false;
                    pose = Pose.STANDING;
                }else {
                    status = true;
                }
                var players = EntityUtil.getPlayersInRange(entity, entity.level(), 16);
                for (Player player : players) {
                    ServerPlayNetworking.send((ServerPlayer) player, new EntityPosePayload(pose, entity.getUUID().toString(), status));
                }
            }
        }

    }
}
