package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CatnipItem extends Item {
    public CatnipItem() {
        super(new Properties().component(DataComponents.FOOD,
                new FoodProperties(2,1.0f,true,1.6f, Optional.empty(),
                        List.of()
                )
            )
        );
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null) {
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
}
