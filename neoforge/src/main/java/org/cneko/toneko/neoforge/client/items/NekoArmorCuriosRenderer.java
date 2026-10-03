package org.cneko.toneko.neoforge.client.items;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_OVER_KNEE_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_PANTYHOSE_20D_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_PANTYHOSE_40D_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_PANTYHOSE_5D_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.NEKO_EARS_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.NEKO_PAWS_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.NEKO_TAIL_HOLDER;

/**
 * Curios 饰品客户端渲染（NeoForge）。
 * <p>
 * Curios 15（26.1.2）的 {@link ICurioRenderer} 已改为渲染状态（RenderState）管线，
 * 与 GeckoLib 26.x 的 GeoArmorRenderer 对接需要重写（Fabric 侧 Trinkets 集成同样暂停）。
 * 当前注册默认渲染器，保证饰品槽可用；饰品槽上的 GeckoLib 视觉效果留待后续恢复。
 */
@OnlyIn(Dist.CLIENT)
public final class NekoArmorCuriosRenderer {
    private NekoArmorCuriosRenderer() {}

    /** 在 FMLClientSetupEvent#enqueueWork 中调用 */
    public static void init() {
        LOGGER.info("Registering NekoArmor & Legwear Curios renderers (default renderer, 26.x render-state API)");
        CuriosRendererRegistry.register(NEKO_EARS_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
        CuriosRendererRegistry.register(NEKO_TAIL_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
        CuriosRendererRegistry.register(NEKO_PAWS_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
        CuriosRendererRegistry.register(LEGWEAR_PANTYHOSE_40D_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
        CuriosRendererRegistry.register(LEGWEAR_PANTYHOSE_20D_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
        CuriosRendererRegistry.register(LEGWEAR_PANTYHOSE_5D_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
        CuriosRendererRegistry.register(LEGWEAR_OVER_KNEE_HOLDER.get(), () -> ICurioRenderer.DEFAULT);
    }
}
