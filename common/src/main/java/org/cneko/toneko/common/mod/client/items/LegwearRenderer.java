package org.cneko.toneko.common.mod.client.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.DyedItemColor;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.Color;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 丝袜渲染器：按 D 值切换半透明管线实现透肉感，
 * 按袜口高度隐藏超出部分的骨骼段，高光带随步频滑动。
 */
public class LegwearRenderer extends GeoArmorRenderer<LegwearItem<?>> {

    // 各段显隐阈值（对应 geo 模型：foot y-0.05~3.0 / calf y3.0~6.5 / thigh y6.5~12.0）
    private static final float SEGMENT_THRESHOLD_FOOT = 0.20f;
    private static final float SEGMENT_THRESHOLD_CALF = 0.50f;
    private static final float SEGMENT_THRESHOLD_THIGH = 0.80f;

    /** 单腿全部骨骼前缀（双 pass 渲染时按腿侧切换可见性） */
    private static final List<String> LEG_BONES = List.of(
            "legwearFoot", "legwearCalf", "legwearThigh", "legwearCuffFoot", "legwearCuffCalf");

    public LegwearRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(MODID, "legwear/legwear")));
    }

    // === 按 denier 切 RenderType：40D+ 不透明，以下半透明透肉 ===

    @Override
    public RenderType getRenderType(LegwearItem<?> animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        int denier = LegwearItem.getDenier(this.currentStack);
        if (denier >= 40) return RenderType.entityCutoutNoCull(texture);
        return RenderType.entityTranslucent(texture);
    }

    // === 左右腿独立染色 + 透肉 alpha（双 pass 渲染） ===

    @Override
    public Color getRenderColor(LegwearItem<?> animatable, float partialTick, int packedLight) {
        // 双 pass 下此值不再用于染色（actuallyRender 自行处理），仅提供默认白色
        return Color.ofRGBA(255, 255, 255, denierAlpha(LegwearItem.getDenier(this.currentStack)));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, LegwearItem<?> animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        int alpha = denierAlpha(LegwearItem.getDenier(this.currentStack));
        int leftRgb = LegwearItem.getLeftRenderColor(this.currentStack);
        int rightRgb = LegwearItem.getRightRenderColor(this.currentStack);

        // 第一遍：按袜口高度恢复段显隐后隐藏右腿，只画左腿（左腿色）
        applyLengthVisibility(model);
        setSideHidden(model, "R", true);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource,
                buffer.setColor((leftRgb >> 16) & 0xFF, (leftRgb >> 8) & 0xFF, leftRgb & 0xFF, alpha),
                isReRender, partialTick, packedLight, packedOverlay,
                (alpha << 24) | (leftRgb & 0xFFFFFF));

        // 第二遍：恢复段显隐后隐藏左腿，只画右腿（右腿色）
        applyLengthVisibility(model);
        setSideHidden(model, "L", true);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource,
                buffer.setColor((rightRgb >> 16) & 0xFF, (rightRgb >> 8) & 0xFF, rightRgb & 0xFF, alpha),
                isReRender, partialTick, packedLight, packedOverlay,
                (alpha << 24) | (rightRgb & 0xFFFFFF));

        // 恢复段显隐（下帧 preRender 也会重设，保持帧末一致）
        applyLengthVisibility(model);
    }

    /** 按腿侧切换全部骨骼可见性（不影响段显隐） */
    private void setSideHidden(BakedGeoModel model, String side, boolean hidden) {
        for (String bone : LEG_BONES) {
            model.getBone(bone + side).ifPresent(b -> b.setHidden(hidden));
        }
    }

    // === Trinkets 渲染器手动驱动用 setter（照 NekoArmorRenderer 模式） ===

    public void setItemStack(net.minecraft.world.item.ItemStack stack) {
        this.currentStack = stack;
    }

    public void setEntity(net.minecraft.world.entity.Entity entity) {
        this.currentEntity = entity;
    }

    public void setBaseModel(net.minecraft.client.model.HumanoidModel<?> baseModel) {
        this.baseModel = baseModel;
    }

    public void setSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        this.currentSlot = slot;
    }

    public void setAnimatable(LegwearItem<?> animatable) {
        this.animatable = animatable;
    }

    // === 按袜口高度隐藏段 + 高光滑动 ===

    @Override
    public void preRender(PoseStack poseStack, LegwearItem<?> item, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        super.preRender(poseStack, item, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        applyLengthVisibility(model);
    }

    /** denier → alpha（0~255）：5D 极透、20D 透肉、40D 不透明 */
    private static int denierAlpha(int denier) {
        if (denier >= 40) return 255;
        if (denier >= 20) return 160;
        return 70;
    }

    private void applyLengthVisibility(BakedGeoModel model) {
        float length = LegwearItem.getStockingTopHeight(this.currentStack);
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
