package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ToNekoNetworkEvents {
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, ToNekoNetworkEvents::onQuirkQueryNetWorking);
        ServerPlayNetworking.registerGlobalReceiver(GiftItemPayload.ID, ToNekoNetworkEvents::onGiftItem);
        ServerPlayNetworking.registerGlobalReceiver(FollowOwnerPayload.ID, ToNekoNetworkEvents::onFollowOwner);
        ServerPlayNetworking.registerGlobalReceiver(RideEntityPayload.ID, ToNekoNetworkEvents::onRideEntity);
        ServerPlayNetworking.registerGlobalReceiver(NekoPosePayload.ID, ToNekoNetworkEvents::onSetPose);
        ServerPlayNetworking.registerGlobalReceiver(NekoMatePayload.ID, ToNekoNetworkEvents::onBreed);
    }

    public static void onBreed(NekoMatePayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(payload.uuid()), neko -> {
            LivingEntity mate = findNearbyEntityByUuid(context.player(),UUID.fromString(payload.mateUuid()),10);
            if (mate instanceof INeko m){
                if (neko != m)
                    neko.tryMating((ServerLevel) context.player().level(), m);
            }
        });
    }

    public static void onSetPose(NekoPosePayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(payload.uuid()), neko -> {
            // 如果已经有姿势了，则移除
            if (EntityPoseManager.contains(neko)){
                EntityPoseManager.remove(neko);
                neko.setPose(Pose.STANDING);
            }else {
                EntityPoseManager.setPose(neko, payload.pose());
            }
        });
    }

    public static void onRideEntity(RideEntityPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(payload.uuid()), neko -> {
            LivingEntity entity = findNearbyEntityByUuid(context.player(),UUID.fromString(payload.vehicleUuid()),5);
            if (entity != null){
                if (neko.isSitting()){
                    neko.stopRiding();
                }else {
                    neko.startRiding(entity, true);
                    if (entity instanceof ServerPlayer sp) {
                        sp.connection.send(new ClientboundSetPassengersPacket(entity));
                    }
                }
            }
        });
    }

    public static void onFollowOwner(FollowOwnerPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(payload.uuid()), neko -> {
            neko.followOwner(context.player());
        });
    }

    public static void onGiftItem(GiftItemPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(payload.uuid()), neko -> {
            neko.giftItem(context.player(), payload.slot());
        });
    }

    public static void processNekoInteractive(ServerPlayer player, UUID targetUuid, EntityFinder finder) {
        // 寻找目标实体
        NekoEntity nekoEntity = findNearbyNekoByUuid(player, targetUuid,NekoEntity.DEFAULT_FIND_RANGE);
        // 如果实体与玩家太远，则不执行
        if(nekoEntity != null && !(nekoEntity.distanceToSqr(player) > 64)){
            // 处理代码
            finder.find(nekoEntity);
        }
    }

    @FunctionalInterface
    public interface EntityFinder {
        void find(NekoEntity nekoEntity);
    }

    public static void onQuirkQueryNetWorking(QuirkQueryPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!PermissionUtil.has(player, Permissions.COMMAND_QUIRK)){
            // 没有权限
            return;
        }
        // 保存数据
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        neko.setQuirksById(payload.getQuirks());
    }


    /**
     * 根据UUID查找附近的特定实体。
     * @param player 查找的玩家。
     * @param targetUuid 目标实体的UUID。
     * @return 找到的实体，如果没有找到则返回null。
     */
    public static @Nullable LivingEntity findNearbyEntityByUuid(ServerPlayer player, UUID targetUuid,double range) {
        // 确定搜索范围，以玩家为中心
        AABB box = new AABB(
                player.getX() - range,
                player.getY() - range,
                player.getZ() - range,
                player.getX() + range,
                player.getY() + range,
                player.getZ() + range
        );

        Level world = player.level();
        // 遍历指定范围内的所有实体
        for (Entity entity : world.getEntitiesOfClass(Entity.class, box)) {
            if (entity.getUUID().equals(targetUuid)) {
                if (entity instanceof LivingEntity le){
                    return le;
                }
            }
        }

        return null; // 没有找到目标实体
    }

    public static @Nullable NekoEntity findNearbyNekoByUuid(ServerPlayer player, UUID targetUuid,double rang) {
        LivingEntity entity = findNearbyEntityByUuid(player,targetUuid,rang);
        if (entity instanceof NekoEntity nekoEntity){
            return nekoEntity;
        }else {
            return null;
        }
    }
}

