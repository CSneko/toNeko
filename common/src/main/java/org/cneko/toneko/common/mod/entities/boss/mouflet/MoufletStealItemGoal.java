package org.cneko.toneko.common.mod.entities.boss.mouflet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tier;
import org.cneko.toneko.common.mod.items.CatnipItem;

import java.util.EnumSet;
import java.util.List;

public class MoufletStealItemGoal extends Goal {
    private final MoufletNekoBoss boss;
    private long lastStealFoodTime = 0;
    private long lastStealWeaponTime = 0;

    public MoufletStealItemGoal(MoufletNekoBoss boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        boolean canStealFood = boss.isFighting() &&
                boss.getHealth() < boss.getMaxHealth() * 0.8 &&
                boss.level().getGameTime() - lastStealFoodTime > 100; // 5秒CD

        boolean canStealWeapon = boss.isFighting() &&
                boss.getHealth() < boss.getMaxHealth() * 0.6 &&
                boss.level().getGameTime() - lastStealWeaponTime > 400; // 20秒CD

        return canStealFood || canStealWeapon;
    }

    @Override
    public void start() {
        List<Player> players = boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(8));
        boolean didSteal = false;

        // 先尝试偷武器/工具
        if (boss.getHealth() < boss.getMaxHealth() * 0.6 &&
                boss.level().getGameTime() - lastStealWeaponTime > 400) {
            for (Player player : players) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof TieredItem tiered) {
                        Tier tier = tiered.getTier();
                        float attackDamage = tier.getAttackDamageBonus() + 1.0F; // 1.0F为基础伤害
                        if (attackDamage > 10.0F) {
                            // 偷取武器/工具
                            boss.eatOrStoreFood(stack); // 这里可自定义为存储武器
                            player.getInventory().removeItem(i, 1);
                            lastStealWeaponTime = boss.level().getGameTime();
                            didSteal = true;
                            break;
                        }
                    }
                }
                if (didSteal) return;
            }
        }

        // 再尝试偷吃食物
        if (boss.getHealth() < boss.getMaxHealth() * 0.8 &&
                boss.level().getGameTime() - lastStealFoodTime > 100) {
            for (Player player : players) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                        if (stack.getItem() instanceof CatnipItem){
                            boss.eatenCatnip++;
                        }
                        boss.eatOrStoreFood(stack);
                        player.getInventory().removeItem(i, 1);
                        lastStealFoodTime = boss.level().getGameTime();
                        return;
                    }
                }
            }
        }
    }
}