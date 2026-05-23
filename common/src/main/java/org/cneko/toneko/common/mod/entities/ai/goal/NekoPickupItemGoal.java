package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.EntityUtil;

import java.util.EnumSet;
import java.util.List;

public class NekoPickupItemGoal extends Goal {
    private final NekoEntity neko;
    private ItemEntity targetItem;
    private int scanCooldown;
    private int timeToRecalcPath;
    private int cooldown;

    public NekoPickupItemGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!neko.getInventory().canAdd()) return false;
        int range = neko.getMoeTags().contains("shoakuma") ? 5 : 3;
        List<ItemEntity> items = EntityUtil.getItemEntitiesInRange(neko, neko.level(), range);
        for (ItemEntity item : items) {
            if (neko.isLikedItem(item.getItem())) {
                targetItem = item;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive()) return false;
        return neko.distanceToSqr(targetItem) <= 64 && neko.getInventory().canAdd();
    }

    @Override
    public void start() {
        scanCooldown = 0;
        timeToRecalcPath = 0;
    }

    @Override
    public void tick() {
        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive()) return;

        if (neko.distanceTo(targetItem) < 1.2) {
            neko.addItem(targetItem.getItem());
            targetItem.remove(Entity.RemovalReason.DISCARDED);
            targetItem = null;
            if (neko.getMoeTags().contains("dojikko") && neko.getRandom().nextFloat() < 0.25f) {
                neko.spawnAtLocation(neko.getRandomInventoryItem());
            }
        } else if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            neko.getNavigation().moveTo(targetItem, neko.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.6);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        cooldown = 40;
        neko.getNavigation().stop();
    }
}
