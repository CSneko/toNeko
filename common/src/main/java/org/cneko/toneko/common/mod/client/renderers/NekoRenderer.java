package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

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
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        if (bone.getName().equals("RightArm")) {
            ItemStack mainHandItem = animatable.getItemInHand();
            if (!mainHandItem.isEmpty()) {
                poseStack.pushPose();
                // 应用骨骼变换
                poseStack.mulPose(bone.getModelSpaceMatrix());
                // 调整物品位置
                poseStack.translate(0.1, -0.6, -0.1);

                // 绕X轴旋转75度
                poseStack.mulPose(Axis.XP.rotationDegrees(285F));

                // 调整物品大小
                poseStack.scale(0.8F, 0.8F, 0.8F);

                // 渲染物品
                ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                itemRenderer.renderStatic(mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        packedLight, OverlayTexture.NO_OVERLAY,
                        poseStack, bufferSource, animatable.level(), animatable.getId());
                poseStack.popPose();
            }
        }
    }

    public static class NekoModel<T extends NekoEntity> extends GeoModel<T> {
        @Override
        public ResourceLocation getModelResource(T animatable) {
            /*
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
             *
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"geo/neko/"+animatable.getRandomSkin()+".geo.json"
            );
            */
             return toNekoLoc("geo/neko/common.geo.json");
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
            /*
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/"+animatable.getSkin()+".animation.json"
            );
            if (checkResource(id)){
                return id;
            }
            return ResourceLocation.fromNamespaceAndPath(
                    MODID,"animations/neko/"+animatable.getRandomSkin()+".animation.json"
            );

             */
            return toNekoLoc("animations/neko/common.animation.json");
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