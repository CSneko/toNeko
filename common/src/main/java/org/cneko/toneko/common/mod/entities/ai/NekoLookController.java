package org.cneko.toneko.common.mod.entities.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import org.cneko.toneko.common.mod.entities.NekoEntity;

/**
 * 猫娘专用的 LookControl。
 *
 * <p>核心改进：移动时头部跟随身体方向（身体朝前、头也朝前），
 * 避免 Minecraft 原版的"身体朝前走、头扭向一边"的僵硬姿态。
 * 静止时允许头部独立旋转，保持围观玩家等自然行为。</p>
 *
 * <p>平滑过渡：在"移动朝向"和"目标朝向"之间根据移动速度做 blend，
 * 移动速度越快，头部越跟随身体；静止时则完全自由。</p>
 */
public class NekoLookController extends LookControl {
    private final NekoEntity neko;
    private static final float HEAD_FOLLOW_BODY_SPEED = 15.0F;

    public NekoLookController(NekoEntity neko) {
        super(neko);
        this.neko = neko;
    }

    @Override
    public void tick() {
        if (this.resetXRotOnTick()) {
            this.neko.setXRot(0.0F);
        }

        if (this.lookAtCooldown > 0) {
            this.lookAtCooldown--;

            // 获取移动速度用于 blend
            float moveSpeed = (float) this.neko.getDeltaMovement().horizontalDistance();

            if (moveSpeed > 0.05f && this.neko.getNekoBrain().isMoving()) {
                // 移动中：头部向身体朝向 blend
                // blendFactor: 0 = 完全自由看目标, 1 = 完全跟身体
                float blendFactor = Math.min(moveSpeed * 3.0f, 0.8f);

                // Y 轴（水平）：blend 身体朝向和 look 目标
                float bodyYaw = this.neko.yBodyRot;
                float targetYaw = this.getYRotD().orElse(bodyYaw);
                float blendedYaw = bodyYaw + (targetYaw - bodyYaw) * (1.0f - blendFactor);
                this.neko.yHeadRot = this.rotateTowards(this.neko.yHeadRot, blendedYaw, this.yMaxRotSpeed);

                // X 轴（俯仰）：移动时轻微跟随
                float targetPitch = this.getXRotD().orElse(0.0f);
                this.neko.setXRot(this.rotateTowards(this.neko.getXRot(), targetPitch * (1.0f - blendFactor * 0.7f), this.xMaxRotAngle));
            } else {
                // 静止或几乎不移动：允许独立头部旋转
                this.getYRotD().ifPresent(yaw -> this.neko.yHeadRot = this.rotateTowards(this.neko.yHeadRot, yaw, this.yMaxRotSpeed));
                this.getXRotD().ifPresent(pitch -> this.neko.setXRot(this.rotateTowards(this.neko.getXRot(), pitch, this.xMaxRotAngle)));
            }
        } else {
            // 没有主动 look 目标时，头部自然回正到身体朝向
            this.neko.yHeadRot = this.rotateTowards(this.neko.yHeadRot, this.neko.yBodyRot, HEAD_FOLLOW_BODY_SPEED);
        }

        // 限制头部旋转范围
        this.clampHeadRotationToBody();
    }

    @Override
    protected boolean resetXRotOnTick() {
        return true;
    }

    @Override
    protected float rotateTowards(float from, float to, float maxDelta) {
        float diff = Mth.degreesDifference(from, to);
        float clamped = Mth.clamp(diff, -maxDelta, maxDelta);
        return from + clamped;
    }

}
