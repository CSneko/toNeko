package org.cneko.toneko.common.mod.util;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class EnchantmentUtil {
    public static boolean hasEnchantment(ResourceLocation id, ItemStack stack){
        AtomicBoolean returnValue = new AtomicBoolean(false);
        stack.getEnchantments().keySet().forEach(enchantment -> {
            if(enchantment.getRegisteredName().equals(id.toString())){
                returnValue.set(true);
            }
        });
        return returnValue.get();
    }
}
