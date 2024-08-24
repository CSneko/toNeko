package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoRenderer<T extends NekoEntity> extends GeoEntityRenderer<T> {

    public NekoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NekoModel<>());
    }



    public static class NekoModel<T extends NekoEntity> extends GeoModel<T> {
        @Override
        public ResourceLocation getModelResource(T animatable) {
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"geo/neko/"+animatable.getSkin()+".geo.json"
            );
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"textures/neko/"+animatable.getSkin()+".png"
            );
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/"+animatable.getSkin()+".animation.json"
            );
        }
    }
}
