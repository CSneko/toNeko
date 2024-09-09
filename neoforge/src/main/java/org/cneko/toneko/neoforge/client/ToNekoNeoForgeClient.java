package org.cneko.toneko.neoforge.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import static org.cneko.toneko.common.Bootstrap.MODID;

@Environment(EnvType.CLIENT)
@Mod(value = MODID, dist = Dist.CLIENT)
public class ToNekoNeoForgeClient {

    public ToNekoNeoForgeClient(IEventBus bus, ModContainer container){
//        new ToNekoClient().onInitializeClient();
        // TODO: 渲染问题我解决不掉
        bus.addListener(ToNekoNeoForgeClient::registerEntityRenderers);

    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event){
//        event.registerEntityRenderer(
//                ToNekoEntities.ADVENTURER_NEKO,
//                NekoRenderer::new
//        );
    }


}
