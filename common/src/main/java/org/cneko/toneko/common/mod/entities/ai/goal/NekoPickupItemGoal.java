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
    public NekoPickupItemGoal(NekoEntity neko) {
        this.neko = neko;
    }

    @Override
    public void tick() {
        super.tick();
        // 检查附近3格内是否有物品
        List<ItemEntity> items = EntityUtil.getItemEntitiesInRange(neko, neko.level(), 3);
        for (ItemEntity item : items){
            // 喜欢物品则拾取
            if (neko.isLikedItem(item.getItem()) && !neko.getInventory().isFull()){
                // 如果在1格以内直接拾取
                if (neko.distanceTo(item) < 1){
                    neko.getInventory().add(item.getItem());
                    item.remove(Entity.RemovalReason.DISCARDED);
                }else {
//                    // 否则尝试寻路（划掉）飞过去
//                    neko.moveTo(item.getX(), item.getY(), item.getZ());
                    neko.getNavigation().moveTo(item, 0.4);
                }
            }
        }
    }

    @Override
    public boolean canUse() {
        return (!neko.getInventory().isFull()) && !stop;
    }

    @Override
    public void stop() {
        super.stop();
        stop = true;
    }

    @Override
    public void start() {
        super.start();
        stop = false;
    }
}
