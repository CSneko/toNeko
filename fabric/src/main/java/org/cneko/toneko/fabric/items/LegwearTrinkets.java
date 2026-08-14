package org.cneko.toneko.fabric.items;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;

import java.util.ArrayList;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

/**
 * Trinkets 集成：丝袜注册为 Trinket（legs/socks 槽），与盔甲裤子共存。
 * 同一物品 ID、同一静态字段，仅替换为 Trinket 版实现类（照 NekoArmorTrinkets 模式）。
 */
public class LegwearTrinkets {
    public static void init() {
        LOGGER.info("Trinkets detected, registering Legwear as TrinketItem");
        Pantyhose40DTrinketItem p40 = new Pantyhose40DTrinketItem();
        Pantyhose20DTrinketItem p20 = new Pantyhose20DTrinketItem();
        Pantyhose5DTrinketItem p5 = new Pantyhose5DTrinketItem();
        OverKneeTrinketItem ok = new OverKneeTrinketItem();
        LEGWEAR_PANTYHOSE_40D = p40;
        LEGWEAR_PANTYHOSE_20D = p20;
        LEGWEAR_PANTYHOSE_5D = p5;
        LEGWEAR_OVER_KNEE = ok;
        TrinketsApi.registerTrinket(p40, p40);
        TrinketsApi.registerTrinket(p20, p20);
        TrinketsApi.registerTrinket(p5, p5);
        TrinketsApi.registerTrinket(ok, ok);
    }

    public static class Pantyhose40DTrinketItem extends LegwearItem.Pantyhose40DItem implements Trinket {
        public Pantyhose40DTrinketItem() {
            super(ToNekoArmorMaterials.LEGWEAR);
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
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            SlotAttributes.addSlotModifier(modifiers, "legs/socks", slotIdentifier, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }

    public static class Pantyhose20DTrinketItem extends LegwearItem.Pantyhose20DItem implements Trinket {
        public Pantyhose20DTrinketItem() {
            super(ToNekoArmorMaterials.LEGWEAR);
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
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            SlotAttributes.addSlotModifier(modifiers, "legs/socks", slotIdentifier, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }

    public static class Pantyhose5DTrinketItem extends LegwearItem.Pantyhose5DItem implements Trinket {
        public Pantyhose5DTrinketItem() {
            super(ToNekoArmorMaterials.LEGWEAR);
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
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            SlotAttributes.addSlotModifier(modifiers, "legs/socks", slotIdentifier, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }

    public static class OverKneeTrinketItem extends LegwearItem.OverKneeSockItem implements Trinket {
        public OverKneeTrinketItem() {
            super(ToNekoArmorMaterials.LEGWEAR);
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
            modifiers.put(ToNekoAttributes.NEKO_DEGREE, new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            SlotAttributes.addSlotModifier(modifiers, "legs/socks", slotIdentifier, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }
}
