package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.boss.NekoBoss;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class NekoBossRenderer<T extends NekoEntity & NekoBoss> extends NekoRenderer<T>{
    public NekoBossRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NekoBossModel<>());
    }

    public static class NekoBossModel<T extends NekoEntity & NekoBoss> extends NekoModel<T> {
        @Override
        public ResourceLocation getModelResource(T animatable) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"geo/neko/boss/"+animatable.getSkin()+".geo.json"
            );
            if (checkResource(id)){
                return id;
            }
            return toNekoLoc("geo/neko/common.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"textures/neko/boss/"+animatable.getSkin()+".png"
            );
            if (checkResource(id)){
                return id;
            }
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"textures/neko/common.png"
            );
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/boss/"+animatable.getSkin()+".animation.json"
            );
            if (checkResource(id)){
                return id;
            }
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/common.animation.json"
            );
        }
    }
}
