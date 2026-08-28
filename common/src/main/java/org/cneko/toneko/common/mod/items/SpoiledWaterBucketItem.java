package org.cneko.toneko.common.mod.items;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.item.Item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluids;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;

import java.util.List;

/**
 * 变质水桶：装着从炼药锅里舀出的「丝袜水」。
 * 水的变质程度（spoilage 0~100）与气味来源（wearer）存在组件中，倒水时行为与原版水桶一致。
 */
public class SpoiledWaterBucketItem extends BucketItem {
    public static final String ID = "spoiled_water_bucket";
    public static final int MAX_SPOILAGE = ScentedWaterUtil.MAX_SPOILAGE;

    public SpoiledWaterBucketItem() {
        super(Fluids.WATER, NekoIds.itemProps(ID)
                .stacksTo(1)
                .component(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT, 0)
                .component(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, ""));
    }

    public static int getSpoilage(ItemStack stack) {
        return ScentedWaterUtil.getSpoilage(stack);
    }

    public static String getWearer(ItemStack stack) {
        return ScentedWaterUtil.getWearer(stack);
    }

    public static ItemStack create(int spoilage, String wearer) {
        ItemStack stack = new ItemStack(ToNekoItems.SPOILED_WATER_BUCKET);
        ScentedWaterUtil.setSpoilage(stack, spoilage, wearer);
        return stack;
    }

    /** 变质等级 lang 后缀：fresh / slight / moderate / heavy / foul / overwhelming */
    public static String grade(int spoilage) {
        return ScentedWaterUtil.grade(spoilage);
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        int spoilage = getSpoilage(stack);
        tooltips.add(Component.translatable("item.toneko.spoiled_water_bucket.tip.spoilage",
                Component.translatable("item.toneko.spoiled_water_bucket.spoilage." + grade(spoilage))));
        String wearer = getWearer(stack);
        if (!wearer.isEmpty()) {
            tooltips.add(Component.translatable("item.toneko.spoiled_water.tip.wearer", wearer));
        }
    
        for (Component t : tooltips) adder.accept(t);
    }
}
