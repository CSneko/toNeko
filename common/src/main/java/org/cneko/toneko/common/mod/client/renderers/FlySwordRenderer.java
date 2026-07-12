package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

        // Sword-flying pose: tilt forward 45°, lay flat
        pose.mulPose(Axis.XP.rotationDegrees(45));   // tilt forward
        pose.mulPose(Axis.YP.rotationDegrees(-90));  // lay flat (like a surfboard)

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

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlySwordEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/diamond_block.png");
    }
}
