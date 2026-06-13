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

public class GrowthTreatItem extends Item {
    public static final String ID = "growth_treat";
    public static final int GROWTH_AMOUNT = 60000; // 2.5 game days worth of growth

    public GrowthTreatItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null) {
            if (!level.isClientSide && livingEntity instanceof INeko neko && neko.isNeko()) {
                int currentAge = neko.getNekoAge();
                if (currentAge < 0) {
                    // 加速成长，最多到成年（age = 0）
                    int newAge = Math.min(currentAge + GROWTH_AMOUNT, 0);
                    neko.setNekoAge(newAge);
                    // 成年时播放音效
                    if (newAge >= 0 && livingEntity instanceof Player player) {
                        player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                    }
                }
            }
            return livingEntity.eat(level, stack, foodProperties);
        }
        return stack;
    }
}