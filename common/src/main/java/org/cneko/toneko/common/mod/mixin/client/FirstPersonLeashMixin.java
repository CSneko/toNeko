package org.cneko.toneko.common.mod.mixin.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/**
 * 第一人称下显示「自己正被拴绳牵着」的绳子。
 *
 * <p>背景：猫娘可以把玩家拴起来（{@code NekoEntity} → {@code Leashable#setLeashedTo(player)}），
 * 这时绳子是画在「被拴的一方」的渲染状态里的。原版只在渲染被拴实体时提交这条绳子，
 * 而第一人称根本不会渲染本地玩家的模型，于是绳子就整个消失了（#94）。
 *
 * <p>做法：不碰玩家的渲染状态，改为把这条绳子挂到 $(持有者)$（猫娘）的渲染状态上——
 * 猫娘在第一人称是正常渲染的，于是绳子自然可见。{@link EntityRenderState.LeashState} 里的
 * {@code offset} 是相对「绳子起点」的位移，而渲染时的姿势矩阵是被挂靠实体的相机相对位移，
 * 所以这里按 {@code start - 被挂靠实体坐标} 折算，和原版公式保持一致。
 *
 * <p>只在第一人称、且本地玩家恰好被当前渲染的实体牵着时触发，第三人称仍走原版路径，
 * 不会重复画两根。
 */
@Mixin(EntityRenderer.class)
public class FirstPersonLeashMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void toneko$extractFirstPersonLeash(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // 只在第一人称补：第三人称由被拴玩家自己的渲染状态负责
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        // 渲染的是别人（包括自己）就没必要补
        if (entity == mc.player || !(mc.player instanceof Leashable self)) {
            return;
        }
        // 只处理「本地玩家被当前渲染的实体牵着」这一种情况
        if (self.getLeashHolder() != entity) {
            return;
        }
        if (!(entity instanceof Leashable holder) || holder.supportQuadLeash()) {
            // 四足拴法在 26.x 有自己的 offset 数组，不在这里处理
            return;
        }

        // 起点：玩家身上的拴绳挂点（与原版 EntityRenderer#extractRenderState 同一套公式）
        Vec3 start = mc.player.getPosition(partialTick).add(
                self.getLeashOffset(partialTick)
                        .yRot(-mc.player.getPreciseBodyRotation(partialTick) * Mth.DEG_TO_RAD));
        // 终点：持有者（猫娘）的握绳位置
        Vec3 end = entity.getRopeHoldPosition(partialTick);

        EntityRenderState.LeashState leashState = new EntityRenderState.LeashState();
        leashState.start = start;
        leashState.end = end;
        // 姿势矩阵是「持有者相对相机」的，所以 offset 用 start 相对持有者的位移
        leashState.offset = start.subtract(entity.getPosition(partialTick));
        leashState.slack = false;

        Level level = entity.level();
        BlockPos startPos = BlockPos.containing(mc.player.getEyePosition(partialTick));
        BlockPos endPos = BlockPos.containing(entity.getEyePosition(partialTick));
        leashState.startBlockLight = level.getBrightness(LightLayer.BLOCK, startPos);
        leashState.startSkyLight = level.getBrightness(LightLayer.SKY, startPos);
        leashState.endBlockLight = level.getBrightness(LightLayer.BLOCK, endPos);
        leashState.endSkyLight = level.getBrightness(LightLayer.SKY, endPos);

        if (state.leashStates == null) {
            state.leashStates = new ArrayList<>(1);
        } else if (!(state.leashStates instanceof ArrayList)) {
            state.leashStates = new ArrayList<>(state.leashStates);
        }
        // 渲染状态是复用的：持有者自己没被拴时，原版不会重建 leashStates，
        // 这里必须每帧重建，否则会一帧一根越积越多。
        // （持有者自己也被拴时，原版下一帧会按自身数量重建列表，多出来的这条会被自然清掉。）
        if (!holder.isLeashed()) {
            state.leashStates = new ArrayList<>(1);
        }
        state.leashStates.add(leashState);
    }
}
