package org.cneko.toneko.common.mod.misc.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 平台实现：定位玩家穿着的腿部服饰。
 * 装了 Curios 时优先查饰品栏（socks 槽），否则回退到 LEGS 盔甲槽——
 * 与 Fabric + Trinkets 的 {@code fabric.LegwearUtilImpl} 行为对齐。
 * <p>
 * Curios 为可选依赖：{@link #CURIOS} 运行期反射探测，
 * 未安装时不会执行引用 Curios API 的分支（JVM 惰性解析，不会 NoClassDefFoundError）。
 */
public class LegwearUtilImpl {
    private static final boolean CURIOS = classExists("top.theillusivec4.curios.api.CuriosApi");

    public static ItemStack getWornLegwear(LivingEntity entity) {
        if (CURIOS) {
            var found = CuriosApi.getCuriosInventory(entity)
                    .flatMap(handler -> handler.findFirstCurio(LegwearItem::isLegwear));
            if (found.isPresent()) {
                return found.get().stack();
            }
        }
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        return LegwearItem.isLegwear(legs) ? legs : ItemStack.EMPTY;
    }

    public static void markLegwearDirty(LivingEntity entity) {
        if (CURIOS) {
            // 重新 setStackInSlot 触发 Curios 容器变更通知（组件改动同步到客户端）
            CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                var equipped = handler.getEquippedCurios();
                for (int i = 0; i < equipped.getSlots(); i++) {
                    ItemStack stack = equipped.getStackInSlot(i);
                    if (LegwearItem.isLegwear(stack)) {
                        equipped.setStackInSlot(i, stack);
                        return;
                    }
                }
            });
        }
        if (entity instanceof ServerPlayer player) {
            player.getInventory().setChanged();
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
