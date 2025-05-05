package org.cneko.toneko.common.mod.packets;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.cneko.toneko.common.mod.packets.interactives.*;

public class ToNekoPackets {
    public static void init(){
        // 注册网络数据包
        PayloadTypeRegistry.playS2C().register(EntityPosePayload.ID, EntityPosePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(QuirkQueryPayload.ID, QuirkQueryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QuirkQueryPayload.ID, QuirkQueryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NekoEntityInteractivePayload.ID, NekoEntityInteractivePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GiftItemPayload.ID, GiftItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FollowOwnerPayload.ID, FollowOwnerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RideEntityPayload.ID, RideEntityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NekoPosePayload.ID, NekoPosePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VehicleStopRidePayload.ID, VehicleStopRidePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NekoMatePayload.ID, NekoMatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ChatWithNekoPayload.ID, ChatWithNekoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MateWithCrystalNekoPayload.ID, MateWithCrystalNekoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerLeadByPlayerPayload.ID, PlayerLeadByPlayerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerLeadByPlayerPayload.ID, PlayerLeadByPlayerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PluginDetectPayload.ID, PluginDetectPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PluginDetectPayload.ID, PluginDetectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TTSSendPayload.ID, TTSSendPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NekoInfoSyncPayload.ID, NekoInfoSyncPayload.CODEC);
    }
}
