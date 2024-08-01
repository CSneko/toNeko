package org.cneko.toneko.fabric.client.items;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import software.bernie.geckolib.renderer.GeoRenderer;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.fabric.items.ToNekoItems.*;
public class NekoArmorTrinketsRenderer {
    public static NekoTailTrinketRenderer NEKO_TAIL_TRINKET_RENDERER;
    public static NekoEarsTrinketRenderer NEKO_EARS_TRINKET_RENDERER;
    public static void init(){
        LOGGER.info("Registering NekoArmorTrinketsRenderer");
        NEKO_TAIL_TRINKET_RENDERER = new NekoTailTrinketRenderer();
        NEKO_EARS_TRINKET_RENDERER = new NekoEarsTrinketRenderer();
        TrinketRendererRegistry.registerRenderer(NEKO_TAIL,NEKO_TAIL_TRINKET_RENDERER);
        TrinketRendererRegistry.registerRenderer(NEKO_EARS,NEKO_EARS_TRINKET_RENDERER);
    }


    public static class NekoTailTrinketRenderer implements TrinketRenderer {
        public NekoArmorRenderer.NekoTailRenderer renderer;

        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, MatrixStack poseStack, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                //if (renderer == null){
                    renderer = NEKO_TAIL.getRenderer();
                    renderer.setItemStack(item);
                    renderer.setEntity(entity);
                    renderer.setBaseModel((BipedEntityModel<?>) model);
                    renderer.setSlot(EquipmentSlot.CHEST);
                //}
                renderer.setAnimatable(NEKO_TAIL);
                // 保存当前矩阵状态
                poseStack.push();

                // 执行渲染
                renderer.render(poseStack, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(Registries.ITEM.getId(NEKO_TAIL))), light, OverlayTexture.DEFAULT_UV, 1);
            } catch (Exception ignored) {} finally {
                // 恢复之前的矩阵状态
                poseStack.pop();
            }
        }

    }

    public static class NekoEarsTrinketRenderer implements TrinketRenderer {
        public NekoArmorRenderer.NekoEarsRenderer renderer;
        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, MatrixStack poseStack, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                //if (renderer == null){
                    renderer = NEKO_EARS.getRenderer();
                    renderer.setItemStack(item);
                    renderer.setEntity(entity);
                    renderer.setBaseModel((BipedEntityModel<?>) model);
                    renderer.setSlot(EquipmentSlot.HEAD);
                //}
                renderer.setAnimatable(NEKO_EARS);
                // 保存当前矩阵状态
                poseStack.push();

                // 执行渲染
                renderer.render(poseStack, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(Registries.ITEM.getId(NEKO_EARS))), light, OverlayTexture.DEFAULT_UV, 1);

            } catch (Exception ignored) {} finally {
                // 恢复之前的矩阵状态
                poseStack.pop();
            }
        }
    }
}
