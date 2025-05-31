package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.events.HudRenderEvent;
import org.cneko.toneko.common.mod.client.renderers.AmmunitionRenderer;
import org.cneko.toneko.common.mod.client.renderers.GhostNekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.events.ClientNetworkEvents;
import org.cneko.toneko.common.mod.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.entities.*;
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
        // 注册trinkets渲染器
        if (ToNekoItems.isTrinketsInstalled){
            Minecraft.getInstance().execute(NekoArmorTrinketsRenderer::init);
        }
        EntityRendererRegistry.register(ToNekoEntities.ADVENTURER_NEKO, (EntityRendererProvider<? super AdventurerNeko>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.CRYSTAL_NEKO, (EntityRendererProvider<? super CrystalNekoEntity>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.GHOST_NEKO, (EntityRendererProvider<? super GhostNekoEntity>) GhostNekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.FIGHTING_NEKO, (EntityRendererProvider<? super FightingNekoEntity>) NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.AMMUNITION_ENTITY, (EntityRendererProvider<? super AmmunitionEntity>) AmmunitionRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ToNekoBlocks.CATNIP, RenderType.cutout());

        org.cneko.toneko.common.mod.client.ToNekoClient.init();
    }
}
