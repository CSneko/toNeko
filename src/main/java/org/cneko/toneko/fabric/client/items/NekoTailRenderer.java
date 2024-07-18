package org.cneko.toneko.fabric.client.items;


import org.cneko.toneko.fabric.items.NekoTailItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;


public class NekoTailRenderer extends GeoArmorRenderer<NekoTailItem> {
    public NekoTailRenderer() {
        super(new NekoTailModel()); // Using DefaultedItemGeoModel like this puts our 'location' as item/armor/example armor in the assets folders.
    }
}
