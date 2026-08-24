package org.cneko.toneko.common.mod.api;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 服务端姿势钉定表（意图表）。
 *
 * 这里只记录「希望钉定成什么姿势」，真正的姿势写入由 LivingEntityMixin
 * 在每 tick aiStep 末尾用普通 setPose 完成，并走原版 DATA_POSE 同步。
 * 使用 WeakHashMap：实体失去引用（卸载/重生/登出）后条目自动回收，防泄漏。
 */
public class EntityPoseManager {
    public static Map<Entity, Pose> poseMap = new WeakHashMap<>();
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
