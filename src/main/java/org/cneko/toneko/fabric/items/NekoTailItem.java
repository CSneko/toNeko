package org.cneko.toneko.fabric.items;

import org.cneko.toneko.fabric.client.items.NekoTailRenderer;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class NekoTailItem extends NekoArmor<NekoTailItem> {
    public static final String ID = "neko_tail";
    public NekoTailItem() {
        super(ToNekoArmorMaterials.NEKO,Type.CHESTPLATE,new Settings().maxCount(1));
    }


    @Override
    public GeoArmorRenderer<NekoTailItem> getRenderer() {
        return new NekoTailRenderer();
    }
}
