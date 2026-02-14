package org.cneko.toneko.common.mod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
                // 恢复一点猫猫能量
                neko.setNekoEnergy(neko.getNekoEnergy() + 30);
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
        hitOnAir(shooter, pos, bazooka, ammunition);
    }

    @Override
    public void hitOnAir(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
        // 粒子
        if (!shooter.level().isClientSide) {
            shooter.level().addParticle(
                    ()-> BuiltInRegistries.PARTICLE_TYPE.wrapAsHolder(ParticleTypes.EFFECT).value(),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    0,
                    0,
                    0
            );
        }
    }

    @Override
    public float getSpeed(ItemStack bazooka, ItemStack ammunition) {
        return 0.8f;
    }

    @Override
    public float getMaxDistance(ItemStack bazooka, ItemStack ammunition) {
        return 30;
    }

    @Override
    public int getCooldownTicks(ItemStack bazooka, ItemStack ammunition) {
        return 5;
    }

    public static class InfiniteCatnipItem extends CatnipItem {
        public InfiniteCatnipItem(Properties properties) {
            super(properties);
        }

        @Override
        public @NotNull ItemStack finishUsingItem(ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
            // 1. 在消耗前先备份物品栈（因为父类逻辑会消耗物品）
            ItemStack returnStack = stack.copy();

            // 2. 如果使用者是玩家，添加 5 秒冷却时间
            if (livingEntity instanceof Player player) {
                player.getCooldowns().addCooldown(this, 100);
            }

            // 3. 执行父类逻辑
            super.finishUsingItem(stack, level, livingEntity);

            // 4. 实现“无限”逻辑
            if (livingEntity instanceof Player player) {
                return returnStack;
            }

            // 对于非玩家实体，返回原本处理后的 stack 即可
            return stack;
        }

        @Override
        public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
            tooltipComponents.add(Component.translatable("item.toneko.infinite_catnip.tip"));
        }
    }

}
