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
        PayloadTypeRegistry.playC2S().register(ChatHistoryRequestPayload.ID, ChatHistoryRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatHistoryResponsePayload.ID, ChatHistoryResponsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ChatModePayload.ID, ChatModePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MateWithCrystalNekoPayload.ID, MateWithCrystalNekoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CrystalNekoNyaPayload.ID, CrystalNekoNyaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DismountPassengerPayload.ID, DismountPassengerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerLeadByPlayerPayload.ID, PlayerLeadByPlayerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerLeadByPlayerPayload.ID, PlayerLeadByPlayerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PluginDetectPayload.ID, PluginDetectPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PluginDetectPayload.ID, PluginDetectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TTSSendPayload.ID, TTSSendPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatStreamPayload.ID, ChatStreamPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NekoChatDisplayPayload.ID, NekoChatDisplayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NekoInfoSyncPayload.ID, NekoInfoSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenPlotScreenPayload.ID, OpenPlotScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenNekoInfoScreenPayload.ID, OpenNekoInfoScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GenomeDataPayload.ID, GenomeDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GenomeDataPayload.ID, GenomeDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToNekoManagementDataPayload.ID, ToNekoManagementDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NekoExpressAnimPayload.ID, NekoExpressAnimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToNekoActionPayload.ID, ToNekoActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NekoMultiToolModePayload.ID, NekoMultiToolModePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClimbWallPayload.ID, ClimbWallPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NekoStealthPayload.ID, NekoStealthPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LegwearAdjustPayload.ID, LegwearAdjustPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LegwearDyePayload.ID, LegwearDyePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LegwearPullUpPayload.ID, LegwearPullUpPayload.CODEC);
        // 玩味的踩
        PayloadTypeRegistry.playC2S().register(StompActionPayload.ID, StompActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StompAnimPayload.ID, StompAnimPayload.CODEC);
    }
}
