package org.cneko.toneko.common.mod.items.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class LightningBombItem extends AmmoItem.SameEffectItem {
    public LightningBombItem(Properties properties) {
        super(properties);
    }
    @Override
    public void applyEffect(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
        if (!shooter.level().isClientSide) {
            var lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, shooter.level());
            lightningBolt.setPos(pos.getX(), pos.getY(), pos.getZ());
            shooter.level().addFreshEntity(lightningBolt);
        }
    }
}
