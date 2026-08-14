package org.cneko.toneko.common.mod.misc;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 平台感知的「腿部服饰」定位工具。
 * <p>
 * Fabric + Trinkets 时丝袜在 {@code legs/socks} 饰品槽，NeoForge / 无 Trinkets 时在 {@code EquipmentSlot.LEGS}
 * 盔甲槽。魅力值、脸红偷看、袜子滑落等特性都通过本工具找到「玩家正穿着的那件丝袜」。
 */
public class LegwearUtil {

    /**
     * 返回实体当前穿着的腿部服饰（丝袜/未来裙装）；未穿返回 {@link ItemStack#EMPTY}。
     * 返回的是活引用，可直接对其写组件（写完后调用 {@link #markLegwearDirty} 同步）。
     */
    @ExpectPlatform
    public static ItemStack getWornLegwear(LivingEntity entity) {
        throw new AssertionError();
    }

    /** 标记腿部服饰已变更，触发对应平台同步（盔甲槽 → 玩家库存容器；Trinkets → 组件 update）。 */
    @ExpectPlatform
    public static void markLegwearDirty(LivingEntity entity) {
        throw new AssertionError();
    }
}
