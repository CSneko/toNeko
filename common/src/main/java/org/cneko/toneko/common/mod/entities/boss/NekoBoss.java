package org.cneko.toneko.common.mod.entities.boss;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface NekoBoss {
    /**
     * 判断是否可以被收服
     */
    boolean canBeTamed(Player player, ItemStack contractItem);

    /**
     * 执行收服逻辑
     */
    boolean tame(Player player, ItemStack contractItem);
}
