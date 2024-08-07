package org.cneko.toneko.fabric.client.items;

import org.cneko.toneko.fabric.enchanments.ToNekoEnchantments;
import org.cneko.toneko.fabric.items.NekoArmor;
import org.cneko.toneko.fabric.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.Color;

import static org.cneko.toneko.common.Bootstrap.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class NekoArmorRenderer<T extends NekoArmor<T>> extends GeoArmorRenderer<T>{

    public NekoArmorRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(MODID, "armor/neko_armor")));
    }

    @Override
    public void preRender(PoseStack poseStack, T item, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        // 如果有反转附魔
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.location(), this.currentStack)) {
            // 旋转180度
            poseStack.mulPose(Axis.XN.rotationDegrees(180));
            // 前进1/16个单位并下移动1单位
            poseStack.translate(0, -1.5, -0.0625);
        }

        super.preRender(poseStack, item, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        // 染色
        try {
            // 如果有染色
            if (this.currentStack.has(DataComponents.DYED_COLOR)) {
                // 物品的染色rgb
                Color renderColor = this.getRenderColor(animatable, partialTick, packedLight);

                // 渲染物品颜色
                buffer = buffer.setColor(renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), renderColor.getAlpha());

                colour = renderColor.getColor();
            }

        } catch (Exception ignored) {
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        // 默认为浅蓝色
        return Color.ofOpaque(DyedItemColor.getOrDefault(this.currentStack, 6463722));
    }
    public void setItemStack(ItemStack stack){
        this.currentStack = stack;
    }
    public void setEntity(Entity entity){
        this.currentEntity = entity;
    }
    public void setBaseModel(HumanoidModel<?> baseModel){
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
