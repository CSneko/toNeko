package org.cneko.toneko.common.mod.items;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FurryBoheItem extends Item {
    public static final String ID = "furry_bohe";

    public FurryBoheItem() {
        super(NekoIds.itemProps(ID));
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();

        tooltips.add(Component.translatable("item.toneko.furry_bohe.info"));
    
        for (Component t : tooltips) adder.accept(t);
    }
}
