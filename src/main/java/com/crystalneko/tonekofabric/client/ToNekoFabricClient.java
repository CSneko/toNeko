package com.crystalneko.tonekofabric.client;

import com.crystalneko.tonekofabric.entity.nekoModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ToNekoFabricClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_NEKO_LAYER = new EntityModelLayer(new Identifier("toneko", "neko"), "main");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MODEL_NEKO_LAYER, nekoModel::getTexturedModelData);
    }


}
