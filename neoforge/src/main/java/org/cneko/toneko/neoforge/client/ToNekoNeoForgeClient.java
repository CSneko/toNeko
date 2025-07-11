package org.cneko.toneko.neoforge.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.events.ClientNetworkEvents;
import org.cneko.toneko.common.mod.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.client.events.HudRenderEvent;
import org.cneko.toneko.common.mod.client.renderers.AmmunitionRenderer;
import org.cneko.toneko.common.mod.client.renderers.GhostNekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoBossRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.screens.ConfigScreen;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;
import org.cneko.toneko.neoforge.items.ToNekoBlocks;

import static org.cneko.toneko.common.Bootstrap.MODID;

@Environment(EnvType.CLIENT)
@Mod(value = MODID, dist = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ToNekoNeoForgeClient {

    public ToNekoNeoForgeClient(IEventBus bus, ModContainer container){
        bus.addListener(ToNekoNeoForgeClient::registerEntityRenderers);
        bus.addListener(ToNekoNeoForgeClient::registerBlockLayerRenderers);
        ClientNetworkEvents.init();
        ClientPlayerJoinEvent.init();
        ClientTickEvent.init();
        HudRenderEvent.init();
        ToNekoKeyBindings.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (a,b)->new ConfigScreen());

        org.cneko.toneko.common.mod.client.ToNekoClient.init();
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(
                ToNekoEntities.ADVENTURER_NEKO_HOLDER.get(),
                NekoRenderer::new
        );
        event.registerEntityRenderer(
                ToNekoEntities.CRYSTAL_NEKO_HOLDER.get(),
                NekoRenderer::new
        );
        event.registerEntityRenderer(
                ToNekoEntities.GHOST_NEKO_HOLDER.get(),
                GhostNekoRenderer::new
        );
        event.registerEntityRenderer(
                ToNekoEntities.FIGHTING_NEKO_HOLDER.get(),
                NekoRenderer::new
        );
        event.registerEntityRenderer(
                ToNekoEntities.AMMUNITION_ENTITY_HOLDER.get(),
                AmmunitionRenderer::new
        );
        event.registerEntityRenderer(
                ToNekoEntities.MOUFLET_NEKO_BOSS_HOLDER.get(),
                NekoBossRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerBlockLayerRenderers(RegisterNamedRenderTypesEvent event){
        // TODO
        event.register(ToNekoBlocks.CATNIP_HOLDER.getId(),RenderType.cutout(), RenderType.entityCutout(ToNekoBlocks.CATNIP_HOLDER.getId()));
    }


}
