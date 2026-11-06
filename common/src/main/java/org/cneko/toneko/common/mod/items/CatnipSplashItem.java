package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

/**
 * 喷溅型猫薄荷水：猫薄荷水 + 火药在酿造台酿造。
 *
 * <p>投掷行为直接复用原版 {@link SplashPotionItem}，瓶身材质用原版
 * {@code minecraft:item/splash_potion} + {@code minecraft:item/potion_overlay}。
 */
public class CatnipSplashItem extends SplashPotionItem {
    public static final String ID = "catnip_splash";

    public CatnipSplashItem() {
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
