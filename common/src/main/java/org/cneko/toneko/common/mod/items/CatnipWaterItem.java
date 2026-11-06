package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 猫薄荷水：由「水瓶 + 猫薄荷」在酿造台酿造。
 *
 * <p>喝下后获得「兴奋 II」并恢复 30 点猫娘能量（和猫薄荷本体一致）。效果在
 * {@link #finishUsingItem} 里挂，而不是写进默认 {@code POTION_CONTENTS}——原因见
 * {@link CatnipBrewing} 的类注释（默认组件在物品构造阶段拿不到模组效果）。
 *
 * <p>瓶身材质复用原版 {@code minecraft:item/potion} + {@code minecraft:item/potion_overlay}，
 * 液体颜色由 {@link CatnipBrewing#COLOR} 决定。
 */
public class CatnipWaterItem extends PotionItem {
    public static final String ID = "catnip_water";

    /** 喝一瓶恢复的猫娘能量 */
    private static final float ENERGY_PER_BOTTLE = 30f;

    public CatnipWaterItem() {
        // 饮用行为由 CONSUMABLE 组件驱动；usingConvertsTo 让喝完自动返还玻璃瓶（同原版药水）
        super(NekoIds.itemProps(ID).stacksTo(1)
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .component(DataComponents.POTION_CONTENTS, CatnipBrewing.baseContents())
                .usingConvertsTo(Items.GLASS_BOTTLE));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(DataComponents.POTION_CONTENTS, CatnipBrewing.baseContents());
        return stack;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            MobEffectInstance excitement = CatnipBrewing.excitementInstance();
            if (excitement != null) {
                player.addEffect(excitement);
            }
            if (player instanceof INeko neko) {
                neko.setNekoEnergy(neko.getNekoEnergy() + ENERGY_PER_BOTTLE);
            }
        }
        // 扣瓶、返还玻璃瓶、统计/进度都交给原版 CONSUMABLE 流程
        return super.finishUsingItem(stack, level, user);
    }

    /**
     * {@link PotionItem#getName} 会用药水名拼出「Water Bottle」之类的名字，
     * 这里固定用物品自己的翻译键（猫薄荷水）。
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context,
                                @NotNull TooltipDisplay display, @NotNull Consumer<Component> adder,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(Component.translatable("item.toneko.catnip_water.tip"));
    }
}
