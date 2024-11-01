package org.cneko.toneko.fabric.items;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;

import java.util.ArrayList;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.mod.items.ToNekoItems.*;
public class NekoArmorTrinkets {
    public static void init() {
        LOGGER.info("Trinkets detected, registering Neko Armors as TrinketItem");
        NekoArmorTrinkets.NekoTailTrinketItem nekoTailTrinketItem = new NekoArmorTrinkets.NekoTailTrinketItem();
        NekoArmorTrinkets.NekoEarsTrinketItem nekoEarsTrinketItem = new NekoArmorTrinkets.NekoEarsTrinketItem();
        NekoArmorTrinkets.NekoPawsTrinketItem nekoPawsTrinketItem = new NekoArmorTrinkets.NekoPawsTrinketItem();
        NEKO_EARS = nekoEarsTrinketItem;
        NEKO_TAIL = nekoTailTrinketItem;
        NEKO_PAWS = nekoPawsTrinketItem;
        TrinketsApi.registerTrinket(nekoEarsTrinketItem, nekoEarsTrinketItem);
        TrinketsApi.registerTrinket(nekoTailTrinketItem,nekoTailTrinketItem);
        TrinketsApi.registerTrinket(nekoPawsTrinketItem,nekoPawsTrinketItem);
    }
    public static class NekoTailTrinketItem extends NekoArmor.NekoTailItem implements Trinket {
        public NekoTailTrinketItem() {
            super(ToNekoArmorMaterials.NEKO);
        }
        @Override
        public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
            return true;
        }
        @Override
        public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
            return true;
        }
        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, ResourceLocation slotIdentifier) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            // 添加 10 的neko_degree
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
            return modifiers;
        }
    }

    public static class NekoEarsTrinketItem extends NekoArmor.NekoEarsItem implements Trinket{
        public NekoEarsTrinketItem() {
            super(ToNekoArmorMaterials.NEKO);
        }
        @Override
        public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
            return true;
        }
        @Override
        public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
            return true;
        }
        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, ResourceLocation slotIdentifier) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            // 添加 10 的neko_degree
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
            return modifiers;
        }
    }
    public static class NekoPawsTrinketItem extends NekoArmor.NekoPawsItem implements Trinket {
        public NekoPawsTrinketItem() {
            super(ToNekoArmorMaterials.NEKO);
        }
        @Override
        public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
            return true;
        }
        @Override
        public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
            return true;
        }
        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, ResourceLocation slotIdentifier) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            // 添加 10 的neko_degree
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
            return modifiers;
        }
    }

}
