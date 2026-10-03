package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 姿势钉定的重申注入（仅非玩家生物：猫娘 NPC、被踩踏目标等）。
 *
 * 旧实现通过 @Inject(HEAD) 取消 Entity#setPose、并全局劫持 getPose，
 * 与原版每 tick 的姿势状态机对抗，导致潜行显示不一致、并破坏其它动作模组。
 * 现改为：让原版正常计算与同步姿势，仅在 aiStep 末尾把被钉定的姿势用普通
 * setPose 写回去 —— 写入直接更新 DATA_POSE，由原版负责同步与刷新碰撞箱。
 *
 * 注意：玩家不在此处理！Player.tick 在 super.tick()（即本钩子）之后还会执行
 * updatePlayerPose 重算姿势，会把这里的写入同 tick 覆盖掉。
 * 玩家统一在 PlayerEntityMixin 中注入 updatePlayerPose 的所有返回点，
 * 保证钉定是该 tick 的最后一次姿势写入。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "aiStep", at = @At("TAIL"))
    public void toneko$reapplyPinnedPose(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof net.minecraft.world.entity.player.Player) {
            return; // 玩家见 PlayerEntityMixin#toneko$reapplyPinnedPose
        }

        Pose pinned = EntityPoseManager.getNullablePose(self);
        if (pinned == null || self.isPassenger()) {
            return;
        }

        // 普通 vanilla 写入：DATA_POSE 自动同步，客户端自动 refreshDimensions
        self.setPose(pinned);
    }
}
