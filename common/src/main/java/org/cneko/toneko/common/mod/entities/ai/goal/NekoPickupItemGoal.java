package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.EntityUtil;

import java.util.List;

public class NekoPickupItemGoal extends Goal {
    private final NekoEntity neko;
    private boolean stop = false;
    private ItemEntity targetItem;
    private int scanCooldown = 0;
    private int timeToRecalcPath = 0;

    public NekoPickupItemGoal(NekoEntity neko) {
        this.neko = neko;
    }

    @Override
    public void tick() {
        if (scanCooldown > 0) scanCooldown--;

        int range = neko.getMoeTags().contains("shoakuma") ? 5 : 3;

        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive() || neko.distanceToSqr(targetItem) > 64) {
            targetItem = null;
            if (scanCooldown <= 0) {
                scanCooldown = 20;
                List<ItemEntity> items = EntityUtil.getItemEntitiesInRange(neko, neko.level(), range);
                for (ItemEntity item : items) {
                    if (neko.isLikedItem(item.getItem()) && neko.getInventory().canAdd()) {
                        targetItem = item;
                        break;
                    }
                }
            }
        }

        if (targetItem != null) {
            if (neko.distanceTo(targetItem) < 1.2) {
                neko.addItem(targetItem.getItem());
                targetItem.remove(Entity.RemovalReason.DISCARDED);
                targetItem = null;
                // dojikko: 25% chance to drop a random item after pickup
                if (neko.getMoeTags().contains("dojikko") && neko.getRandom().nextFloat() < 0.25f) {
                    neko.spawnAtLocation(neko.getRandomInventoryItem());
                }
            } else {
                if (--timeToRecalcPath <= 0) {
                    timeToRecalcPath = 10;
                    neko.getNavigation().moveTo(targetItem, 0.4);
                }
            }
        }
    }

    @Override
    public boolean canUse() {
        return neko.getInventory().canAdd() && !stop;
    }

    @Override
    public void stop() {
        stop = true;
        targetItem = null;
    }

    @Override
    public void start() {
        stop = false;
        targetItem = null;
        scanCooldown = 0;
        timeToRecalcPath = 0;
    }
}
