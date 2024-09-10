package org.cneko.toneko.common.mod.api;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class EntityPoseManager {
    public static Map<Entity, Pose> poseMap = new HashMap<>();
    public static void setPose(Entity entity, Pose pose) {
        poseMap.put(entity, pose);
    }
    public static boolean contains(Entity entity) {
        return poseMap.containsKey(entity);
    }
    public static Pose getPose(Entity entity) {
        return poseMap.getOrDefault(entity, Pose.STANDING);
    }
    public static void remove(Entity entity) {
        poseMap.remove(entity);
    }
}
