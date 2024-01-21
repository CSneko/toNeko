package com.crystalneko.tonekofabric.entity.client;

import com.crystalneko.tonekofabric.entity.client.nekoModel;
import com.crystalneko.tonekofabric.entity.nekoEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class nekoRender extends GeoEntityRenderer<nekoEntity> {
    public nekoRender(EntityRendererFactory.Context renderManager) {
        super(renderManager, new nekoModel());
    }
    @Override
    public Identifier getTextureLocation(nekoEntity entity){
        return new Identifier("toneko","textures/entity/neko.png");
    }
    /*@Override
    public void render(nekoEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider buffSource, int packedLight){
        if(entity.isBaby()){
            poseStack.scale(0.4F,0.4F,0.4F);
        }
        super.render(entity,entityYaw,partialTick,poseStack,buffSource,packedLight);
    }*/

}
