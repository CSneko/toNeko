package org.cneko.toneko.common.mod.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.SpoiledWaterProjectile;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 可投掷变质水：false=喷溅型（短时间气味云），true=滞留型（长时间气味云）。
 */
public class SpoiledWaterThrowableItem extends Item implements ProjectileItem {
    public static final String SPLASH_ID = "spoiled_water_splash";
    public static final String LINGERING_ID = "spoiled_water_lingering";
    private final boolean lingering;

    public SpoiledWaterThrowableItem(String idPath, boolean lingering) {
        super(NekoIds.itemProps(idPath).stacksTo(16)
                .component(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT, 0)
                .component(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, ""));
        this.lingering = lingering;
    }

    public boolean isLingering() {
        return lingering;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.NEUTRAL, 0.5f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide()) {
            SpoiledWaterProjectile projectile = new SpoiledWaterProjectile(level, player);
            projectile.setItem(stack.copy());
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.2f, 1.0f);
            level.addFreshEntity(projectile);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }

    @Override
    public @NotNull Projectile asProjectile(@NotNull Level level, Position pos, @NotNull ItemStack stack,
                                            @NotNull Direction direction) {
        SpoiledWaterProjectile projectile = new SpoiledWaterProjectile(level, pos.x(), pos.y(), pos.z());
        projectile.setItem(stack.copy());
        return projectile;
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        int spoilage = ScentedWaterUtil.getSpoilage(stack);
        tooltips.add(Component.translatable("item.toneko.spoiled_water_throwable.tip.spoilage",
                Component.translatable("item.toneko.spoiled_water_bucket.spoilage." + ScentedWaterUtil.grade(spoilage))));
        String wearer = ScentedWaterUtil.getWearer(stack);
        if (!wearer.isEmpty()) {
            tooltips.add(Component.translatable("item.toneko.spoiled_water.tip.wearer", wearer));
        }
        tooltips.add(Component.translatable(lingering
                ? "item.toneko.spoiled_water_throwable.tip.lingering"
                : "item.toneko.spoiled_water_throwable.tip.splash"));
    
        for (Component t : tooltips) adder.accept(t);
    }
}
