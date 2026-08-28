package org.cneko.toneko.common.mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.util.ConfigUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 猫娘「萝莉头」渲染缩放。
 *
 * <h2>26.x 迁移说明</h2>
 * {@code PlayerRenderer#render} 直接绘制挂钩已被移除：现在在
 * {@link AvatarRenderer} 的 extract（实体→渲染状态）阶段计算缩放系数，
 * 在 scale（状态→姿态）阶段统一应用身体缩放与头部补偿。
 * 模型部件为共享实例，因此所有改写遵循「每帧先复位再设置」的原则。
 */
@Mixin(AvatarRenderer.class)
public class PlayerRendererMixin {

    @Unique
    private static final float BASE_HEAD_SCALE = 1.0f;

    /** 渲染状态 → 身体缩放系数（extract 写入，scale 读取并移除；弱引用防泄漏） */
    @Unique
    private static final Map<AvatarRenderState, float[]> SCALE_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void toneko$afterExtract(Avatar player, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (!ConfigUtil.isLoliHeadEnabled()) return;
        // 只有猫娘才触发萝莉头效果
        if (!(player instanceof INeko neko) || !neko.isNeko()) return;

        // 根据猫娘年龄计算身体缩放比例
        double ageScale = neko.getNekoAgeScale();
        if (ageScale >= 1.0f) return; // 成年猫娘不需要处理

        SCALE_CACHE.put(state, new float[]{(float) ageScale});
    }

    @Inject(
        method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("TAIL")
    )
    private void toneko$loliScale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        float[] cached = SCALE_CACHE.remove(state);
        if (cached == null) return;
        float ageScale = cached[0];

        poseStack.scale(ageScale, ageScale, ageScale);

        PlayerModel model = (PlayerModel) ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel();

        // 先复位共享模型部件的缩放，避免上一帧残留
        resetModelScales(model);

        if (ConfigUtil.isLoliHeadAlgorithmEnabled()) {
            // 动态算法模式：身体越小，头部补偿越大
            float headScale = BASE_HEAD_SCALE / ageScale * ConfigUtil.getLoliHeadAlgorithmRatio();
            applyHeadScale(model, headScale);
        } else {
            // 自定义缩放模式：使用配置的固定头部大小
            applyHeadScale(model,
                ConfigUtil.getLoliHeadCustomXScale(),
                ConfigUtil.getLoliHeadCustomYScale(),
                ConfigUtil.getLoliHeadCustomZScale());
        }
    }

    // === 辅助方法 ===

    @Unique
    private void applyHeadScale(PlayerModel model, float scale) {
        setScales(model.head, scale);
        setScales(model.hat, scale);
    }

    @Unique
    private void applyHeadScale(PlayerModel model, float x, float y, float z) {
        model.head.xScale = x;
        model.head.yScale = y;
        model.head.zScale = z;
        model.hat.xScale = x;
        model.hat.yScale = y;
        model.hat.zScale = z;
    }

    @Unique
    private static void setScales(ModelPart part, float s) {
        part.xScale = s;
        part.yScale = s;
        part.zScale = s;
    }

    @Unique
    private void resetModelScales(PlayerModel model) {
        // 重置头部
        applyHeadScale(model, 1f);

        // 重置身体各部分
        setScales(model.body, 1f);
        setScales(model.leftArm, 1f);
        setScales(model.rightArm, 1f);
        setScales(model.leftLeg, 1f);
        setScales(model.rightLeg, 1f);
    }
}
