package org.cneko.toneko.fabric.client.items;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.cneko.toneko.fabric.enchanments.ToNekoEnchantments;
import org.cneko.toneko.fabric.items.NekoArmor;
import org.cneko.toneko.fabric.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.Color;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoArmorRenderer<T extends NekoArmor<T>> extends GeoArmorRenderer<T>{

    public NekoArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.of(MODID, "armor/neko_armor")));
    }

    @Override
    public void preRender(MatrixStack poseStack, T item, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        // 如果有反转附魔
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.getValue(), this.currentStack)) {
            // 旋转180度
            poseStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180));
            // 前进1/16个单位并下移动1单位
            poseStack.translate(0, -1.5, -0.0625);
        }

        super.preRender(poseStack, item, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void actuallyRender(MatrixStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderLayer renderType,
                               VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        // 染色
        try {
            // 如果有染色
            if (this.currentStack.contains(DataComponentTypes.DYED_COLOR)) {
                // 物品的染色rgb
                Color renderColor = this.getRenderColor(animatable, partialTick, packedLight);

                // 渲染物品颜色
                buffer = buffer.color(renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), renderColor.getAlpha());

                colour = renderColor.getColor();
            }

        } catch (Exception ignored) {
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        // 默认为浅蓝色
        return Color.ofOpaque(DyedColorComponent.getColor(this.currentStack, 6463722));
    }
    public void setItemStack(ItemStack stack){
        this.currentStack = stack;
    }
    public void setEntity(Entity entity){
        this.currentEntity = entity;
    }
    public void setBaseModel(BipedEntityModel<?> baseModel){
        this.baseModel = baseModel;
    }
    public void setSlot(EquipmentSlot slot){
        this.currentSlot = slot;
    }
    public void setAnimatable(T animatable){
        this.animatable = animatable;
    }

    public static class NekoTailRenderer extends NekoArmorRenderer<NekoArmor.NekoTailItem> {
        public NekoTailRenderer() {
            super();
        }
    }

    public static class NekoEarsRenderer extends NekoArmorRenderer<NekoArmor.NekoEarsItem> {
        public NekoEarsRenderer() {
            super();
        }
    }

    public static class NekoPawsRenderer extends NekoArmorRenderer<NekoArmor.NekoPawsItem> {
        public NekoPawsRenderer() {
            super();
        }
    }

}
