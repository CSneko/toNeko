package org.cneko.toneko.fabric.api;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class PlayerPoseAPI {
    public static Map<PlayerEntity, EntityPose> poseMap = new HashMap<>();
    public static void setPose(PlayerEntity player, EntityPose pose) {
        poseMap.put(player, pose);
    }
    public static boolean contains(PlayerEntity player) {
        return poseMap.containsKey(player);
    }
    public static EntityPose getPose(PlayerEntity player) {
        return poseMap.getOrDefault(player, EntityPose.STANDING);
    }
    public static void remove(PlayerEntity player) {
        poseMap.remove(player);
    }
}
