package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.toneko.common.mod.entities.FlySwordEntity;

/**
 * 飞剑实体渲染器（26.x 提交式渲染）。
 *
 * <h2>26.x 迁移说明</h2>
 * 原版 {@code EntityRenderer} 已全面转为「提取渲染状态 → 提交渲染任务」双阶段管线：
 * <ul>
 *   <li>数据提取在 {@link #extractRenderState}（物品→{@link ItemStackRenderState}，
 *       方块→{@link BlockModelRenderState}，矿车→嵌套 {@link MinecartRenderState}）；</li>
 *   <li>绘制在 {@link #submit}，通过 {@link SubmitNodeCollector} 提交。</li>
 * </ul>
 */
public class FlySwordRenderer extends EntityRenderer<FlySwordEntity, FlySwordRenderer.FlySwordRenderState> {

    private final ItemModelResolver itemModelResolver;
    private final BlockModelResolver blockModelResolver;

    public FlySwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.5f;
    }

    /** 飞剑渲染状态：姿态插值 + 底座（物品/方块）+ 可选矿车。 */
    public static class FlySwordRenderState extends net.minecraft.client.renderer.entity.state.EntityRenderState {
        public float yRot;
        public float yawOffset;
        public float pitch;
        public float roll;
        public boolean minecartMode;
        public final ItemStackRenderState item = new ItemStackRenderState();
        public final BlockModelRenderState blockModel = new BlockModelRenderState();
        /** 嵌套矿车渲染状态（仅矿车模式非空） */
        public MinecartRenderState minecartState;
        /** 矿车渲染器缓存（与 state 配对，避免每帧查找） */
        public net.minecraft.client.renderer.entity.AbstractMinecartRenderer<? super AbstractMinecart, ? super MinecartRenderState> minecartRenderer;
    }

    @Override
    public FlySwordRenderState createRenderState() {
        return new FlySwordRenderState();
    }

    @Override
    public void extractRenderState(FlySwordEntity entity, FlySwordRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.yawOffset = entity.getYawOffset(partialTick);
        state.pitch = entity.getPitch(partialTick);
        state.roll = entity.getRoll(partialTick);
        state.minecartMode = entity.isSyncedMinecartMode();

        if (!state.minecartMode && !entity.getDisplayItem().isEmpty()) {
            // 物品底座：解析为 ItemStackRenderState
            Identifier rl = Identifier.tryParse(entity.getDisplayItem());
            java.util.Optional<Item> item = rl == null ? java.util.Optional.empty()
                    : BuiltInRegistries.ITEM.getOptional(rl);
            item.ifPresent(value -> this.itemModelResolver.updateForNonLiving(
                    state.item, new ItemStack(value), ItemDisplayContext.GROUND, entity));
        }

        // 方块底座：物品缺失或矿车模式时显示（非法形状回退钻石块）
        BlockState block = entity.getBlockState();
        RenderShape shape = block.getRenderShape();
        if (shape == net.minecraft.world.level.block.RenderShape.INVISIBLE) {
            block = Blocks.DIAMOND_BLOCK.defaultBlockState();
        }
        if (block.is(Blocks.AIR)) {
            block = Blocks.DIAMOND_BLOCK.defaultBlockState();
        }
        this.blockModelResolver.update(state.blockModel, block, BlockDisplayContext.create());

        if (state.minecartMode) {
            AbstractMinecart minecart = entity.getOrCreateRenderMinecart();
            if (minecart != null) {
                minecart.setPos(entity.getX(), entity.getY(), entity.getZ());
                // 让原版渲染器的 YP(180-yRot) 与外层姿态组合后不产生额外旋转
                minecart.yRotO = 180;
                minecart.setYRot(180);

                EntityRenderDispatcher dispatcher = net.minecraft.client.Minecraft.getInstance()
                        .getEntityRenderDispatcher();
                var renderer = dispatcher.getRenderer(minecart);
                state.minecartRenderer = asMinecartRenderer(renderer);
                if (state.minecartRenderer != null) {
                    MinecartRenderState mrs = state.minecartState != null ? state.minecartState : new MinecartRenderState();
                    state.minecartState = mrs;
                    state.minecartRenderer.extractRenderState(minecart, mrs, partialTick);
                }
            }
        } else {
            state.minecartState = null;
            state.minecartRenderer = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.client.renderer.entity.AbstractMinecartRenderer<AbstractMinecart, MinecartRenderState> asMinecartRenderer(
            EntityRenderer<?, ?> renderer) {
        try {
            return (net.minecraft.client.renderer.entity.AbstractMinecartRenderer<AbstractMinecart, MinecartRenderState>) renderer;
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public void submit(FlySwordRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, 0.2, 0);

        float yawRot = 180 - state.yRot + state.yawOffset; // yRot 由下方 extract 填充
        pose.mulPose(Axis.YP.rotationDegrees(yawRot));
        pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(state.roll));

        // 矿车竖直立于世界空间（在冲浪板倾斜前提交）
        if (state.minecartMode && state.minecartRenderer != null && state.minecartState != null) {
            pose.pushPose();
            pose.translate(0.0, 0.55, 0.0);
            pose.scale(1.8f, 1.8f, 1.8f);
            state.minecartRenderer.submit(state.minecartState, pose, collector, camera);
            pose.popPose();
        }

        // 剑乘姿态：前倾45°，平放（冲浪板）
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(45));
        pose.mulPose(Axis.YP.rotationDegrees(-90));

        if (!state.item.isEmpty()) {
            // 物品模型
            pose.translate(0, 0.15, 0);
            pose.scale(1.2f, 1.2f, 1.2f);
            state.item.submit(pose, collector, state.lightCoords, 0, state.outlineColor);
        } else {
            // 方块底座
            pose.translate(-0.5, 0, -0.5);
            state.blockModel.submit(pose, collector, state.lightCoords, 0, state.outlineColor);
        }

        pose.popPose(); // end surfboard
        pose.popPose(); // end main

        // 名牌与阴影沿用基类（此时 pose 栈已恢复平衡）
        super.submit(state, pose, collector, camera);
    }
}
