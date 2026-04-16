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

    // 缓存目标物品,扫描冷却和寻路冷却
    private ItemEntity targetItem;
    private int scanCooldown = 0;
    private int timeToRecalcPath = 0;

    public NekoPickupItemGoal(NekoEntity neko) {
        this.neko = neko;
    }

    @Override
    public void tick() {
        super.tick();
        if (scanCooldown > 0) scanCooldown--;

        // 1. 如果没有目标，或者目标失效，或者离得太远，则重新扫描
        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive() || neko.distanceToSqr(targetItem) > 64) {
            targetItem = null; // 重置目标
            if (scanCooldown <= 0) {
                scanCooldown = 20; // 找不到目标时，每 20 tick (1秒) 扫描一次范围，而不是每 tick 都扫
                List<ItemEntity> items = EntityUtil.getItemEntitiesInRange(neko, neko.level(), 3);
                for (ItemEntity item : items) {
                    if (neko.isLikedItem(item.getItem()) && neko.getInventory().canAdd()) {
                        targetItem = item; // 找到目标，锁定它
                        break;
                    }
                }
            }
        }

        // 2. 如果锁定了目标，则向目标移动
        if (targetItem != null) {
            if (neko.distanceTo(targetItem) < 1.2) { // 稍微扩大一点判定范围防止捡不到卡住
                neko.addItem(targetItem.getItem());
                targetItem.remove(Entity.RemovalReason.DISCARDED);
                targetItem = null;
            } else {
                // 每 10 tick 寻路一次
                if (--timeToRecalcPath <= 0) {
                    timeToRecalcPath = 10;
                    neko.getNavigation().moveTo(targetItem, 0.4);
                }
            }
        }
    }

    @Override
    public boolean canUse() {
        return (neko.getInventory().canAdd()) && !stop;
    }

    @Override
    public void stop() {
        super.stop();
        stop = true;
        targetItem = null;
    }

    @Override
    public void start() {
        super.start();
        stop = false;
        targetItem = null;
        scanCooldown = 0;
        timeToRecalcPath = 0;
    }
}
