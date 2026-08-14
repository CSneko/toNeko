package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.events.HudRenderEvent;
import org.cneko.toneko.common.mod.client.renderers.AmmunitionRenderer;
import org.cneko.toneko.common.mod.client.renderers.FlySwordRenderer;
import org.cneko.toneko.common.mod.client.renderers.GhostNekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoBossRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.SeatRenderer;
import org.cneko.toneko.common.mod.client.renderers.ShengDengBewlr;
import org.cneko.toneko.common.mod.client.events.ClientNetworkEvents;
import org.cneko.toneko.common.mod.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.client.events.LegwearRustleHandler;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.fabric.client.items.LegwearTrinketsRenderer;
import org.cneko.toneko.fabric.client.items.NekoArmorTrinketsRenderer;
import org.cneko.toneko.fabric.items.ToNekoItems;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToNekoKeyBindings.init();
        ClientNetworkEvents.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
        HudRenderEvent.init();
        LegwearRustleHandler.init();
        // 注册trinkets渲染器
        if (ToNekoItems.isTrinketsInstalled){
            Minecraft.getInstance().execute(NekoArmorTrinketsRenderer::init);
            Minecraft.getInstance().execute(LegwearTrinketsRenderer::init);
        }
        EntityRendererRegistry.register(ToNekoEntities.ADVENTURER_NEKO, (EntityRendererProvider<? super AdventurerNeko>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.CRYSTAL_NEKO, (EntityRendererProvider<? super CrystalNekoEntity>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.GHOST_NEKO, (EntityRendererProvider<? super GhostNekoEntity>) GhostNekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.FIGHTING_NEKO, (EntityRendererProvider<? super FightingNekoEntity>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.AMMUNITION_ENTITY, (EntityRendererProvider<? super AmmunitionEntity>) AmmunitionRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.MOUFLET_NEKO_BOSS, (EntityRendererProvider<? super MoufletNekoBoss>) NekoBossRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.RAVENN_ENTITY, (EntityRendererProvider<? super RavennEntity>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.NOELLE_MAID_NEKO, (EntityRendererProvider<? super NoelleMaidNekoEntity>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.FLY_SWORD_ENTITY, FlySwordRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.SEAT_ENTITY, SeatRenderer::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ToNekoBlocks.CATNIP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ToNekoBlocks.WILD_CATNIP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ToNekoBlocks.SHENG_DENG, RenderType.cutout());

        // 省凳法棍：Fabric 侧注册动态渲染器（NeoForge 侧走 RegisterClientExtensionsEvent）。
        // 注意必须引用 common 的字段：fabric 的 ToNekoItems 只是静态导入它，
        // 而带类名前缀的写法不会解析静态导入；该字段在 fabric 平台已由注册流程赋值
        BuiltinItemRendererRegistry.INSTANCE.register(
                org.cneko.toneko.common.mod.items.ToNekoItems.SHENG_DENG_ITEM,
                ShengDengBewlr.INSTANCE::renderByItem);

        org.cneko.toneko.common.mod.client.ToNekoClient.init();
    }
}
