package org.cneko.toneko.common.mod.util;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

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

    public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantment, ItemStack stack, Level level){
        var en = level.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(lookup -> lookup.get(Enchantments.LOYALTY)).orElse(null);
        if (en == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(en,stack);
    }
}
