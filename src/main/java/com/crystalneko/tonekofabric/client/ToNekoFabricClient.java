package com.crystalneko.tonekofabric.client;

import com.crystalneko.tonekofabric.ToNekoFabric;
import com.crystalneko.tonekofabric.entity.neko.nekoModel;
import com.crystalneko.tonekofabric.entity.neko.nekoRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ToNekoFabricClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_NEKO_LAYER = new EntityModelLayer(new Identifier("toneko", "neko"), "main");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ToNekoFabric.NEKO, (context) -> {
            return new nekoRender(context);
        });
        EntityModelLayerRegistry.registerModelLayer(MODEL_NEKO_LAYER, nekoModel::getTexturedModelData);
    }


}
