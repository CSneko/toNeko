package org.cneko.toneko.bukkit.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class EntityUtil {
    /**
     * 从范围获取最近的实体
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 最近的LivingEntity或null
     */
    public static LivingEntity findNearestEntityInRange(Location center, double radius) {
        @NotNull Collection<Entity> entities = getLivingEntitiesInRange(center, radius);

        if (entities.isEmpty()) {
            return null;
        }

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity entity1){
                return entity1;
            }
        }
        return null;
    }

    /**
     * 获取指定范围内的所有LivingEntity
     *
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 包含所有在范围内的LivingEntity的列表
     */
    public static @NotNull Collection<Entity> getLivingEntitiesInRange(Location center, double radius) {
        Vector centerVector = center.toVector();
        Vector radiusVector = new Vector(radius, radius, radius);

        Vector min = centerVector.subtract(radiusVector);
        Vector max = centerVector.add(radiusVector);

        BoundingBox boundingBox = BoundingBox.of(min, max);

        return center.getWorld().getNearbyEntities(boundingBox);
    }
}

