package org.cneko.toneko.common.mod.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.codecs.Scent;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 气味香水：由变质水 + 糖在炼药锅中调制。
 * 手持香水右键时，会把香味（等级 + 来源）复制到另一只手上的干净丝袜上。
 */
public class ScentPerfumeItem extends Item {
    public static final String ID = "scent_perfume";

    public ScentPerfumeItem() {
        super(NekoIds.itemProps(ID).stacksTo(1)
                .component(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT, 0)
                .component(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, ""));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack perfume = player.getItemInHand(hand);
        ItemStack legwear = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!org.cneko.toneko.common.mod.items.LegwearItem.isLegwear(legwear)
                || !legwear.has(ToNekoComponents.LEGWEAR_SCENT_COMPONENT)) {
            player.sendOverlayMessage(Component.translatable("item.toneko.scent_perfume.tip.need_legwear"));
            return InteractionResult.FAIL;
        }

        if (ScentUtil.getIntensity(legwear) > 0) {
            player.sendOverlayMessage(Component.translatable("item.toneko.scent_perfume.tip.not_clean"));
            return InteractionResult.FAIL;
        }

        int spoilage = ScentedWaterUtil.getSpoilage(perfume);
        String wearer = ScentedWaterUtil.getWearer(perfume);
        legwear.set(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, new Scent(spoilage, wearer));
        perfume.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 0.8f, 1.0f);
        player.sendOverlayMessage(Component.translatable("item.toneko.scent_perfume.tip.applied",
                Component.translatable("item.toneko.spoiled_water_bucket.spoilage." + ScentedWaterUtil.grade(spoilage))));
        return InteractionResult.SUCCESS;
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        int spoilage = ScentedWaterUtil.getSpoilage(stack);
        tooltips.add(Component.translatable("item.toneko.scent_perfume.tip.spoilage",
                Component.translatable("item.toneko.spoiled_water_bucket.spoilage." + ScentedWaterUtil.grade(spoilage))));
        String wearer = ScentedWaterUtil.getWearer(stack);
        if (!wearer.isEmpty()) {
            tooltips.add(Component.translatable("item.toneko.spoiled_water.tip.wearer", wearer));
        }
        tooltips.add(Component.translatable("item.toneko.scent_perfume.tip.use"));
    
        for (Component t : tooltips) adder.accept(t);
    }
}
