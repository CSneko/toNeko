package org.cneko.toneko.neoforge.items;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_OVER_KNEE_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_PANTYHOSE_20D_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_PANTYHOSE_40D_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.LEGWEAR_PANTYHOSE_5D_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.NEKO_EARS_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.NEKO_PAWS_HOLDER;
import static org.cneko.toneko.neoforge.items.ToNekoItems.NEKO_TAIL_HOLDER;

/**
 * Curios 集成（NeoForge，可选依赖）：把猫娘饰品与丝袜接入 Curios 槽位。
 * <p>
 * 与 Fabric 的 Trinkets 集成（{@code NekoArmorTrinkets} / {@code LegwearTrinkets}）对齐：
 * <ul>
 *   <li>猫耳 → {@code head} 头部槽、猫尾 → {@code back} 背部槽：
 *       Curios 默认槽位带 {@code curios:tag} 校验器，通过物品标签
 *       （{@code data/curios/tags/item/}，位于 common 资源）准入；</li>
 *   <li>猫爪 → 无标签限制，可放入通用 {@code curio} 槽（默认无校验器）；</li>
 *   <li>丝袜（4 款腿部服饰）→ 自定义 {@code socks} 槽
 *       （{@code data/toneko/curios/slots/socks.json}，同样带 tag 校验器；
 *       槽位通过 {@code data/toneko/curios/entities/player.json} 分配给玩家）。</li>
 * </ul>
 * 属性与 Trinkets 版一致：每件 +0.05 猫娘度（NEKO_DEGREE）；
 * 猫耳/猫尾额外 +1 对应槽位、丝袜额外 +1 袜子槽（对应 Trinkets 的 SlotAttributes 加成）。
 * <p>
 * 行为通过 {@link CuriosApi#registerCurio} 外挂到已注册的盔甲物品上，
 * 不改动物品类继承结构；Curios 未安装时本类不会被加载。
 */
public final class NekoArmorCurios {
    /** 猫耳/猫尾/丝袜提供的槽位加成修饰符 ID */
    private static final Identifier SLOT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(MODID, "curio_slot");

    private NekoArmorCurios() {}

    /** 在 FMLCommonSetupEvent#enqueueWork 中调用（注册表就绪后） */
    public static void init() {
        CuriosApi.registerCurio(NEKO_EARS_HOLDER.get(), new NekoEarsCurio());
        CuriosApi.registerCurio(NEKO_TAIL_HOLDER.get(), new NekoTailCurio());
        CuriosApi.registerCurio(NEKO_PAWS_HOLDER.get(), new NekoPawsCurio());
        LegwearCurio legwear = new LegwearCurio();
        CuriosApi.registerCurio(LEGWEAR_PANTYHOSE_40D_HOLDER.get(), legwear);
        CuriosApi.registerCurio(LEGWEAR_PANTYHOSE_20D_HOLDER.get(), legwear);
        CuriosApi.registerCurio(LEGWEAR_PANTYHOSE_5D_HOLDER.get(), legwear);
        CuriosApi.registerCurio(LEGWEAR_OVER_KNEE_HOLDER.get(), legwear);
        LOGGER.info("Curios detected, registering Neko Armors & Legwear as Curios "
                + "(ears→head, tail→back, paws→curio, legwear→socks)");
    }

    /** 猫耳行为：+0.05 猫娘度 + 1 个额外头部饰品槽 */
    public static class NekoEarsCurio implements ICurioItem {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, Identifier identifier, ItemStack stack) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                    Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            // +0.05 猫娘度（与 Trinkets 版一致）
            modifiers.put(ToNekoAttributes.NEKO_DEGREE,
                    new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            // 额外 1 个头部饰品槽
            CuriosApi.addSlotModifier(modifiers, "head", SLOT_MODIFIER_ID, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }

    /** 猫尾行为：+0.05 猫娘度 + 1 个额外背部饰品槽 */
    public static class NekoTailCurio implements ICurioItem {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, Identifier identifier, ItemStack stack) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                    Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            modifiers.put(ToNekoAttributes.NEKO_DEGREE,
                    new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            // 额外 1 个背部饰品槽
            CuriosApi.addSlotModifier(modifiers, "back", SLOT_MODIFIER_ID, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }

    /** 猫爪行为：+0.05 猫娘度（通用 curio 槽使用） */
    public static class NekoPawsCurio implements ICurioItem {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, Identifier identifier, ItemStack stack) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                    Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            modifiers.put(ToNekoAttributes.NEKO_DEGREE,
                    new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            return modifiers;
        }
    }

    /**
     * 丝袜行为（4 款腿部服饰共用）：+0.05 猫娘度 + 1 个额外袜子槽。
     * 与 Fabric Trinkets 版 {@code LegwearTrinkets} 的属性完全一致。
     */
    public static class LegwearCurio implements ICurioItem {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, Identifier identifier, ItemStack stack) {
            Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                    Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
            modifiers.put(ToNekoAttributes.NEKO_DEGREE,
                    new AttributeModifier(ToNekoAttributes.NEKO_DEGREE_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
            // 额外 1 个袜子槽（对应 Trinkets 的 legs/socks 槽加成）
            CuriosApi.addSlotModifier(modifiers, "socks", SLOT_MODIFIER_ID, 1, AttributeModifier.Operation.ADD_VALUE);
            return modifiers;
        }
    }
}
