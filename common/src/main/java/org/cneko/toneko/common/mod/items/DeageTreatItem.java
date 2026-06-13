package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

public class DeageTreatItem extends Item {
    public static final String ID = "deage_treat";
    public static final int DEAGE_AMOUNT = 60000; // 2.5 game days worth of de-aging

    public DeageTreatItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null) {
            if (!level.isClientSide && livingEntity instanceof INeko neko && neko.isNeko()) {
                int currentAge = neko.getNekoAge();
                int maxAge = neko.getMaxAge();
                if (currentAge > -maxAge) {
                    // 逐渐幼化（但不能低于 -maxAge）
                    int newAge = Math.max(currentAge - DEAGE_AMOUNT, -maxAge);
                    neko.setNekoAge(newAge);
                    if (livingEntity instanceof Player player) {
                        player.playSound(SoundEvents.TOTEM_USE, 0.5F, 1.5F);
                    }
                }
            }
            return livingEntity.eat(level, stack, foodProperties);
        }
        return stack;
    }
}
