package org.cneko.toneko.common.mod.items;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.cneko.toneko.common.mod.entities.FlySwordEntity;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;

import java.util.List;

public class FlySwordItem extends Item {

    public FlySwordItem(Properties properties) {
        super(properties);
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        // Read upgrade levels from stored data
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        int iron = 0, diamond = 0, netherite = 0, maxLimit = 10;
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            iron = tag.getIntOr("iron", 0);
            diamond = tag.getIntOr("diamond", 0);
            netherite = tag.getIntOr("netherite", 0);
            maxLimit = tag.contains("maxLimit") ? tag.getIntOr("maxLimit", 0) : 10;
        }
        tooltips.add(Component.literal("§7===== §fUpgrades §7====="));
        tooltips.add(Component.literal("§8Iron: §7" + iron + " §8/ §7" + maxLimit));
        tooltips.add(Component.literal("§bDiamond: §7" + diamond + " §8/ §7" + maxLimit));
        tooltips.add(Component.literal("§4Netherite: §7" + netherite + " §8/ §7" + maxLimit));
        tooltips.add(Component.literal("§eMax Limit: §7" + maxLimit));
        for (int i = 0; i < 6; i++) {
            tooltips.add(Component.translatable("item.toneko.fly_sword.tooltip." + i));
        }
    
        for (Component t : tooltips) adder.accept(t);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        FlySwordEntity sword = ToNekoEntities.FLY_SWORD_ENTITY.create((net.minecraft.server.level.ServerLevel) level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (sword == null) return InteractionResult.FAIL;

        sword.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getPlayer().getYRot(), 0);

        // Load upgrades from item's CUSTOM_DATA (limit first, then levels)
        CustomData custom = context.getItemInHand().get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            if (tag.contains("maxLimit")) sword.setMaxUpgradeLimit(tag.getIntOr("maxLimit", 0));
            sword.setNetherStarUpgrade(tag.getBooleanOr("netherStar", false));
            sword.setIronLevel(tag.getIntOr("iron", 0));
            sword.setDiamondLevel(tag.getIntOr("diamond", 0));
            sword.setNetheriteLevel(tag.getIntOr("netherite", 0));
        }

        level.addFreshEntity(sword);
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
