package org.cneko.toneko.fabric.api;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public class PlayerPoseAPI {
    public static Map<Player, Pose> poseMap = new HashMap<>();
    public static void setPose(Player player, Pose pose) {
        poseMap.put(player, pose);
    }
    public static boolean contains(Player player) {
        return poseMap.containsKey(player);
    }
    public static Pose getPose(Player player) {
        return poseMap.getOrDefault(player, Pose.STANDING);
    }
    public static void remove(Player player) {
        poseMap.remove(player);
    }
}
