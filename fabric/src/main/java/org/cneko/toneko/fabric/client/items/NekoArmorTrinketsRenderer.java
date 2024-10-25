package org.cneko.toneko.fabric.client.items;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

import com.mojang.blaze3d.vertex.PoseStack;
import org.cneko.toneko.common.mod.client.items.NekoArmorRenderer;

public class NekoArmorTrinketsRenderer {
    public static NekoTailTrinketRenderer NEKO_TAIL_TRINKET_RENDERER;
    public static NekoEarsTrinketRenderer NEKO_EARS_TRINKET_RENDERER;
    public static NekoPawsTrinketRenderer NEKO_PAWS_TRINKET_RENDERER;
    public static void init(){
        LOGGER.info("Registering NekoArmorTrinketsRenderer");
        NEKO_TAIL_TRINKET_RENDERER = new NekoTailTrinketRenderer();
        NEKO_EARS_TRINKET_RENDERER = new NekoEarsTrinketRenderer();
        NEKO_PAWS_TRINKET_RENDERER = new NekoPawsTrinketRenderer();
        TrinketRendererRegistry.registerRenderer(NEKO_TAIL,NEKO_TAIL_TRINKET_RENDERER);
        TrinketRendererRegistry.registerRenderer(NEKO_TAIL,NEKO_TAIL_TRINKET_RENDERER);
        TrinketRendererRegistry.registerRenderer(NEKO_EARS,NEKO_EARS_TRINKET_RENDERER);
        TrinketRendererRegistry.registerRenderer(NEKO_PAWS,NEKO_PAWS_TRINKET_RENDERER);
    }


    public static class NekoTailTrinketRenderer implements TrinketRenderer {
        public NekoArmorRenderer.NekoTailRenderer renderer;

        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                //if (renderer == null){
                    renderer = NEKO_TAIL.getRenderer();
                    renderer.setItemStack(item);
                    renderer.setEntity(entity);
                    renderer.setBaseModel((HumanoidModel<?>) model);
                    renderer.setSlot(EquipmentSlot.CHEST);
                //}
                renderer.setAnimatable(NEKO_TAIL);
                // 保存当前矩阵状态
                poseStack.pushPose();

                // 执行渲染
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(NEKO_TAIL))), light, OverlayTexture.NO_OVERLAY, 1);
            } catch (Exception ignored) {} finally {
                // 恢复之前的矩阵状态
                poseStack.popPose();
            }
        }

    }

    public static class NekoEarsTrinketRenderer implements TrinketRenderer {
        public NekoArmorRenderer.NekoEarsRenderer renderer;
        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                //if (renderer == null){
                    renderer = NEKO_EARS.getRenderer();
                    renderer.setItemStack(item);
                    renderer.setEntity(entity);
                    renderer.setBaseModel((HumanoidModel<?>) model);
                    renderer.setSlot(EquipmentSlot.HEAD);
                //}
                renderer.setAnimatable(NEKO_EARS);
                // 保存当前矩阵状态
                poseStack.pushPose();

                // 执行渲染
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(NEKO_EARS))), light, OverlayTexture.NO_OVERLAY, 1);

            } catch (Exception ignored) {} finally {
                // 恢复之前的矩阵状态
                poseStack.popPose();
            }
        }
    }
    public static class NekoPawsTrinketRenderer implements TrinketRenderer {
        public NekoArmorRenderer.NekoPawsRenderer renderer;
        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

            try {
                //if (renderer == null){
                renderer = NEKO_PAWS.getRenderer();
                renderer.setItemStack(item);
                renderer.setEntity(entity);
                renderer.setBaseModel((HumanoidModel<?>) model);
                renderer.setSlot(EquipmentSlot.FEET);
                //}
                renderer.setAnimatable(NEKO_PAWS);
                // 保存当前矩阵状态
                poseStack.pushPose();

                // 执行渲染
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(NEKO_PAWS))), light, OverlayTexture.NO_OVERLAY, 1);

            } catch (Exception ignored) {} finally {
                // 恢复之前的矩阵状态
                poseStack.popPose();
            }
        }
    }
}
