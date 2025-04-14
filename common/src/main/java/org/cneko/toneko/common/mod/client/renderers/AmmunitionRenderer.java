package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.entities.AmmunitionEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class AmmunitionRenderer extends GeoEntityRenderer<AmmunitionEntity> {
    public AmmunitionRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmmunitionModel());
    }

    @Override
    public void preRender(PoseStack poseStack, AmmunitionEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        poseStack.scale(0.1f,0.1f,0.1f);
    }

    public static class AmmunitionModel extends GeoModel<AmmunitionEntity> {

        @Override
        public ResourceLocation getModelResource(AmmunitionEntity ammunitionEntity) {
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"geo/neko/crystal_neko.geo.json"
            );
        }

        @Override
        public ResourceLocation getTextureResource(AmmunitionEntity ammunitionEntity) {
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"textures/neko/crystal_neko.png"
            );
        }

        @Override
        public ResourceLocation getAnimationResource(AmmunitionEntity ammunitionEntity) {
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/crystal_neko.animation.json"
            );
        }
    }
}
