package org.cneko.toneko.common.mod.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NineLivesCharmItem extends Item {
    public static final String ID = "nine_lives_charm";
    public static final int MAX_LIVES = 9;

    public NineLivesCharmItem() {
        super(new Properties().stacksTo(1).durability(MAX_LIVES).rarity(Rarity.RARE));
    }

    /**
     * 当玩家即将死亡时调用。尝试消耗护符阻止死亡。
     *
     * @param player 即将死亡的玩家
     * @return {@code true} 表示允许死亡（护符未触发），{@code false} 表示死亡被阻止
     */
    public static boolean tryPreventDeath(ServerPlayer player) {
        if (player.isNeko()) {
            return tryUseAsNeko(player);
        } else {
            return tryUseAsNonNeko(player);
        }
    }

    // ========================
    //  猫娘玩家：扫描全背包（含副手）
    // ========================
    private static boolean tryUseAsNeko(ServerPlayer player) {
        Inventory inv = player.getInventory();

        // 先检查副手
        ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (tryConsumeCharmForNeko(player, offhand)) return false;

        // 扫描主背包 + 护甲栏
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (tryConsumeCharmForNeko(player, stack)) return false;
        }
        return true; // 没有可用的护符
    }

    /** 尝试消耗护符（猫娘版），成功返回 true */
    private static boolean tryConsumeCharmForNeko(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof NineLivesCharmItem)) return false;
        if (isBroken(stack)) return false;
        float cost = calcEnergyCost(stack);
        if (player.getNekoEnergy() < cost) return false;

        // 触发护符
        player.setNekoEnergy(player.getNekoEnergy() - cost);
        int remainingBefore = getRemainingLives(stack);
        consumeLife(stack);
        applyResurrection(player);
        broadcastUse(player, remainingBefore - 1);
        // 触发成就：九命猫
        ToNekoCriteria.NINE_LIVES.trigger(player);
        return true;
    }

    // ========================
    //  非猫娘玩家：仅副手 + 消耗充能注能石
    // ========================
    private static boolean tryUseAsNonNeko(ServerPlayer player) {
        ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!(offhand.getItem() instanceof NineLivesCharmItem)) return true;
        if (isBroken(offhand)) return true;

        // 寻找充能注能石，优先消耗容量最小的
        int stoneSlot = findChargedStoneSlot(player);
        if (stoneSlot == -1) return true;

        // 消耗注能石
        player.getInventory().getItem(stoneSlot).shrink(1);
        // 消耗护符耐久
        int remainingBefore = getRemainingLives(offhand);
        consumeLife(offhand);
        applyResurrection(player);
        broadcastUse(player, remainingBefore - 1);
        // 触发成就：九命猫（非猫娘也能触发）
        ToNekoCriteria.NINE_LIVES.trigger(player);
        return false;
    }

    // ========================
    //  辅助方法
    // ========================

    private static boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    private static int getRemainingLives(ItemStack charm) {
        return charm.getMaxDamage() - charm.getDamageValue();
    }

    /** 计算猫娘玩家的能量消耗 */
    private static float calcEnergyCost(ItemStack charm) {
        int remaining = getRemainingLives(charm);
        return 150 + (MAX_LIVES - remaining) * 20;
    }

    /** 消耗护符的 1 点耐久，耐久归零时物品消失 */
    private static void consumeLife(ItemStack stack) {
        stack.setDamageValue(stack.getDamageValue() + 1);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            stack.shrink(1);
        }
    }

    /**
     * 在背包中寻找充能注能石，返回最优（容量最小）的那格的索引。
     * @return 格子索引，找不到返回 -1
     */
    private static int findChargedStoneSlot(ServerPlayer player) {
        Inventory inv = player.getInventory();
        int bestSlot = -1;
        double bestCapacity = Double.MAX_VALUE;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof NekoEnergyStorageItem stone && stone.isCharged()) {
                if (stone.getEnergyCapacity() < bestCapacity) {
                    bestCapacity = stone.getEnergyCapacity();
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    /** 应用复活效果（比原版不死图腾弱） */
    private static void applyResurrection(ServerPlayer player) {
        // 清除现有效果
        player.removeAllEffects();
        // 恢复到 30% 血量
        player.setHealth(player.getMaxHealth() * 0.3f);
        // 效果：吸收等级比原版图腾低（图腾是 Absorption IV）
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));    // 生命恢复 II, 45s
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));      // 伤害吸收 II, 5s
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)); // 抗火 I, 40s
        // 短暂伤害免疫
        player.invulnerableTime = 20;

        // 图腾粒子动画
        player.level().broadcastEntityEvent(player, (byte) 35);
        // 音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, player.getSoundSource(), 1.0F, 1.0F);
    }

    private static void broadcastUse(ServerPlayer player, int remainingAfter) {
        if (player.getServer() == null) return;
        String msg = Component.translatable("message.toneko.nine_lives_charm.use",
                player.getName().getString(), remainingAfter).getString();
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }

    // ========================
    //  Tooltip
    // ========================
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        int remaining = getRemainingLives(stack);
        tooltipComponents.add(Component.translatable("item.toneko.nine_lives_charm.tip", remaining));
        tooltipComponents.add(Component.translatable("item.toneko.nine_lives_charm.tip.non_neko"));
    }
}
