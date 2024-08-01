package org.cneko.toneko.fabric.client.items;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
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


import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoArmorRenderer<T extends NekoArmor<T>> extends GeoArmorRenderer<T>{

    public NekoArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.of(MODID, "armor/neko_armor")));
    }

    @Override
    public void preRender(MatrixStack poseStack, T item, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        super.preRender(poseStack,item,model,bufferSource,buffer,isReRender,partialTick,packedLight,packedOverlay,colour);
        // 如果有反转附魔
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.getValue(),this.currentStack)){
            // 旋转180度
            poseStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180));
            // 前进1/16个单位并下移动1单位
            poseStack.translate(0,-1.5,-0.0625);
        }
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

}
