package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

/**
 * 滞留型猫薄荷水：喷溅型猫薄荷水 + 龙息在酿造台酿造。
 *
 * <p>投掷行为直接复用原版 {@link LingeringPotionItem}，瓶身材质用原版
 * {@code minecraft:item/lingering_potion} + {@code minecraft:item/potion_overlay}。
 */
public class CatnipLingeringItem extends LingeringPotionItem {
    public static final String ID = "catnip_lingering";

    public CatnipLingeringItem() {
        super(NekoIds.itemProps(ID).stacksTo(1));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(DataComponents.POTION_CONTENTS, CatnipBrewing.CONTENTS);
        return stack;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }
}
