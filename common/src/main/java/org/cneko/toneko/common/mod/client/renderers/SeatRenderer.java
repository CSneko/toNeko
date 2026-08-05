package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.entities.SeatEntity;

/**
 * 座椅实体的空渲染器。
 * SeatEntity 本身是不可见的，但 Iris 等光影 mod 在渲染阴影时
 * 仍需要找到渲染器，否则会 NPE 崩溃。
 */
public class SeatRenderer extends EntityRenderer<SeatEntity> {

    public SeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        // 不可见，永远不会被调用到
        return null;
    }
}
