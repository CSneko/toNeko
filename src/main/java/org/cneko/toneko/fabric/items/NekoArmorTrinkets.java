package org.cneko.toneko.fabric.items;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.emi.trinkets.api.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.fabric.items.ToNekoItems.*;
public class NekoArmorTrinkets {
    public static void init() {
        LOGGER.info("Trinkets detected, registering Neko Armors as TrinketItem");
        NekoArmorTrinkets.NekoTailTrinketItem nekoTailTrinketItem = new NekoArmorTrinkets.NekoTailTrinketItem();
        NekoArmorTrinkets.NekoEarsTrinketItem nekoEarsTrinketItem = new NekoArmorTrinkets.NekoEarsTrinketItem();
        NEKO_EARS = nekoEarsTrinketItem;
        NEKO_TAIL = nekoTailTrinketItem;
        TrinketsApi.registerTrinket(nekoEarsTrinketItem, nekoEarsTrinketItem);
        TrinketsApi.registerTrinket(nekoTailTrinketItem,nekoTailTrinketItem);
    }
    public static class NekoTailTrinketItem extends NekoArmor.NekoTailItem implements Trinket {
        public NekoTailTrinketItem() {
            super();
        }
        @Override
        public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
            return true;
        }
        @Override
        public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
            return true;
        }
    }

    public static class NekoEarsTrinketItem extends NekoArmor.NekoEarsItem implements Trinket{
        public NekoEarsTrinketItem() {
            super();
        }
        @Override
        public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
            return true;
        }
        @Override
        public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
            return true;
        }
    }

}
