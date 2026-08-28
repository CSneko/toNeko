package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.blocks.ToNekoBlockEntities;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.events.HudRenderEvent;
import org.cneko.toneko.common.mod.client.renderers.AmmunitionRenderer;
import org.cneko.toneko.common.mod.client.renderers.ClotheslineBlockEntityRenderer;
import org.cneko.toneko.common.mod.client.renderers.FlySwordRenderer;
import org.cneko.toneko.common.mod.client.renderers.GhostNekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoBossRenderer;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.SeatRenderer;
import org.cneko.toneko.common.mod.client.renderers.SpoiledWaterProjectileRenderer;
import org.cneko.toneko.common.mod.client.events.ClientNetworkEvents;
import org.cneko.toneko.common.mod.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.client.events.LegwearRustleHandler;
import org.cneko.toneko.common.mod.client.events.LegwearWetDripHandler;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.fabric.client.model.ShengDengModelHook;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToNekoKeyBindings.init();
        ClientNetworkEvents.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
        HudRenderEvent.init();
        ShengDengModelHook.register();
        LegwearRustleHandler.init();
        LegwearWetDripHandler.init();
        // 26.x 迁移说明：Trinkets 尚无 26.x 版本，其渲染器注册已随集成类一同移除。
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
        EntityRendererRegistry.register(ToNekoEntities.SPOILED_WATER_PROJECTILE_ENTITY, SpoiledWaterProjectileRenderer::new);

        // 26.x：Fabric blockrenderlayer.v1 API 已移除；透明层改由模型 JSON 的 render_type 字段声明
        // （assets/toneko/models/block/*.json 中 "render_type": "minecraft:cutout"）。

        BlockEntityRenderers.register(ToNekoBlockEntities.CLOTHESLINE, ClotheslineBlockEntityRenderer::new);

        // 省凳法棍：26.x 起通过 AfterBakeItem 模型钩子接入 SpecialModelWrapper
        // （详见 ShengDengSpecialModel），不再需要 BuiltinItemRendererRegistry。

        org.cneko.toneko.common.mod.client.ToNekoClient.init();
    }
}
