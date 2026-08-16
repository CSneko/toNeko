package org.cneko.toneko.common.mod.mixin.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.cneko.toneko.common.mod.client.api.StompAnimations;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 PlayerAnimator 的 fakeThirdPersonMode 之后、实体渲染之前，关闭 firstPersonPass。
 * <p>
 * PlayerAnimator 的 THIRD_PERSON_MODEL 第一人称渲染会把 firstPersonPass 置 true，
 * 从而触发两个副作用：hideBonesInFirstPerson（隐藏腿等所有部件）和
 * filterLayers（过滤掉 Trinkets legwear 等 layer）。本 mixin 在踩踏期间把该标记
 * 改回 false，保留「强制第三人称相机渲染玩家模型」的行为，但去掉隐藏/过滤副作用，
 * 让玩家第一人称能看到自己的腿与腿部物品。
 */
@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class StompFirstPersonPassMixin {

    @Inject(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;isDetached()Z",
                    shift = At.Shift.AFTER
            )
    )
    private void toneko$disableFirstPersonPassForStomp(DeltaTracker deltaTracker, boolean bl, Camera camera,
                                                        GameRenderer gameRenderer, LightTexture lightTexture,
                                                        Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (isFirstPersonStomping(camera)) {
            FirstPersonMode.setFirstPersonPass(false);
        }
    }

    private static boolean isFirstPersonStomping(Camera camera) {
        if (!(camera.getEntity() instanceof AbstractClientPlayer player)) return false;
        if (!StompAnimations.isStomping(player)) return false;
        return Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
    }
}
