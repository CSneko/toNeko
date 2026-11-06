package org.cneko.toneko.common.mod.items;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 喷溅型猫薄荷水：猫薄荷水 + 火药在酿造台酿造。
 *
 * <p>投掷行为直接复用原版 {@link SplashPotionItem}。默认堆只有「水 + 绿色」，真正投掷前
 * 在 {@link #use} 里把「猫薄荷香气」写进内容——这样 {@code /give} / 创造栏拿到的物品
 * 也正常，同时不依赖物品构造阶段的效果注册。
 *
 * <p>瓶身材质用原版 {@code minecraft:item/splash_potion} + {@code minecraft:item/potion_overlay}。
 */
public class CatnipSplashItem extends SplashPotionItem {
    public static final String ID = "catnip_splash";

    public CatnipSplashItem() {
        super(NekoIds.itemProps(ID).stacksTo(1)
                .component(DataComponents.POTION_CONTENTS, CatnipBrewing.baseContents()));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(DataComponents.POTION_CONTENTS, CatnipBrewing.baseContents());
        return stack;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        // 抛出前挂上香气（默认内容里不带效果，见 CatnipBrewing 注释）
        player.getItemInHand(hand).set(DataComponents.POTION_CONTENTS, CatnipBrewing.aromaContents());
        return super.use(level, player, hand);
    }

    @Override
    public @NotNull Projectile asProjectile(@NotNull Level level, @NotNull Position pos,
                                            @NotNull ItemStack stack, @NotNull Direction direction) {
        // 发射器路径也要带上香气
        stack.set(DataComponents.POTION_CONTENTS, CatnipBrewing.aromaContents());
        return super.asProjectile(level, pos, stack, direction);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context,
                                @NotNull TooltipDisplay display, @NotNull Consumer<Component> adder,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(Component.translatable("item.toneko.catnip_splash.tip"));
    }
}
