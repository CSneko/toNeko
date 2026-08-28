package org.cneko.toneko.common.mod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * 实体移除（死亡/区块卸载/换维度/重生）时清除姿势钉定：
     * 防止旧实体实例残留强引用（泄漏）与“幽灵姿势”。
     * 玩家断线不经过此方法，但 EntityPoseManager 为 WeakHashMap，
     * 实体失去引用后条目自动消失，双重保险。
     */
    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("TAIL"))
    public void toneko$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof ServerPlayer sp && !sp.hasDisconnected()) {
            // 连接仍在（死亡/换维度）：通知本人客户端解除本地预测钉定
            Pose prev = EntityPoseManager.getNullablePose(sp);
            if (prev != null) {
                EntityPoseManager.remove(sp);
                ServerPlayNetworking.send(sp, new EntityPosePayload(prev, "self", false));
            }
            return;
        }
        EntityPoseManager.remove(entity);
    }

    /**
     * 骑乘 Boss（MoufletNekoBoss）下车拦截：能量不足时禁止下车。
     * 26.x 新版 Mixin 不再对裸名选择器解析超类方法（旧版可以），
     * ServerPlayer 未覆写 stopRiding，因此把注入下沉到声明类 Entity，
     * 处理器内自行 instanceof 过滤，语义与原先「仅服务端玩家」一致。
     */
    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    public void toneko$stopRiding(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof ServerPlayer player)) return;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof MoufletNekoBoss boss) {
            if (!boss.allowDismount(player)) {
                // 需要消耗100能量挣脱
                if (((INeko) player).getNekoEnergy() >= 100) {
                    ((INeko) player).setNekoEnergy(((INeko) player).getNekoEnergy() - 100);
                } else {
                    // 能量不足，取消下车
                    ci.cancel();
                }
            }
        }
    }
}
