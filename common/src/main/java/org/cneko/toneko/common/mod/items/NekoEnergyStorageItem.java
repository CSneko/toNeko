package org.cneko.toneko.common.mod.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NekoEnergyStorageItem extends Item {
    @Getter
    private double energyCapacity;
    @Getter
    private boolean isCharged;
    public NekoEnergyStorageItem(String idPath, double energyCapacity, boolean isCharged) {
        super(NekoIds.itemProps(idPath));
        this.energyCapacity = energyCapacity;
        this.isCharged = isCharged;
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        tooltips.add(Component.translatable("item.toneko.neko_energy_storage.tip.energy_count",energyCapacity));
        if (isCharged) {
            tooltips.add(Component.translatable("item.toneko.neko_energy_storage.tip.charged"));
        } else {
            tooltips.add(Component.translatable("item.toneko.neko_energy_storage.tip.uncharged"));
        }
    
        for (Component t : tooltips) adder.accept(t);
    }

    // 附魔光效
    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isCharged || super.isFoil(stack);
    }
}
