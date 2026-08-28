package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import org.cneko.toneko.common.mod.entities.SpoiledWaterProjectile;

/**
 * 变质水投掷物渲染器：把喷溅/滞留水瓶画成旋转的小瓶子。
 * 26.x 使用原版 ThrownItemRenderer 的 render state 管线；实体实现 ItemSupplier 提供物品。
 */
public class SpoiledWaterProjectileRenderer extends ThrownItemRenderer<SpoiledWaterProjectile> {

    public SpoiledWaterProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, 0.25f, true);
    }
}
