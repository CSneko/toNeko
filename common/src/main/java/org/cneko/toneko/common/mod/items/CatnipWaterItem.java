package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.component.Consumables;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

/**
 * 猫薄荷水：由「水瓶 + 猫薄荷」在酿造台酿造，可饮用（效果后续再加）。
 *
 * <p>瓶身材质复用原版的 {@code minecraft:item/potion} + {@code minecraft:item/potion_overlay}，
 * 液体颜色由 {@link CatnipBrewing#CONTENTS} 里的 customColor 决定。
 */
public class CatnipWaterItem extends PotionItem {
    public static final String ID = "catnip_water";

    public CatnipWaterItem() {
        // 饮用行为由 CONSUMABLE 组件驱动；usingConvertsTo 让喝完自动返还玻璃瓶（同原版药水）
        super(NekoIds.itemProps(ID).stacksTo(1)
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .usingConvertsTo(Items.GLASS_BOTTLE));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        // PotionItem 默认会塞一瓶「水」，这里换成猫薄荷水的内容（带绿色）
        ItemStack stack = super.getDefaultInstance();
        stack.set(DataComponents.POTION_CONTENTS, CatnipBrewing.CONTENTS);
        return stack;
    }

    /**
     * {@link PotionItem#getName} 会用药水名拼出「Water Bottle」之类的名字，
     * 这里固定用物品自己的翻译键（猫薄荷水）。
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }
}
