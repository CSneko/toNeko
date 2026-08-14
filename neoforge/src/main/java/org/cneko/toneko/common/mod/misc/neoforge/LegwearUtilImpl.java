package org.cneko.toneko.common.mod.misc.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;

public class LegwearUtilImpl {
    public static ItemStack getWornLegwear(LivingEntity entity) {
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        return LegwearItem.isLegwear(legs) ? legs : ItemStack.EMPTY;
    }

    public static void markLegwearDirty(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            player.getInventory().setChanged();
        }
    }
}
