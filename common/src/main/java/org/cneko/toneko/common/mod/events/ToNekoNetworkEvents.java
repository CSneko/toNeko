package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.MateWithCrystalNekoPayload;
import org.cneko.toneko.common.mod.packets.PlayerLeadByPlayerPayload;
import org.cneko.toneko.common.mod.packets.PluginDetectPayload;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.UUID;

public class ToNekoNetworkEvents {
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, ToNekoNetworkEvents::onQuirkQueryNetWorking);
        ServerPlayNetworking.registerGlobalReceiver(GiftItemPayload.ID, ToNekoNetworkEvents::onGiftItem);
        ServerPlayNetworking.registerGlobalReceiver(FollowOwnerPayload.ID, ToNekoNetworkEvents::onFollowOwner);
        ServerPlayNetworking.registerGlobalReceiver(RideEntityPayload.ID, ToNekoNetworkEvents::onRideEntity);
        ServerPlayNetworking.registerGlobalReceiver(NekoPosePayload.ID, ToNekoNetworkEvents::onSetPose);
        ServerPlayNetworking.registerGlobalReceiver(NekoMatePayload.ID, ToNekoNetworkEvents::onBreed);
        ServerPlayNetworking.registerGlobalReceiver(ChatWithNekoPayload.ID, ToNekoNetworkEvents::onChatWithNeko);
        ServerPlayNetworking.registerGlobalReceiver(MateWithCrystalNekoPayload.ID, ToNekoNetworkEvents::onMateWithCrystalNeko);
        ServerPlayNetworking.registerGlobalReceiver(PlayerLeadByPlayerPayload.ID,ToNekoNetworkEvents::onPlayerLeadByPlayer);
        ServerPlayNetworking.registerGlobalReceiver(PluginDetectPayload.ID,(a,b)->{});// 什么也不干
    }

    public static void onPlayerLeadByPlayer(PlayerLeadByPlayerPayload payload, ServerPlayNetworking.Context context) {
        try{
            // 寻找对应玩家（如果有的话）
            Player holder = PlayerUtil.getPlayerByUUID(UUID.fromString(payload.holder()));
            Player target = PlayerUtil.getPlayerByUUID(UUID.fromString(payload.target()));
            // 告诉玩家自己被拴上了
            ServerPlayNetworking.send((ServerPlayer) holder, new PlayerLeadByPlayerPayload(holder.getUUID().toString(),target.getUUID().toString()));
            ServerPlayNetworking.send((ServerPlayer) target, new PlayerLeadByPlayerPayload(holder.getUUID().toString(),target.getUUID().toString()));
        }catch (Exception ignored){
        }
    }

    public static void onMateWithCrystalNeko(MateWithCrystalNekoPayload mateWithCrystalNekoPayload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(mateWithCrystalNekoPayload.uuid()), neko -> {
            if (neko instanceof CrystalNekoEntity cneko){
                cneko.tryMating((ServerLevel) neko.level(), context.player());
            }
        });
    }

    public static void onChatWithNeko(ChatWithNekoPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(payload.uuid()), neko -> {
            // 如果没有开启AI，则不执行
            if (!ConfigUtil.isAIEnabled()){
                context.player().sendSystemMessage(Component.translatable("messages.toneko.ai.not_enabled"));
            }else {
                AIUtil.sendMessage(neko.getUUID(),context.player().getUUID(), neko.generateAIPrompt(context.player()), payload.message(), message -> {
                    String r = Messaging.format(message,neko.getCustomName().getString(),"", Collections.singletonList(LanguageUtil.prefix),ConfigUtil.getChatFormat());
                    context.player().sendSystemMessage(Component.literal(r));
                });
            }
        });
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

