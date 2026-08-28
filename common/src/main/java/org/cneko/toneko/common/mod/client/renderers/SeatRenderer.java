package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.cneko.toneko.common.mod.entities.SeatEntity;

/**
 * 座椅实体的空渲染器。
 * SeatEntity 本身是不可见的，但 Iris 等光影 mod 在渲染阴影时
 * 仍需要找到渲染器，否则会 NPE 崩溃。
 */
public class SeatRenderer extends NoopRenderer<SeatEntity> {

    public SeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
