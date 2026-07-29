package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClimbWallHandler {
    private static final Set<UUID> climbingPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Float> verticalInputs = new ConcurrentHashMap<>();

    /** 向上爬时每 tick 能量消耗 */
    private static final double ENERGY_PER_TICK_UP = 0.8;
    /** 向下移动时每 tick 能量消耗 */
    private static final double ENERGY_PER_TICK_DOWN = 0.3;
    /** 静止悬挂时每 tick 能量消耗 */
    private static final double ENERGY_PER_TICK_CLING = 0.1;

    public static void startClimbing(ServerPlayer player, float verticalInput) {
        if (!player.isNeko()) return;
        if (player.getNekoEnergy() <= 0) {
            player.displayClientMessage(
                    Component.translatable("messages.toneko.climb.no_energy"), true);
            return;
        }
        if (!isAgainstWall(player)) {
            player.displayClientMessage(
                    Component.translatable("messages.toneko.climb.no_wall"), true);
            return;
        }
        UUID uuid = player.getUUID();
        climbingPlayers.add(uuid);
        verticalInputs.put(uuid, verticalInput);

        // 触发成就：猫爪攀墙
        ToNekoCriteria.NEKO_CLIMB.trigger(player);
    }

    public static void updateVerticalInput(ServerPlayer player, float verticalInput) {
        if (climbingPlayers.contains(player.getUUID())) {
            verticalInputs.put(player.getUUID(), verticalInput);
        }
    }

    public static void stopClimbing(ServerPlayer player) {
        UUID uuid = player.getUUID();
        climbingPlayers.remove(uuid);
        verticalInputs.remove(uuid);
    }

    public static boolean isClimbing(ServerPlayer player) {
        return climbingPlayers.contains(player.getUUID());
    }

    public static void onServerTick(MinecraftServer server) {
        if (climbingPlayers.isEmpty()) return;

        Iterator<UUID> iter = climbingPlayers.iterator();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);

            if (player == null || player.isRemoved()) {
                iter.remove();
                verticalInputs.remove(uuid);
                continue;
            }
            if (!player.isNeko()) {
                iter.remove();
                verticalInputs.remove(uuid);
                continue;
            }
            if (player.getNekoEnergy() <= 0) {
                player.displayClientMessage(
                        Component.translatable("messages.toneko.climb.no_energy"), true);
                iter.remove();
                verticalInputs.remove(uuid);
                continue;
            }
            if (!isAgainstWall(player)) {
                iter.remove();
                verticalInputs.remove(uuid);
                continue;
            }

            handleClimbingPhysics(player, uuid);
        }
    }

    /**
     * 检测玩家是否贴近墙壁（公开方法，可供客户端调用）。
     * 从玩家碰撞箱边缘向外检测。
     */
    public static boolean isAgainstWall(LivingEntity entity) {
        var box = entity.getBoundingBox();
        float yaw = entity.getYRot();
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);

        double edgeX, edgeZ;
        if (dx > 0) {
            edgeX = box.maxX + 0.05;
        } else if (dx < 0) {
            edgeX = box.minX - 0.05;
        } else {
            edgeX = entity.getX();
        }
        if (dz > 0) {
            edgeZ = box.maxZ + 0.05;
        } else if (dz < 0) {
            edgeZ = box.minZ - 0.05;
        } else {
            edgeZ = entity.getZ();
        }

        Level level = entity.level();
        BlockPos feetPos = BlockPos.containing(edgeX, entity.getY() - 0.1, edgeZ);
        BlockPos waistPos = BlockPos.containing(edgeX, entity.getY() + 1.0, edgeZ);
        BlockPos headPos = BlockPos.containing(edgeX, entity.getY() + 1.6, edgeZ);

        return level.getBlockState(feetPos).isSolid()
                || level.getBlockState(waistPos).isSolid()
                || level.getBlockState(headPos).isSolid();
    }

    private static void handleClimbingPhysics(ServerPlayer player, UUID uuid) {
        player.fallDistance = 0;
        player.setOnGround(true);
        player.zza = 0;
        player.xxa = 0;
        player.setDeltaMovement(0, 0, 0);

        float verticalInput = verticalInputs.getOrDefault(uuid, 0f);
        double energyCost;
        if (verticalInput > 0.5f) {
            energyCost = ENERGY_PER_TICK_UP;
        } else if (verticalInput < -0.5f) {
            energyCost = ENERGY_PER_TICK_DOWN;
        } else {
            energyCost = ENERGY_PER_TICK_CLING;
        }
        player.setNekoEnergy((float) Math.max(0, player.getNekoEnergy() - energyCost));
    }
}
