package org.cneko.toneko.common.mod.misc.fabric;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;

public class LegwearUtilImpl {
    /** Trinkets 为可选依赖：运行期反射探测，避免未安装时 NoClassDefFoundError。 */
    private static final boolean TRINKETS = classExists("dev.emi.trinkets.api.TrinketsApi");

    public static ItemStack getWornLegwear(LivingEntity entity) {
        if (TRINKETS) {
            var component = TrinketsApi.getTrinketComponent(entity);
            if (component.isPresent()) {
                for (Tuple<SlotReference, ItemStack> pair : component.get().getEquipped(LegwearItem::isLegwear)) {
                    return pair.getB();
                }
            }
        }
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        return LegwearItem.isLegwear(legs) ? legs : ItemStack.EMPTY;
    }

    public static void markLegwearDirty(LivingEntity entity) {
        if (TRINKETS) {
            TrinketsApi.getTrinketComponent(entity).ifPresent(TrinketComponent::update);
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
