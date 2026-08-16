package org.cneko.toneko.common.mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.cneko.toneko.common.mod.client.api.StompAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称踩踏时调整玩家模型可见性：隐藏头/身体/手臂（避免遮挡视线），保留腿。
 * <p>
 * 由于 {@link StompFirstPersonPassMixin} 已关闭 firstPersonPass，PlayerAnimator 的
 * hideBonesInFirstPerson 不再隐藏部件，因此这里手动隐藏遮挡部件。可见性每帧会被
 * PlayerRenderer.setModelPose 重置为 true，无残留问题。
 */
@Mixin(PlayerRenderer.class)
public abstract class StompFirstPersonRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void toneko$adjustBodyForStomp(AbstractClientPlayer player, float yaw, float tickDelta,
                                           PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        // 仅本地玩家第一人称踩踏时调整
        if (player != Minecraft.getInstance().getCameraEntity()) return;
        if (!StompAnimations.isStomping(player)) return;
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) return;

        PlayerModel<?> model = ((PlayerRenderer) (Object) this).getModel();
        // 隐藏头/身体/手臂（避免遮挡视线），腿保持可见
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.jacket.visible = false;
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightSleeve.visible = false;
        model.leftSleeve.visible = false;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;
    }
}
