package org.cneko.toneko.common.mod.client.items;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 丝袜渲染器：按 D 值切换半透明管线实现透肉感，
 * 按袜口高度隐藏超出部分的骨骼段，左右腿独立染色。
 *
 * <h2>GeckoLib 5.5 渲染说明</h2>
 * 直接继承 {@link GeoArmorRenderer} 而非 {@code DyeableGeoArmorRenderer}：
 * GeckoLib 5.5.2 的 {@code DyeableGeoArmorRenderer#renderDyedBone} 丢弃传入的 {@code pose}
 * 并使用空 {@code new PoseStack()} 渲染，导致所有可染色骨骼浮在相机原点（天上）；
 * 同时它未调用 {@code super.adjustModelBonesForRender} 且无视段隐藏。
 * 这里在 {@link #submitRenderTasks} 中按腿段分别传入左/右腿染色并沿用正确的 {@code pose}，
 * 使动画、段显隐、透肉与左右腿独立染色在玩家/猫娘实体/物品栏预览下全部正确生效。
 */
public class LegwearRenderer<S extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<LegwearItem<?>, S> {

    // 各段显隐阈值（对应 geo 模型：foot y-0.05~3.0 / calf y3.0~6.5 / thigh y6.5~12.0）
    private static final float SEGMENT_THRESHOLD_FOOT = 0.20f;
    private static final float SEGMENT_THRESHOLD_CALF = 0.50f;
    private static final float SEGMENT_THRESHOLD_THIGH = 0.80f;

    public LegwearRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MODID, "legwear/legwear")));
    }

    @Override
    public void captureDefaultRenderState(LegwearItem<?> item, GeoArmorRenderer.RenderData data,
                                          S state, float partialTick) {
        super.captureDefaultRenderState(item, data, state, partialTick);
        // 物品堆入状态，供 getRenderType / 段显隐 / 左右腿染色读取
        state.addGeckolibData(NekoArmorRenderer.ITEM_STACK, data.itemStack());
    }

    // === 按 denier 切 RenderType：40D+ 不透明，以下半透明透肉 ===

    @Override
    public RenderType getRenderType(S state, Identifier texture) {
        ItemStack stack = state.getOrDefaultGeckolibData(NekoArmorRenderer.ITEM_STACK, ItemStack.EMPTY);
        return pickRenderType(stack, texture);
    }

    /** 按 denier / 湿度选择渲染管线。 */
    private static RenderType pickRenderType(ItemStack stack, Identifier texture) {
        if (stack.isEmpty()) return RenderTypes.armorCutoutNoCull(texture);
        int denier = LegwearItem.getDenier(stack);
        int wetness = WetnessUtil.get(stack);
        // 40D+ 不透明，但湿透（>=50）时贴肉透出肤色，切回半透明管线
        if (denier >= 40 && wetness < 50) return RenderTypes.armorCutoutNoCull(texture);
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public int getRenderColor(LegwearItem<?> animatable, GeoArmorRenderer.RenderData stackAndSlot, float partialTick) {
        return 0xFFFFFFFF;
    }

    // === 左右腿独立染色 + 透肉 alpha ===

    @Override
    public void submitRenderTasks(RenderPassInfo<S> renderPassInfo, OrderedSubmitNodeCollector renderTasks, @Nullable RenderType renderType) {
        if (renderType == null) return;

        final int packedLight = renderPassInfo.packedLight();
        final int packedOverlay = renderPassInfo.packedOverlay();
        final S renderState = renderPassInfo.renderState();
        final EquipmentSlot slot = Objects.requireNonNull(renderState.getGeckolibData(CURRENT_SLOT));
        final BakedGeoModel model = renderPassInfo.model();

        if (model.isMissingno()) {
            submitMissingModelRender(renderPassInfo, renderTasks);
            return;
        }

        final ItemStack stack = renderState.getOrDefaultGeckolibData(NekoArmorRenderer.ITEM_STACK, ItemStack.EMPTY);
        final int alpha = stack.isEmpty() ? 255 : renderAlpha(stack);
        final int leftColor = stack.isEmpty()
                ? renderPassInfo.renderColor()
                : ((alpha << 24) | (LegwearItem.getLeftRenderColor(stack) & 0xFFFFFF));
        final int rightColor = stack.isEmpty()
                ? renderPassInfo.renderColor()
                : ((alpha << 24) | (LegwearItem.getRightRenderColor(stack) & 0xFFFFFF));

        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, vertexConsumer) -> {
            final PoseStack poseStack = renderPassInfo.poseStack();

            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPassInfo.renderPosed(() -> {
                for (ArmorSegment segment : getSegmentsForSlot(renderState, slot)) {
                    int segColor = (segment == ArmorSegment.LEFT_LEG || segment == ArmorSegment.LEFT_FOOT)
                            ? leftColor : rightColor;
                    renderPassInfo.model().getBone(getBoneNameForSegment(renderState, segment))
                            .ifPresent(bone -> bone.positionAndRender(renderPassInfo, vertexConsumer, packedLight, packedOverlay, segColor));
                }
            });
            poseStack.popPose();
        });
    }

    /** 按袜口高度隐藏段（每帧在骨骼编译阶段重设） */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<S> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        ItemStack stack = info.renderState().getOrDefaultGeckolibData(NekoArmorRenderer.ITEM_STACK, ItemStack.EMPTY);
        if (stack.isEmpty()) return;
        float length = LegwearItem.getStockingTopHeight(stack);
        boolean foot = length >= SEGMENT_THRESHOLD_FOOT;
        boolean calf = length >= SEGMENT_THRESHOLD_CALF;
        boolean thigh = length >= SEGMENT_THRESHOLD_THIGH;

        for (String side : List.of("L", "R")) {
            // 三段本体
            setBoneSkipped(snapshots, "legwearFoot" + side, !foot);
            setBoneSkipped(snapshots, "legwearCalf" + side, !calf);
            setBoneSkipped(snapshots, "legwearThigh" + side, !thigh);
            // 勒痕环只显示在"当前袜口所在的那一段"顶部（大腿段顶部即髋部，不做环）
            setBoneSkipped(snapshots, "legwearCuffFoot" + side, !(foot && !calf));
            setBoneSkipped(snapshots, "legwearCuffCalf" + side, !(calf && !thigh));
        }
    }

    private static void setBoneSkipped(BoneSnapshots snapshots, String name, boolean skipped) {
        snapshots.ifPresent(name, snapshot -> snapshot.skipRender(skipped));
    }

    /** denier → alpha（0~255）：5D 极透、20D 透肉、40D 不透明 */
    private static int denierAlpha(int denier) {
        if (denier >= 40) return 255;
        if (denier >= 20) return 160;
        return 70;
    }

    /** 渲染 alpha：D 值透肉基础上，湿透进一步贴肉透出肤色（alpha 下调 = 更透） */
    private static int renderAlpha(ItemStack stack) {
        int alpha = denierAlpha(LegwearItem.getDenier(stack));
        int wetness = WetnessUtil.get(stack);
        if (wetness >= 80) alpha = (int) (alpha * 0.6f);
        else if (wetness >= 50) alpha = (int) (alpha * 0.8f);
        return Math.max(0, Math.min(255, alpha));
    }
}
