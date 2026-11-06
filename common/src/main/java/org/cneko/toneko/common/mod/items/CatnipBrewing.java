package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;
import java.util.Optional;

/**
 * 猫薄荷水的酿造规则。
 *
 * <p>三条配方，全部在酿造台里完成（和原版药水一样）：
 * <ol>
 *   <li>{@code 水瓶 + 猫薄荷 → 猫薄荷水}；</li>
 *   <li>{@code 猫薄荷水 + 火药 → 喷溅型猫薄荷水}；</li>
 *   <li>{@code 喷溅型猫薄荷水 + 龙息 → 滞留型猫薄荷水}。</li>
 * </ol>
 *
 * <p>原版 {@code PotionBrewing} 只认识 {@code Items.POTION/SPLASH_POTION/LINGERING_POTION}，
 * 所以这三条配方由 {@code PotionBrewingMixin} 在 {@code isIngredient/hasMix/mix} 处注入。
 *
 * <p>暂时还没有实际效果，{@link PotionContents} 里只带一个绿色用于染色；效果后续再加。
 */
public final class CatnipBrewing {
    /** 猫薄荷水的颜色：取自猫薄荷物品贴图的主色（#88B008） */
    public static final int COLOR = 0x88B008;

    /**
     * 物品默认的 {@link PotionContents}。
     *
     * <p>药水部分暂时借用 {@link Potions#WATER}：它没有任何效果，但能让
     * {@code BrewingStandMenu.PotionSlot} 认为这是可以放进酿造槽的「药水」，
     * 同时颜色由 {@code customColor} 覆盖成猫薄荷绿。
     */
    public static final PotionContents CONTENTS = new PotionContents(
            Optional.of(Potions.WATER), Optional.of(COLOR), List.of(), Optional.empty());

    private CatnipBrewing() {
    }

    /** 造一个带默认药水内容的猫薄荷水物品堆。 */
    public static ItemStack newStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, CONTENTS);
        return stack;
    }

    /**
     * 匹配一条猫薄荷水配方。
     *
     * @param ingredient 酿造材料（酿造台上方槽）
     * @param potion     待酿造的药水（下方三个槽）
     * @return 产物；不匹配返回 {@code null}
     */
    public static ItemStack resultFor(ItemStack ingredient, ItemStack potion) {
        if (ingredient.isEmpty() || potion.isEmpty()) {
            return null;
        }
        // 物品还没注册完时（极早期调用）直接不匹配，避免 NPE
        if (ToNekoItems.CATNIP == null || ToNekoItems.CATNIP_WATER == null
                || ToNekoItems.CATNIP_SPLASH == null || ToNekoItems.CATNIP_LINGERING == null) {
            return null;
        }
        if (ingredient.is(ToNekoItems.CATNIP) && isWaterBottle(potion)) {
            return newStack(ToNekoItems.CATNIP_WATER);
        }
        if (ingredient.is(Items.GUNPOWDER) && potion.is(ToNekoItems.CATNIP_WATER)) {
            return newStack(ToNekoItems.CATNIP_SPLASH);
        }
        if (ingredient.is(Items.DRAGON_BREATH) && potion.is(ToNekoItems.CATNIP_SPLASH)) {
            return newStack(ToNekoItems.CATNIP_LINGERING);
        }
        return null;
    }

    /** 是否是原版水瓶（只认水，不认粗制的药水或其它药水）。 */
    public static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.WATER);
    }

    /** 是否是三件猫薄荷水之一（用于允许放回酿造槽）。 */
    public static boolean isCatnipPotion(ItemStack stack) {
        return (ToNekoItems.CATNIP_WATER != null && stack.is(ToNekoItems.CATNIP_WATER))
                || (ToNekoItems.CATNIP_SPLASH != null && stack.is(ToNekoItems.CATNIP_SPLASH))
                || (ToNekoItems.CATNIP_LINGERING != null && stack.is(ToNekoItems.CATNIP_LINGERING));
    }
}
