package org.cneko.toneko.fabric.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;

public class EnchantmentUtil {
    public static boolean hasEnchantment(Identifier id, ItemStack stack){
        AtomicBoolean returnValue = new AtomicBoolean(false);
        stack.getEnchantments().getEnchantments().forEach(enchantment -> {
            if(enchantment.getIdAsString().equals(id.toString())){
                returnValue.set(true);
            }
        });
        return returnValue.get();
    }
}
