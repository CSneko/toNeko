package org.cneko.toneko.fabric.client.items;

import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.items.NekoTailItem;
import software.bernie.geckolib.model.GeoModel;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoTailModel extends GeoModel<NekoTailItem> {
    public Identifier modelResource = Identifier.of(MODID, "geo/neko_armor.geo.json");
    public Identifier textureResource = Identifier.of(MODID, "textures/armor/neko_armor.png");
    public Identifier animationResource = Identifier.of(MODID, "animations/neko_armor.animation.json");
    @Override
    public Identifier getModelResource(NekoTailItem animatable) {
        return modelResource;
    }

    @Override
    public Identifier getTextureResource(NekoTailItem animatable) {
        return textureResource;
    }

    @Override
    public Identifier getAnimationResource(NekoTailItem animatable) {
        return animationResource;
    }
}
