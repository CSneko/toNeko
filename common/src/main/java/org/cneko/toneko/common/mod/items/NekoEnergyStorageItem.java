package org.cneko.toneko.common.mod.items;

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
    public NekoEnergyStorageItem(double energyCapacity,boolean isCharged) {
        super(new Properties());
        this.energyCapacity = energyCapacity;
        this.isCharged = isCharged;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.toneko.neko_energy_storage.tip.energy_count",energyCapacity));
        if (isCharged) {
            tooltipComponents.add(Component.translatable("item.toneko.neko_energy_storage.tip.charged"));
        } else {
            tooltipComponents.add(Component.translatable("item.toneko.neko_energy_storage.tip.uncharged"));
        }
    }

    // 附魔光效
    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isCharged || super.isFoil(stack);
    }
}
