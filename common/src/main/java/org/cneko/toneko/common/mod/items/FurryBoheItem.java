package org.cneko.toneko.common.mod.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import static org.cneko.toneko.common.mod.items.ToNekoItems.key;

public class FurryBoheItem extends Item {
    public static final String ID = "furry_bohe";

    public FurryBoheItem() {
        super(new Properties().setId(key(ID)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable("item.toneko.furry_bohe.info"));
    }
}
