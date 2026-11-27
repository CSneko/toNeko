package org.cneko.toneko.common.mod.items;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.TooltipDisplay;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * 猫薄荷水的酿造规则、默认药水内容与运行时效果。
 *
 * <p>三条配方，全部在酿造台里完成（和原版药水一样）：
 * <ol>
 *   <li>{@code 水瓶 + 猫薄荷 → 猫薄荷水}（饮用：兴奋 II + 30 能量）；</li>
 *   <li>{@code 猫薄荷水 + 火药 → 喷溅型猫薄荷水}（只给玩家挂猫薄荷香气）；</li>
 *   <li>{@code 喷溅型猫薄荷水 + 龙息 → 滞留型猫薄荷水}（同上）。</li>
 * </ol>
 *
 * <p><b>为什么默认内容里不带效果：</b>物品的默认组件来自 {@code Item.Properties}，
 * 而 {@code /give}、创造模式物品栏都用 {@code new ItemStack(item)} 读取它（不会走
 * {@code getDefaultInstance}）。Fabric 上物品注册早于效果注册、NeoForge 上效果 Holder 更是
 * 在注册事件里才可用，所以 Properties 阶段拿不到 NEKO_EFFECT / CATNIP_AROMA。
 *
 * <p>于是拆成两层：
 * <ul>
 *   <li>Properties / 默认实例：只放「水 + 绿色」的静态内容（染色 + 酿造槽识别）；</li>
 *   <li>喝下 / 抛出时：再通过 {@link #excitementInstance()} / {@link #aromaContents()} 挂效果。</li>
 * </ul>
 */
public final class CatnipBrewing {
    /** 猫薄荷水的颜色：取自猫薄荷物品贴图的主色（#88B008） */
    public static final int COLOR = 0x88B008;

    /** 普通猫薄荷水：兴奋 II 持续 10000 tick（和猫薄荷本体一致） */
    public static final int DRINK_DURATION = 10000;
    /** 喷溅 / 滞留：猫薄荷香气持续 8 分钟 */
    public static final int AROMA_DURATION = 8 * 60 * 20;

    private static PotionContents baseContents;
    private static PotionContents aromaContents;

    /**
     * 隐藏药水组件的 tooltip：默认内容里没有效果，原版会显示一行「无效果」。
     * 猫薄荷水不需要这行，直接把这个组件从 tooltip 里藏掉。
     */
    public static final TooltipDisplay HIDE_POTION_TOOLTIP = new TooltipDisplay(false,
            new LinkedHashSet<>(List.of(DataComponents.POTION_CONTENTS)));

    private CatnipBrewing() {
    }

    /**
     * 默认内容：水（让酿造槽和 {@code PotionSlot} 认为这是药水）+ 自定义颜色，没有效果。
     * 这个可以在物品构造阶段安全使用。
     */
    public static PotionContents baseContents() {
        if (baseContents == null) {
            baseContents = new PotionContents(Optional.of(Potions.WATER), Optional.of(COLOR), List.of(), Optional.empty());
        }
        return baseContents;
    }

    /** 普通猫薄荷水喝下时挂的「兴奋 II」；效果未注册时返回 {@code null}。 */
    public static MobEffectInstance excitementInstance() {
        MobEffect exciting = ToNekoEffects.NEKO_EFFECT;
        return exciting == null ? null : new MobEffectInstance(holder(exciting), DRINK_DURATION, 1);
    }

    /**
     * 喷溅 / 滞留的内容：猫薄荷香气（只对玩家生效）；效果未注册时退回默认内容。
     *
     * <p>药水部分故意用 {@link Potions#AWKWARD} 而不是水：{@code AbstractThrownPotion#onHit}
     * 会先判断内容是不是水，是水就只走「浇水」分支、不调用 {@code onHitAsPotion}，
     * 香气就永远挂不上。粗制的药水没有效果，但 customEffects 非空，能正常走药水分支。
     */
    public static PotionContents aromaContents() {
        MobEffect aroma = ToNekoEffects.CATNIP_AROMA;
        if (aromaContents == null && aroma != null) {
            aromaContents = new PotionContents(
                    Optional.of(Potions.AWKWARD),
                    Optional.of(COLOR),
                    List.of(new MobEffectInstance(holder(aroma), AROMA_DURATION, 0)),
                    Optional.empty());
        }
        return aromaContents != null ? aromaContents : baseContents();
    }

    private static Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    /** 是不是猫薄荷香气（喷溅/滞留落地时用来过滤非玩家）。 */
    public static boolean isAroma(MobEffectInstance instance) {
        return instance != null
                && ToNekoEffects.CATNIP_AROMA != null
                && instance.getEffect().value() == ToNekoEffects.CATNIP_AROMA;
    }

    /** 造一个带默认药水内容的猫薄荷水物品堆。 */
    public static ItemStack newStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, baseContents());
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
