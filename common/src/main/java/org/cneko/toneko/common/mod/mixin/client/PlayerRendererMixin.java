package org.cneko.toneko.common.mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.util.ConfigUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Unique
    private static final float BASE_HEAD_SCALE = 1.0f;

    @Inject(
        method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void beforeRender(AbstractClientPlayer player, float f, float g, PoseStack poseStack,
                              MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (!ConfigUtil.isLoliHeadEnabled()) return;
        // 只有猫娘才触发萝莉头效果
        if (!(player instanceof INeko neko) || !neko.isNeko()) return;

        // 根据猫娘年龄计算身体缩放比例
        double ageScale = neko.getNekoAgeScale();
        if (ageScale >= 1.0f) return; // 成年猫娘不需要处理

        poseStack.pushPose();
        PlayerModel<?> model = ((PlayerRenderer) (Object) this).getModel();

        if (ConfigUtil.isLoliHeadAlgorithmEnabled()) {
            // 动态算法模式：身体越小，头部补偿越大
            float headScale = (float) (BASE_HEAD_SCALE / ageScale * ConfigUtil.getLoliHeadAlgorithmRatio());
            applyHeadScale(model, headScale);
            poseStack.scale((float) ageScale, (float) ageScale, (float) ageScale);
        } else {
            // 自定义缩放模式：使用配置的固定头部大小
            applyHeadScale(model,
                ConfigUtil.getLoliHeadCustomXScale(),
                ConfigUtil.getLoliHeadCustomYScale(),
                ConfigUtil.getLoliHeadCustomZScale());
            poseStack.scale((float) ageScale, (float) ageScale, (float) ageScale);
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("TAIL")
    )
    private void afterRender(AbstractClientPlayer player, float f, float g, PoseStack poseStack,
                             MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (!ConfigUtil.isLoliHeadEnabled()) return;
        if (!(player instanceof INeko neko) || !neko.isNeko()) return;
        if (neko.getNekoAgeScale() >= 1.0f) return;

        poseStack.popPose();
        resetModelScales(((PlayerRenderer) (Object) this).getModel());
    }

    // === 辅助方法 ===

    @Unique
    private void applyHeadScale(PlayerModel<?> model, float scale) {
        model.head.xScale = model.head.yScale = model.head.zScale = scale;
        model.hat.xScale = model.hat.yScale = model.hat.zScale = scale;
    }

    @Unique
    private void applyHeadScale(PlayerModel<?> model, float x, float y, float z) {
        model.head.xScale = x;
        model.head.yScale = y;
        model.head.zScale = z;
        model.hat.xScale = x;
        model.hat.yScale = y;
        model.hat.zScale = z;
    }

    @Unique
    private void resetModelScales(PlayerModel<?> model) {
        // 重置头部
        applyHeadScale(model, 1f);

        // 重置身体各部分
        model.body.xScale = model.body.yScale = model.body.zScale = 1f;
        model.leftArm.xScale = model.rightArm.xScale = 1f;
        model.leftLeg.xScale = model.rightLeg.xScale = 1f;
    }
}
