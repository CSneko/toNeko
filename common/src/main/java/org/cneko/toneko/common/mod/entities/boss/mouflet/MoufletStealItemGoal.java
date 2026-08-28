package org.cneko.toneko.common.mod.entities.boss.mouflet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.cneko.toneko.common.mod.items.CatnipItem;

import java.util.List;

public class MoufletStealItemGoal extends Goal {
    private final MoufletNekoBoss boss;
    private long lastStealFoodTime = 0;
    private long lastStealWeaponTime = 0;
    private long lastStealCatnipTime = 0;
    private static final long CATNIP_COOLDOWN = 100; // 猫薄荷独立冷却 5 秒

    public MoufletStealItemGoal(MoufletNekoBoss boss) {
        this.boss = boss;
        // 不设置标志 —— 本 Goal 只遍历玩家背包，不控制移动或观察
    }

    @Override
    public boolean canUse() {
        // 魅惑状态下不偷东西
        if (boss.isCharmed()) return false;

        boolean canStealCatnip = boss.isFighting() &&
                boss.level().getGameTime() - lastStealCatnipTime > CATNIP_COOLDOWN;
        long foodCd = boss.isPetMode() ? 600 : 300; // 驯服后30秒，否则15秒
        boolean canStealFood = boss.level().getGameTime() - lastStealFoodTime > foodCd;
        boolean canStealWeapon = boss.isFighting() &&
                boss.getHealth() < boss.getMaxHealth() * 0.6 &&
                boss.level().getGameTime() - lastStealWeaponTime > 400;
        return canStealCatnip || canStealFood || canStealWeapon;
    }

    @Override
    public void start() {
        List<Player> players = boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(8));

        // 1. 优先偷吃猫薄荷（独立冷却，战斗中每 5 秒尝试）
        if (boss.isFighting() && boss.level().getGameTime() - lastStealCatnipTime > CATNIP_COOLDOWN) {
            for (Player player : players) {
                if (boss.isPetMode() && boss.hasOwner(player.getUUID())) continue;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof CatnipItem) {
                        boss.eatenCatnip++;
                        // eatOrStoreFood 契约：成功时恰好消耗 1 个，无需再 removeItem
                        boss.eatOrStoreFood(stack);
                        lastStealCatnipTime = boss.level().getGameTime();
                        return; // 猫薄荷独立处理，不阻塞其他偷窃
                    }
                }
            }
        }

        // 2. 尝试偷武器/工具
        if (boss.getHealth() < boss.getMaxHealth() * 0.6 &&
                boss.level().getGameTime() - lastStealWeaponTime > 400) {
            for (Player player : players) {
                if (boss.isPetMode() && boss.hasOwner(player.getUUID())) continue;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.has(DataComponents.TOOL)) {
                        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                        if (modifiers == null) continue;
                        double attackDamage = 1.0D;
                        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                            if (entry.attribute() == Attributes.ATTACK_DAMAGE
                                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                                attackDamage += entry.modifier().amount();
                            }
                        }
                        if (attackDamage > 10.0F) {
                            boss.eatOrStoreFood(stack); // 契约：成功时恰好消耗 1 个
                            lastStealWeaponTime = boss.level().getGameTime();
                            return;
                        }
                    }
                }
            }
        }

        // 3. 偷普通食物
        long foodCd = boss.isPetMode() ? 600 : 300; // 驯服后30秒，否则15秒
        if (boss.level().getGameTime() - lastStealFoodTime > foodCd) {
            for (Player player : players) {
                if (boss.isPetMode() && boss.hasOwner(player.getUUID())) continue;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                        boss.eatOrStoreFood(stack); // 契约：成功时恰好消耗 1 个
                        lastStealFoodTime = boss.level().getGameTime();
                        return;
                    }
                }
            }
        }
    }

}