package com.crystalneko.tonekofabric.entity.client;

import net.minecraft.client.render.item.HeldItemRenderer;
import com.crystalneko.tonekofabric.entity.nekoEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class nekoRender extends GeoEntityRenderer<nekoEntity> {
    public HeldItemRenderer heldItemRenderer;

    public nekoRender(EntityRendererFactory.Context renderManager) {
        super(renderManager, new nekoModel());
        this.heldItemRenderer = renderManager.getHeldItemRenderer();
    }
    @Override
    public Identifier getTextureLocation(nekoEntity entity){
        return new Identifier("toneko","textures/entity/neko.png");
    }
    @Override
    public void render(nekoEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider buffSource, int packedLight){
        ItemStack mainHandStack = entity.getMainHandStack();
        if (!mainHandStack.isEmpty()) {
            poseStack.push();
            poseStack.translate(0.2F, 1.5F, 0.5F); // 将物品栈的位置调整到合适的位置
            this.heldItemRenderer.renderItem(entity ,mainHandStack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, poseStack,buffSource, packedLight);
            poseStack.pop();
        }
        super.render(entity,entityYaw,partialTick,poseStack,buffSource,packedLight);
    }
}
