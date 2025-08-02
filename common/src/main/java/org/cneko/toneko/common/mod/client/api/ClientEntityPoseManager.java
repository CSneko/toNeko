package org.cneko.toneko.common.mod.client.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ClientEntityPoseManager {
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
    public static @Nullable Pose getNullablePose(Entity entity) {
        return poseMap.get(entity);
    }
    public static void remove(Entity entity) {
        poseMap.remove(entity);
    }
}
