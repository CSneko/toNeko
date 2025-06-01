package org.cneko.toneko.common.mod.items.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.cneko.toneko.common.mod.items.BazookaItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AmmoItem extends Item implements BazookaItem.Ammunition {
    public AmmoItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltips, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltips, tooltipFlag);
        tooltips.add(Component.translatable("item.toneko.ammo.use_with_bazooka"));
    }

    public abstract static class SameEffectItem extends AmmoItem{

        public abstract void applyEffect(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition);

        public SameEffectItem(Properties properties) {
            super(properties);
        }

        @Override
        public void hitOnEntity(LivingEntity shooter, LivingEntity target, ItemStack bazooka, ItemStack ammunition) {
            applyEffect( shooter, target.blockPosition(), bazooka, ammunition);
        }

        @Override
        public void hitOnBlock(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
             applyEffect( shooter, pos, bazooka, ammunition);
        }

        @Override
        public void hitOnAir(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
            applyEffect( shooter, pos, bazooka, ammunition);
        }
    }
}
