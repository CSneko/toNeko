package org.cneko.toneko.neoforge.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.neoforge.ToNekoNeoForge;
import org.cneko.toneko.neoforge.fabric.client.ToNekoClient;
import org.cneko.toneko.neoforge.fabric.entities.ToNekoEntities;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;

@Environment(EnvType.CLIENT)
@Mod(value = MODID, dist = Dist.CLIENT)
public class ToNekoNeoForgeClient {

    public ToNekoNeoForgeClient(IEventBus bus, ModContainer container){
        new ToNekoClient().onInitializeClient();
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
