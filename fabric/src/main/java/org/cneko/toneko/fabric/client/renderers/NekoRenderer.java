package org.cneko.toneko.fabric.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoRenderer<T extends NekoEntity> extends GeoEntityRenderer<T> {

    public NekoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NekoModel<>());
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (animatable.isBaby()){
            poseStack.scale(0.5F, 0.5F, 0.5F); // 将幼年实体的尺寸缩小为原来的一半
        }
        // 坐下时向下移动
        if (animatable.isSitting()) {
            poseStack.translate(0, -0.7, 0);
        }
        // 游泳/爬行时向下移动
        if (animatable.getPose() == Pose.SWIMMING){
            poseStack.translate(0, -0.5, 0);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T entity, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
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
                    MODID,"animations/neko/"+animatable.getSkin()+".animation.json"
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
