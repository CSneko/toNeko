package org.cneko.toneko.neoforge.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.events.ClientNetworkEvents;
import org.cneko.toneko.common.mod.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;

import static org.cneko.toneko.common.Bootstrap.MODID;

@Environment(EnvType.CLIENT)
@Mod(value = MODID, dist = Dist.CLIENT)
public class ToNekoNeoForgeClient {

    public ToNekoNeoForgeClient(IEventBus bus, ModContainer container){
        bus.addListener(ToNekoNeoForgeClient::registerEntityRenderers);
        ClientNetworkEvents.init();
        ClientPlayerJoinEvent.init();
        ClientTickEvent.init();
        ToNekoKeyBindings.init();
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
    }


}
