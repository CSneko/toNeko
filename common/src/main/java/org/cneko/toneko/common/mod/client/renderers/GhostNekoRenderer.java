package org.cneko.toneko.common.mod.client.renderers;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class GhostNekoRenderer extends NekoRenderer<GhostNekoEntity> {
    public GhostNekoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, GhostNekoEntity entity, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        RenderType translucentRenderType = RenderType.entityTranslucent(getTextureLocation(entity));
        VertexConsumer translucentBuffer = bufferSource.getBuffer(translucentRenderType);

        // 调用父类进行实际渲染
        super.actuallyRender(poseStack, entity, model, translucentRenderType, bufferSource, translucentBuffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }


}
