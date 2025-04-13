package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.entities.AmmunitionEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class AmmunitionRenderer extends GeoEntityRenderer<AmmunitionEntity> {
    public AmmunitionRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmmunitionModel());
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
