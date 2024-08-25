package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoRenderer<T extends NekoEntity> extends GeoEntityRenderer<T> {

    public NekoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NekoModel<>());
    }



    public static class NekoModel<T extends NekoEntity> extends GeoModel<T> {
        @Override
        public ResourceLocation getModelResource(T animatable) {
            // 检查文件是否存在
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"geo/neko/"+animatable.getSkin()+".geo.json"
            );
            if (checkResource(id)){
                return id;
            }
            /* 虽然随机皮肤会导致动画，模型，贴图可能不会一一对应，但是能用
             * 更好的实现方法应该是getDefaultSkin()
             * 但我觉得...这样似乎更为抽象和有趣(bushi)
             */
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"geo/neko/"+animatable.getRandomSkin()+".geo.json"
            );
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"textures/neko/"+animatable.getSkin()+".png"
            );
            if (checkResource(id)){
                return id;
            }
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"textures/neko/"+animatable.getRandomSkin()+".png"
            );
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/"+animatable.getRandomSkin()+".animation.json"
            );
            if (checkResource(id)){
                return id;
            }
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/"+animatable.getRandomSkin()+".animation.json"
            );
        }
    }

    public static ResourceManager getResourceManager(){
        return Minecraft.getInstance().getResourceManager();
    }
    public static boolean checkResource(ResourceLocation location){
        try {
            Optional<Resource> resource = getResourceManager().getResource(location);
            return resource.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
