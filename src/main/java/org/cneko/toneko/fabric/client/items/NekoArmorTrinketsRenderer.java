package org.cneko.toneko.fabric.client.items;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import static org.cneko.toneko.fabric.items.ToNekoItems.*;
public class NekoArmorTrinketsRenderer {
    public static NekoTailTrinketRenderer NEKO_TAIL_TRINKET_RENDERER;
    public static NekoEarsTrinketRenderer NEKO_EARS_TRINKET_RENDERER;
    public static void init(){
        NEKO_TAIL_TRINKET_RENDERER = new NekoTailTrinketRenderer();
        NEKO_EARS_TRINKET_RENDERER = new NekoEarsTrinketRenderer();
        TrinketRendererRegistry.registerRenderer(NEKO_TAIL,NEKO_TAIL_TRINKET_RENDERER);
        TrinketRendererRegistry.registerRenderer(NEKO_EARS,NEKO_EARS_TRINKET_RENDERER);
    }

    public static class NekoTailTrinketRenderer extends NekoArmorRenderer.NekoTailRenderer implements TrinketRenderer {
        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, MatrixStack poseStack, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            // 使用 GeckoLib 渲染模型
            try {
                PlayerEntityModel<AbstractClientPlayerEntity> playerModel = (PlayerEntityModel<AbstractClientPlayerEntity>) model;
                AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
                TrinketRenderer.translateToChest(poseStack,playerModel,player);
                super.render(poseStack, vertexConsumers.getBuffer(RenderLayer.getEntitySolid(Registries.ITEM.getId(item.getItem()))), light,
                        OverlayTexture.DEFAULT_UV, 1);
                ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
                itemRenderer.renderItem(player,this.currentStack, ModelTransformationMode.FIXED, false, poseStack, vertexConsumers,player.getWorld(),light,OverlayTexture.DEFAULT_UV,123);
            }catch (Exception ignored){}
        }

        @Override
        public void setAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            try {
                super.setAngles((LivingEntity) entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            }catch (Exception ignored){}
        }
    }

    public static class NekoEarsTrinketRenderer extends NekoArmorRenderer.NekoEarsRenderer implements TrinketRenderer {
        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel, MatrixStack poseStack, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            // 使用 GeckoLib 渲染模型
            try {
                super.render(poseStack, vertexConsumers.getBuffer(RenderLayer.getEntitySolid(Registries.ITEM.getId(item.getItem()))), light,
                        OverlayTexture.DEFAULT_UV, 1);
            }catch (Exception ignored){}
        }

        @Override
        public void setAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            try {
                super.setAngles((LivingEntity) entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            }catch (Exception ignored){}
        }
    }
}
