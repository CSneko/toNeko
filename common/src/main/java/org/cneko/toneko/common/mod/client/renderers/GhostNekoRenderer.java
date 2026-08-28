package org.cneko.toneko.common.mod.client.renderers;


import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState;

/**
 * 幽灵猫娘渲染器：整只实体 60% 半透明。
 *
 * <h2>26.x / GeckoLib 5.5 迁移说明</h2>
 * 旧版覆写 actuallyRender / Color 类的做法随直接绘制管线一并移除：
 * 现在通过 {@link #getRenderType} 切到半透明管线、
 * {@link #getRenderColor} 注入 ARGB alpha（0x99 = 60% 不透明）实现同样效果。
 */
public class GhostNekoRenderer extends NekoRenderer<GhostNekoEntity> {
    public GhostNekoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public RenderType getRenderType(TonekoLivingEntityGeoState state, net.minecraft.resources.Identifier texture) {
        // 只换半透明管线
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public int getRenderColor(GhostNekoEntity animatable, Void unused, float partialTick) {
        // ARGB：0x99 = 60% 不透明
        return 0x99FFFFFF;
    }
}
