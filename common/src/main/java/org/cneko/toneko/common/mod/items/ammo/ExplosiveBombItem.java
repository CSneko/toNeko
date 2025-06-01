package org.cneko.toneko.common.mod.items.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExplosiveBombItem extends AmmoItem.SameEffectItem {
    public ExplosiveBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public void applyEffect(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
        if (!shooter.level().isClientSide) {
            shooter.level().explode(null, pos.getX(), pos.getY(), pos.getZ(), 2, false, Level.ExplosionInteraction.NONE);
        }
    }
}
