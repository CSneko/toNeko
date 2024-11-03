package org.cneko.toneko.common.mod.util;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EntityUtil {
    /**
     * 随机设置属性值
     *
     * @param entity          实体
     * @param attribute       属性的
     * @param baseValue       基础值
     * @param min             最小值
     * @param max             最大值
     */
    public static void randomizeAttributeValue(LivingEntity entity, Holder<Attribute> attribute, double baseValue, double min, double max) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null && attributeInstance.getValue() == baseValue) {
            double newValue = min + (max - min) * Math.random();
            attributeInstance.setBaseValue(newValue);
        }
    }


    // 从范围获取实体
    public static LivingEntity findNearestEntityInRange(Entity entity, Level world, float radius) {
        List<LivingEntity> entities = getLivingEntitiesInRange(entity, world, radius);

        for (LivingEntity entity1 : entities) {
            return entity1;
        }
        return null;
    }

    // 获取范围内所有LivingEntity
    public static List<LivingEntity> getLivingEntitiesInRange(Entity entity, Level world, float radius) {
        AABB box = new AABB(entity.getX() - radius, entity.getY() - radius, entity.getZ() - radius,
                entity.getX() + radius, entity.getY() + radius, entity.getZ() + radius);
        return world.getEntitiesOfClass(LivingEntity.class, box);
    }

    // 获取范围内所有ItemEntity
    public static List<ItemEntity> getItemEntitiesInRange(Entity entity, Level world, float radius) {
        AABB box = new AABB(entity.getX() - radius, entity.getY() - radius, entity.getZ() - radius,
                entity.getX() + radius, entity.getY() + radius, entity.getZ() + radius);
        return world.getEntitiesOfClass(ItemEntity.class, box);
    }


}
