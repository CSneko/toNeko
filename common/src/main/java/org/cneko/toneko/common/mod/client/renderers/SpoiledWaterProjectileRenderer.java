package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.SpoiledWaterProjectile;
import org.jetbrains.annotations.NotNull;

/**
 * 变质水投掷物渲染器：把喷溅/滞留水瓶画成旋转的小瓶子，并补上 shadowRadius，
 * 避免 Iris 等光影在渲染实体阴影时因无 renderer 而 NPE。
 */
public class SpoiledWaterProjectileRenderer extends EntityRenderer<SpoiledWaterProjectile> {

    public SpoiledWaterProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25f;
    }

    @Override
    public void render(@NotNull SpoiledWaterProjectile entity, float yaw, float partialTick,
                       @NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int packedLight) {
        ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            pose.pushPose();
            pose.translate(0.0, 0.12, 0.0);
            float spin = (entity.tickCount + partialTick) * 18.0f;
            pose.mulPose(Axis.YP.rotationDegrees(spin));
            pose.scale(0.8f, 0.8f, 0.8f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), 0);
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpoiledWaterProjectile entity) {
        return ResourceLocation.withDefaultNamespace("textures/item/glass_bottle.png");
    }
}
