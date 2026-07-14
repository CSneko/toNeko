package org.cneko.toneko.common.mod.items;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Read upgrade levels from stored data
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        int iron = 0, diamond = 0, netherite = 0, maxLimit = 10;
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            iron = tag.getInt("iron");
            diamond = tag.getInt("diamond");
            netherite = tag.getInt("netherite");
            maxLimit = tag.contains("maxLimit") ? tag.getInt("maxLimit") : 10;
        }
        tooltip.add(Component.literal("§7===== §fUpgrades §7====="));
        tooltip.add(Component.literal("§8Iron: §7" + iron + " §8/ §7" + maxLimit));
        tooltip.add(Component.literal("§bDiamond: §7" + diamond + " §8/ §7" + maxLimit));
        tooltip.add(Component.literal("§4Netherite: §7" + netherite + " §8/ §7" + maxLimit));
        tooltip.add(Component.literal("§eMax Limit: §7" + maxLimit));
        for (int i = 0; i < 6; i++) {
            tooltip.add(Component.translatable("item.toneko.fly_sword.tooltip." + i));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        FlySwordEntity sword = ToNekoEntities.FLY_SWORD_ENTITY.create(level);
        if (sword == null) return InteractionResult.FAIL;

        sword.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getPlayer().getYRot(), 0);

        // Load upgrades from item's CUSTOM_DATA (limit first, then levels)
        CustomData custom = context.getItemInHand().get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            if (tag.contains("maxLimit")) sword.setMaxUpgradeLimit(tag.getInt("maxLimit"));
            sword.setNetherStarUpgrade(tag.getBoolean("netherStar"));
            sword.setIronLevel(tag.getInt("iron"));
            sword.setDiamondLevel(tag.getInt("diamond"));
            sword.setNetheriteLevel(tag.getInt("netherite"));
        }

        level.addFreshEntity(sword);
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
