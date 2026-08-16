package org.cneko.toneko.common.mod.client.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 丝袜物品渲染器（背包 / 手持 / 掉落物 / 展示框）。
 * <p>
 * 与 {@link LegwearRenderer} 保持同一套 D 值透肉、左右腿独立染色和袜口高度显隐逻辑，
 * 让物品图标与身上穿的模型完全一致。
 */
public class LegwearItemRenderer extends GeoItemRenderer<LegwearItem<?>> {

    private static final float SEGMENT_THRESHOLD_FOOT = 0.20f;
    private static final float SEGMENT_THRESHOLD_CALF = 0.50f;
    private static final float SEGMENT_THRESHOLD_THIGH = 0.80f;

    /** 单腿全部骨骼前缀（双 pass 渲染时按腿侧切换可见性） */
    private static final List<String> LEG_BONES = List.of(
            "legwearFoot", "legwearCalf", "legwearThigh", "legwearCuffFoot", "legwearCuffCalf");

    public LegwearItemRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(MODID, "legwear/legwear")));
        useAlternateGuiLighting();
    }

    // === 按 denier 切 RenderType：40D+ 不透明，以下半透明透肉 ===

    @Override
    public RenderType getRenderType(LegwearItem<?> animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        ItemStack stack = getCurrentItemStack();
        if (stack == null) return RenderType.entityCutoutNoCull(texture);
        int denier = LegwearItem.getDenier(stack);
        int wetness = WetnessUtil.get(stack);
        // 40D+ 不透明，但湿透（>=50）时贴肉透出肤色，切回半透明管线
        if (denier >= 40 && wetness < 50) return RenderType.entityCutoutNoCull(texture);
        return RenderType.entityTranslucent(texture);
    }

    // === 左右腿独立染色 + 透肉 alpha（双 pass 渲染） ===

    @Override
    public Color getRenderColor(LegwearItem<?> animatable, float partialTick, int packedLight) {
        // 双 pass 下此值不再用于染色（actuallyRender 自行处理），仅提供默认白色
        ItemStack stack = getCurrentItemStack();
        return stack == null ? Color.WHITE : Color.ofRGBA(255, 255, 255, renderAlpha(stack));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, LegwearItem<?> animatable, BakedGeoModel model,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        ItemStack stack = getCurrentItemStack();
        int alpha = renderAlpha(stack);
        int leftRgb = LegwearItem.getLeftRenderColor(stack);
        int rightRgb = LegwearItem.getRightRenderColor(stack);

        // 第一遍：按袜口高度恢复段显隐后隐藏右腿，只画左腿（左腿色）
        applyLengthVisibility(model, stack);
        setSideHidden(model, "R", true);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource,
                buffer == null ? null : buffer.setColor((leftRgb >> 16) & 0xFF, (leftRgb >> 8) & 0xFF, leftRgb & 0xFF, alpha),
                isReRender, partialTick, packedLight, packedOverlay,
                (alpha << 24) | (leftRgb & 0xFFFFFF));

        // 第二遍：恢复段显隐后隐藏左腿，只画右腿（右腿色）
        applyLengthVisibility(model, stack);
        setSideHidden(model, "L", true);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource,
                buffer == null ? null : buffer.setColor((rightRgb >> 16) & 0xFF, (rightRgb >> 8) & 0xFF, rightRgb & 0xFF, alpha),
                true, partialTick, packedLight, packedOverlay,
                (alpha << 24) | (rightRgb & 0xFFFFFF));

        // 恢复段显隐（下帧 preRender 也会重设，保持帧末一致）
        applyLengthVisibility(model, stack);
    }

    /** 按腿侧切换全部骨骼可见性（不影响段显隐） */
    private void setSideHidden(BakedGeoModel model, String side, boolean hidden) {
        for (String bone : LEG_BONES) {
            model.getBone(bone + side).ifPresent(b -> b.setHidden(hidden));
        }
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

    private void applyLengthVisibility(BakedGeoModel model, ItemStack stack) {
        float length = LegwearItem.getStockingTopHeight(stack);
        boolean foot = length >= SEGMENT_THRESHOLD_FOOT;
        boolean calf = length >= SEGMENT_THRESHOLD_CALF;
        boolean thigh = length >= SEGMENT_THRESHOLD_THIGH;

        for (String side : List.of("L", "R")) {
            // 三段本体
            setBoneHidden(model, "legwearFoot" + side, !foot);
            setBoneHidden(model, "legwearCalf" + side, !calf);
            setBoneHidden(model, "legwearThigh" + side, !thigh);
            // 勒痕环只显示在"当前袜口所在的那一段"顶部（大腿段顶部即髋部，不做环）
            setBoneHidden(model, "legwearCuffFoot" + side, !(foot && !calf));
            setBoneHidden(model, "legwearCuffCalf" + side, !(calf && !thigh));
        }
    }

    private static void setBoneHidden(BakedGeoModel model, String name, boolean hidden) {
        model.getBone(name).ifPresent((GeoBone b) -> b.setHidden(hidden));
    }
}
