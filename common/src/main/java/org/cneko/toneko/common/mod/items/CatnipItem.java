package org.cneko.toneko.common.mod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

public class CatnipItem extends Item implements BazookaItem.Ammunition {
    public CatnipItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null && !livingEntity.level().isClientSide) {
            if (livingEntity instanceof INeko neko && neko.isNeko()){
                livingEntity.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.NEKO_EFFECT),
                        10000,
                        0
                ));
            }
            return livingEntity.eat(level, stack, foodProperties);
        }
        return stack;
    }

    @Override
    public void hitOnEntity(LivingEntity shooter, LivingEntity target, ItemStack bazooka, ItemStack ammunition) {
        if (!shooter.level().isClientSide) {
            if (target instanceof INeko neko && neko.isNeko()) {
                target.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.NEKO_EFFECT),
                        10000,
                        0
                ));
            }
        }
    }

    @Override
    public void hitOnBlock(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
    }

    @Override
    public void hitOnAir(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
    }

    @Override
    public float getSpeed(ItemStack bazooka, ItemStack ammunition) {
        return 1;
    }

    @Override
    public float getMaxDistance(ItemStack bazooka, ItemStack ammunition) {
        return 30;
    }

    @Override
    public int getCooldownTicks(ItemStack bazooka, ItemStack ammunition) {
        return 5;
    }
}
