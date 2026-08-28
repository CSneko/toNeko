package org.cneko.toneko.common.mod.items;
import net.minecraft.world.item.Item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;
import org.cneko.toneko.common.mod.blocks.ShengDengBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShengDengItem extends BlockItem {

    public static final String ID = "sheng_deng";
    private static final String STACK_COUNT_KEY = "stack_count";

    public ShengDengItem(Block block, Properties properties) {
        super(block, properties);
    }

    // ==================== 叠放数量 NBT ====================

    public static int getStackCount(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        int count = 1;
        if (data != null && data.copyTag().contains(STACK_COUNT_KEY)) {
            count = data.copyTag().getIntOr(STACK_COUNT_KEY, 0);
        }
        // 兜底：叠加 >1 但属性组件为空时补上（如 /give 或旧存档物品）。
        // 注意：物品默认组件自带空的 attribute_modifiers（EMPTY），has() 恒为 true，
        // 必须检查值是否为空，否则动态属性永远写不进去
        if (count > 1) {
            ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (modifiers == null || modifiers.modifiers().isEmpty()) {
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildAttributeModifiers(count));
            }
        }
        return count;
    }

    public static void setStackCount(ItemStack stack, int count) {
        if (count <= 1) {
            stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(STACK_COUNT_KEY, count);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        // 动态属性写在物品组件里（1.21.1 的 Item 属性是静态的，无法按栈变化）
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildAttributeModifiers(count));
    }

    public static ItemStack createStackedItem(int count) {
        ItemStack stack = new ItemStack(ToNekoItems.SHENG_DENG_ITEM, 1);
        setStackCount(stack, count);
        return stack;
    }

    /** 物品栏每 tick 兜底：手动 /give 或旧存档物品属性组件为空时补上（服务端属性应用需要） */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        getStackCount(stack); // 内部自带组件兜底
    }

    // ==================== Tooltip ====================

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        int count = getStackCount(stack);
        if (count > 1) {
            tooltips.add(Component.translatable("item.toneko.sheng_deng.stacked", count));
        }
        tooltips.add(Component.translatable("block.toneko.sheng_deng.tooltip"));
        tooltips.add(Component.translatable("item.toneko.sheng_deng.weapon"));
    
        for (Component t : tooltips) adder.accept(t);
    }

    /** 按叠放数量重建属性修饰符组件（26.x：ItemStack 属性来自 ATTRIBUTE_MODIFIERS 组件） */
    private static ItemAttributeModifiers buildAttributeModifiers(int count) {
        double extra = count - 1; // 超出 1 张的部分

        return ItemAttributeModifiers.builder()
                // 攻击伤害：基础 3.0，每多一张 +0.5
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath("toneko", "sheng_deng_damage"),
                                3.0 + extra * 0.5, AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                // 攻击速度：每多一张 -5% 攻速（乘算，永不归零）
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath("toneko", "sheng_deng_attack_speed"),
                                -extra * 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                // 移动速度：每多一张 -0.8% 移速（乘算，轻微减速即可）
                .add(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath("toneko", "sheng_deng_move_speed"),
                                -extra * 0.008, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                // 攻击距离（实体交互范围）：每多一张 +0.3 格
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath("toneko", "sheng_deng_range"),
                                extra * 0.3, AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                // 击退：基础 1.5，每多一张 +0.05
                .add(Attributes.ATTACK_KNOCKBACK,
                        new AttributeModifier(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath("toneko", "sheng_deng_knockback"),
                                1.5 + extra * 0.05, AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
