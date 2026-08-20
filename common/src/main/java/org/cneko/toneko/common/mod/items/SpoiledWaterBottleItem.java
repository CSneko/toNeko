package org.cneko.toneko.common.mod.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 变质水瓶：可以直接喝（后果自负）。
 * 变质程度越高，反胃/中毒/虚弱越强；喝高等级水还会被附近猫娘嫌弃。
 */
public class SpoiledWaterBottleItem extends PotionItem {
    public static final String ID = "spoiled_water_bottle";

    public SpoiledWaterBottleItem() {
        super(new Properties().stacksTo(1)
                .component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .component(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT, 0)
                .component(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, ""));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT, 0);
        stack.set(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, "");
        return stack;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user) {
        Player player = user instanceof Player p ? p : null;
        if (player instanceof ServerPlayer sp) {
            CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
        }
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide && player != null) {
            int spoilage = ScentedWaterUtil.getSpoilage(stack);
            applyDrinkEffects(player, spoilage);
            if (spoilage >= 80 && player instanceof ServerPlayer sp) {
                ToNekoCriteria.SPOILED_WATER_DRINK.trigger(sp);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK,
                    SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        user.gameEvent(GameEvent.DRINK);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (player != null) {
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    private void applyDrinkEffects(Player player, int spoilage) {
        if (spoilage <= 0) return;
        if (spoilage < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        } else if (spoilage < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
        } else if (spoilage < 60) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 0));
        } else if (spoilage < 80) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0));
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int spoilage = ScentedWaterUtil.getSpoilage(stack);
        tooltip.add(Component.translatable("item.toneko.spoiled_water_bottle.tip.spoilage",
                Component.translatable("item.toneko.spoiled_water_bucket.spoilage." + ScentedWaterUtil.grade(spoilage))));
        String wearer = ScentedWaterUtil.getWearer(stack);
        if (!wearer.isEmpty()) {
            tooltip.add(Component.translatable("item.toneko.spoiled_water.tip.wearer", wearer));
        }
        tooltip.add(Component.translatable("item.toneko.spoiled_water_bottle.tip.drink"));
    }
}
