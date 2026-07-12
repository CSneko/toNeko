package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.toneko.common.mod.entities.FlySwordEntity;
import org.jetbrains.annotations.NotNull;

public class FlySwordRenderer extends EntityRenderer<FlySwordEntity> {

    public FlySwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(FlySwordEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0, 0.2, 0);

        float yawOffset = entity.getYawOffset(partialTick);
        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw + yawOffset));
        float pitch = entity.getPitch(partialTick);
        float roll = entity.getRoll(partialTick);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));

        boolean isMinecart = entity.isSyncedMinecartMode();

        // Render minecart upright in world space BEFORE surfboard tilt
        if (isMinecart) {
            renderMinecartUpright(entity, yaw, yawOffset, partialTick, pose, buffer, packedLight);
        }

        // Sword-flying pose: tilt forward 45°, lay flat (board underneath)
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(45));   // tilt forward
        pose.mulPose(Axis.YP.rotationDegrees(-90));  // lay flat (like a surfboard)

        if (isMinecart) {
            // Render board/diamond block as the base under the minecart
            pose.translate(-0.5, 0, -0.5);
            BlockState state = entity.getBlockState();
            RenderShape shape = state.getRenderShape();
            if (shape != RenderShape.MODEL && shape != RenderShape.ENTITYBLOCK_ANIMATED) {
                state = Blocks.DIAMOND_BLOCK.defaultBlockState();
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    state, pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        } else {
            String itemId = entity.getDisplayItem();
            if (!itemId.isEmpty()) {
                // Render item model
                ResourceLocation rl = ResourceLocation.tryParse(itemId);
                if (rl != null) {
                    var item = BuiltInRegistries.ITEM.getOptional(rl);
                    if (item.isPresent()) {
                        ItemStack stack = new ItemStack(item.get());
                        pose.translate(0, 0.15, 0);
                        pose.scale(1.2f, 1.2f, 1.2f);
                        Minecraft.getInstance().getItemRenderer().renderStatic(
                                stack, ItemDisplayContext.GROUND,
                                packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), 0);
                    }
                }
            } else {
                // Render block
                pose.translate(-0.5, 0, -0.5);
                BlockState state = entity.getBlockState();
                RenderShape shape = state.getRenderShape();
                if (shape != RenderShape.MODEL && shape != RenderShape.ENTITYBLOCK_ANIMATED) {
                    state = Blocks.DIAMOND_BLOCK.defaultBlockState();
                }
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                        state, pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }

        pose.popPose(); // end surfboard
        pose.popPose(); // end main
        super.render(entity, yaw, partialTick, pose, buffer, packedLight);
    }

    private void renderMinecartUpright(FlySwordEntity entity, float yaw, float yawOffset,
                                        float partialTick, PoseStack pose, MultiBufferSource buffer,
                                        int packedLight) {
        AbstractMinecart minecart = entity.getOrCreateRenderMinecart();
        if (minecart == null) return;

        minecart.setPos(entity.getX(), entity.getY(), entity.getZ());
        // Set yRot to match entity direction — the entity renderer will apply
        // YP(180 - yRot) which combines with outer pose YP(180 - yaw + yawOffset)
        // Use yRot = yaw - yawOffset so renderer adds YP(180 - yaw + yawOffset)
        // But that doubles with outer pose... Instead set yRot=180 so renderer
        // applies YP(0) = no rotation, letting outer pose handle facing entirely
        minecart.yRotO = 180;
        minecart.setYRot(180);

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super AbstractMinecart> renderer = dispatcher.getRenderer(minecart);

        if (renderer != null) {
            pose.pushPose();
            // Position minecart above the board
            pose.translate(0.0, 0.55, 0.0);
            pose.scale(1.8f, 1.8f, 1.8f);
            // yaw=180 so renderer does YP(180-180)=YP(0) — no extra rotation
            renderer.render(minecart, 180, partialTick, pose, buffer, packedLight);
            pose.popPose();
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlySwordEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/diamond_block.png");
    }
}
