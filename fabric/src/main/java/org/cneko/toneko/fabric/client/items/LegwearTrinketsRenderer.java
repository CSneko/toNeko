package org.cneko.toneko.fabric.client.items;

import com.mojang.blaze3d.vertex.PoseStack;
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
import org.cneko.toneko.common.mod.client.items.LegwearRenderer;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

/**
 * Trinkets 渲染器：手动驱动 LegwearRenderer（透肉/左右染色/长度隐藏全由 currentStack 组件决定）。
 * 每个物品一个渲染器，槽位固定 EquipmentSlot.LEGS（照 NekoArmorTrinketsRenderer 模式）。
 */
public class LegwearTrinketsRenderer {
    public static Pantyhose40DRenderer PANTYHOSE_40D_RENDERER;
    public static Pantyhose20DRenderer PANTYHOSE_20D_RENDERER;
    public static Pantyhose5DRenderer PANTYHOSE_5D_RENDERER;
    public static OverKneeRenderer OVER_KNEE_RENDERER;

    public static void init() {
        LOGGER.info("Registering LegwearTrinketsRenderer");
        PANTYHOSE_40D_RENDERER = new Pantyhose40DRenderer();
        PANTYHOSE_20D_RENDERER = new Pantyhose20DRenderer();
        PANTYHOSE_5D_RENDERER = new Pantyhose5DRenderer();
        OVER_KNEE_RENDERER = new OverKneeRenderer();
        TrinketRendererRegistry.registerRenderer(LEGWEAR_PANTYHOSE_40D, PANTYHOSE_40D_RENDERER);
        TrinketRendererRegistry.registerRenderer(LEGWEAR_PANTYHOSE_20D, PANTYHOSE_20D_RENDERER);
        TrinketRendererRegistry.registerRenderer(LEGWEAR_PANTYHOSE_5D, PANTYHOSE_5D_RENDERER);
        TrinketRendererRegistry.registerRenderer(LEGWEAR_OVER_KNEE, OVER_KNEE_RENDERER);
    }

    public static class Pantyhose40DRenderer implements TrinketRenderer {
        public LegwearRenderer renderer;

        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                renderer = (LegwearRenderer) LEGWEAR_PANTYHOSE_40D.getRenderer();
                renderer.setItemStack(item);
                renderer.setEntity(entity);
                renderer.setBaseModel((HumanoidModel<?>) model);
                renderer.setSlot(EquipmentSlot.LEGS);
                renderer.setAnimatable(LEGWEAR_PANTYHOSE_40D);
                poseStack.pushPose();
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(LEGWEAR_PANTYHOSE_40D))), light, OverlayTexture.NO_OVERLAY, 1);
            } catch (Exception ignored) {
            } finally {
                poseStack.popPose();
            }
        }
    }

    public static class Pantyhose20DRenderer implements TrinketRenderer {
        public LegwearRenderer renderer;

        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                renderer = (LegwearRenderer) LEGWEAR_PANTYHOSE_20D.getRenderer();
                renderer.setItemStack(item);
                renderer.setEntity(entity);
                renderer.setBaseModel((HumanoidModel<?>) model);
                renderer.setSlot(EquipmentSlot.LEGS);
                renderer.setAnimatable(LEGWEAR_PANTYHOSE_20D);
                poseStack.pushPose();
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(LEGWEAR_PANTYHOSE_20D))), light, OverlayTexture.NO_OVERLAY, 1);
            } catch (Exception ignored) {
            } finally {
                poseStack.popPose();
            }
        }
    }

    public static class Pantyhose5DRenderer implements TrinketRenderer {
        public LegwearRenderer renderer;

        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                renderer = (LegwearRenderer) LEGWEAR_PANTYHOSE_5D.getRenderer();
                renderer.setItemStack(item);
                renderer.setEntity(entity);
                renderer.setBaseModel((HumanoidModel<?>) model);
                renderer.setSlot(EquipmentSlot.LEGS);
                renderer.setAnimatable(LEGWEAR_PANTYHOSE_5D);
                poseStack.pushPose();
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(LEGWEAR_PANTYHOSE_5D))), light, OverlayTexture.NO_OVERLAY, 1);
            } catch (Exception ignored) {
            } finally {
                poseStack.popPose();
            }
        }
    }

    public static class OverKneeRenderer implements TrinketRenderer {
        public LegwearRenderer renderer;

        @Override
        public void render(ItemStack item, SlotReference slotReference, EntityModel<? extends LivingEntity> model, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            try {
                renderer = (LegwearRenderer) LEGWEAR_OVER_KNEE.getRenderer();
                renderer.setItemStack(item);
                renderer.setEntity(entity);
                renderer.setBaseModel((HumanoidModel<?>) model);
                renderer.setSlot(EquipmentSlot.LEGS);
                renderer.setAnimatable(LEGWEAR_OVER_KNEE);
                poseStack.pushPose();
                renderer.renderToBuffer(poseStack, vertexConsumers.getBuffer(RenderType.entityTranslucent(BuiltInRegistries.ITEM.getKey(LEGWEAR_OVER_KNEE))), light, OverlayTexture.NO_OVERLAY, 1);
            } catch (Exception ignored) {
            } finally {
                poseStack.popPose();
            }
        }
    }
}
