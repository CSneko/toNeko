package com.crystalneko.tonekofabric.entity.neko;

import com.crystalneko.tonekofabric.client.ToNekoFabricClient;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class nekoRender extends MobEntityRenderer<nekoEntity, nekoModel> {
    public nekoRender(EntityRendererFactory.Context context) {
        super(context, new nekoModel(context.getPart(ToNekoFabricClient.MODEL_NEKO_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(nekoEntity entity) {
        return new Identifier("toneko", "textures/entity/neko/neko.png");
    }
}
