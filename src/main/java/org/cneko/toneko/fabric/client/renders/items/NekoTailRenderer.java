package org.cneko.toneko.fabric.client.renders.items;

import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.items.NekoTail;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class NekoTailRenderer extends GeoArmorRenderer<NekoTail> {
    public NekoTailRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.of(MODID, "armor/neko_tail"))); // Using DefaultedItemGeoModel like this puts our 'location' as item/armor/example armor in the assets folders.
    }
}
