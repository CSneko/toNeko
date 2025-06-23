package org.cneko.toneko.common.mod.items;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;
import org.cneko.toneko.common.mod.util.RandomUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContractItem extends Item {
    public ContractItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        if (player.level().isClientSide) return super.interactLivingEntity(stack,player,interactionTarget,usedHand);
        if (interactionTarget instanceof INeko neko){
            if (!neko.isNeko()){
                player.sendSystemMessage(Component.translatable("item.toneko.contract.not_a_neko"));
                return InteractionResult.FAIL;
            }
            // 如果是主人
            if (neko.hasOwner(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("item.toneko.contract.already_owner"));
                return InteractionResult.FAIL;
            }else {
                if (neko instanceof NekoEntity nekoEntity) {
                    if (nekoEntity.getMoeTags().contains("mesugaki") && !EnchantmentUtil.hasEnchantment(ToNekoEnchantments.ENFORCEMENT_ID,stack)){
                        player.sendSystemMessage(Component.translatable("item.toneko.contract.mesugaki."+ RandomUtil.randomInt(0,10),neko.getEntity().getName()));
                        return InteractionResult.FAIL;
                    }
                }
                if (player.experienceLevel <= 30){
                    player.sendSystemMessage(Component.translatable("item.toneko.contract.not_enough_xp"));
                    return InteractionResult.FAIL;
                }else {
                    neko.addOwner(player.getUUID(), new INeko.Owner(List.of(),0));
                    player.giveExperienceLevels(-30);
                    player.sendSystemMessage(Component.translatable("item.toneko.contract.success",neko.getEntity().getName()));
                    // 删除物品
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }


    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 30;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        return enchantment.is(ToNekoEnchantments.ENFORCEMENT);
    }
}
