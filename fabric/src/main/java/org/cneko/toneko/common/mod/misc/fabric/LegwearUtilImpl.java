package org.cneko.toneko.common.mod.misc.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;

/**
 * 26.x 迁移说明：Trinkets 尚无 Minecraft 26.x（去混淆版本）发布，
 * 原先经 Trinkets 饰品槽读取/标记丝袜的逻辑暂时移除，仅保留原版 LEGS 槽位路径。
 * 待 Trinkets 适配 26.x 后从 git 历史（tag V1.9.6 前后）恢复。
 */
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
