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
        long foodCd = boss.isPetMode() ? 1200 : 600; // 驯服后60秒，否则30秒
        boolean canStealFood = boss.level().getGameTime() - lastStealFoodTime > foodCd;
        boolean canStealWeapon = boss.isFighting() &&
                boss.getHealth() < boss.getMaxHealth() * 0.6 &&
                boss.level().getGameTime() - lastStealWeaponTime > 400;
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
                        float attackDamage = tier.getAttackDamageBonus() + 1.0F;
                        if (attackDamage > 10.0F) {
                            boss.eatOrStoreFood(stack);
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

        // 偷食物：根据是否宠物模式决定cd
        long foodCd = boss.isPetMode() ? 1200 : 600;
        if (boss.level().getGameTime() - lastStealFoodTime > foodCd) {
            for (Player player : players) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                        if (stack.getItem() instanceof CatnipItem){
                            if (boss.isFighting()) {
                                boss.eatenCatnip++;
                            }
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