package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;

import java.util.List;
import java.util.Optional;

/**
 * 猫薄荷水的酿造规则与药水内容。
 *
 * <p>三条配方，全部在酿造台里完成（和原版药水一样）：
 * <ol>
 *   <li>{@code 水瓶 + 猫薄荷 → 猫薄荷水}（饮用：兴奋 II + 30 能量）；</li>
 *   <li>{@code 猫薄荷水 + 火药 → 喷溅型猫薄荷水}（只给玩家挂猫薄荷香气）；</li>
 *   <li>{@code 喷溅型猫薄荷水 + 龙息 → 滞留型猫薄荷水}（同上）。</li>
 * </ol>
 *
 * <p>原版 {@code PotionBrewing} 只认识 {@code Items.POTION/SPLASH_POTION/LINGERING_POTION}，
 * 所以这三条配方由 {@code PotionBrewingMixin} 在 {@code isIngredient/hasMix/mix} 处注入。
 *
 * <p>药水内容按需构建：物品注册（Fabric 上）早于效果注册，静态常量会在效果还没注册时初始化，
 * 所以这里用惰性构建，并且效果缺失时退回「只有颜色、没有效果」的版本。
 */
public final class CatnipBrewing {
    /** 猫薄荷水的颜色：取自猫薄荷物品贴图的主色（#88B008） */
    public static final int COLOR = 0x88B008;

    /** 普通猫薄荷水：兴奋 II 持续 10000 tick（和猫薄荷本体一致） */
    private static final int DRINK_DURATION = 10000;
    /** 喷溅 / 滞留：猫薄荷香气持续 8 分钟 */
    private static final int AROMA_DURATION = 8 * 60 * 20;

    private static PotionContents baseContents;
    private static PotionContents drinkContents;
    private static PotionContents aromaContents;

    private CatnipBrewing() {
    }

    /** 只带颜色、没有效果的兜底内容（效果尚未注册时使用）。 */
    private static PotionContents baseContents() {
        if (baseContents == null) {
            baseContents = new PotionContents(Optional.of(Potions.WATER), Optional.of(COLOR), List.of(), Optional.empty());
        }
        return baseContents;
    }

    /** 普通猫薄荷水喝下后的内容：兴奋 II。 */
    public static PotionContents drinkContents() {
        MobEffect exciting = ToNekoEffects.NEKO_EFFECT;
        if (drinkContents == null && exciting != null) {
            drinkContents = new PotionContents(
                    Optional.of(Potions.WATER),
                    Optional.of(COLOR),
                    List.of(new MobEffectInstance(holder(exciting), DRINK_DURATION, 1)),
                    Optional.empty());
        }
        return drinkContents != null ? drinkContents : baseContents();
    }

    /** 喷溅 / 滞留的内容：猫薄荷香气（只对玩家生效）。 */
    public static PotionContents aromaContents() {
        MobEffect aroma = ToNekoEffects.CATNIP_AROMA;
        if (aromaContents == null && aroma != null) {
            aromaContents = new PotionContents(
                    Optional.of(Potions.WATER),
                    Optional.of(COLOR),
                    List.of(new MobEffectInstance(holder(aroma), AROMA_DURATION, 0)),
                    Optional.empty());
        }
        return aromaContents != null ? aromaContents : baseContents();
    }

    private static net.minecraft.core.Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    /** 是不是猫薄荷香气（喷溅/滞留落地时用来过滤非玩家）。 */
    public static boolean isAroma(MobEffectInstance instance) {
        return instance != null
                && ToNekoEffects.CATNIP_AROMA != null
                && instance.getEffect().value() == ToNekoEffects.CATNIP_AROMA;
    }

    /** 造一个带正确药水内容的猫薄荷水物品堆。 */
    public static ItemStack newStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS,
                item == ToNekoItems.CATNIP_WATER ? drinkContents() : aromaContents());
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
