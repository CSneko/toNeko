package org.cneko.toneko.common.mod.packets;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.cneko.toneko.common.mod.packets.interactives.*;

public class ToNekoPackets {
    public static void init(){
        // 注册网络数据包
        PayloadTypeRegistry.clientboundPlay().register(EntityPosePayload.ID, EntityPosePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(QuirkQueryPayload.ID, QuirkQueryPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(QuirkQueryPayload.ID, QuirkQueryPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NekoEntityInteractivePayload.ID, NekoEntityInteractivePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GiftItemPayload.ID, GiftItemPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FollowOwnerPayload.ID, FollowOwnerPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RideEntityPayload.ID, RideEntityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NekoPosePayload.ID, NekoPosePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VehicleStopRidePayload.ID, VehicleStopRidePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NekoMatePayload.ID, NekoMatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChatWithNekoPayload.ID, ChatWithNekoPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChatHistoryRequestPayload.ID, ChatHistoryRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChatHistoryResponsePayload.ID, ChatHistoryResponsePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChatModePayload.ID, ChatModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MateWithCrystalNekoPayload.ID, MateWithCrystalNekoPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CrystalNekoNyaPayload.ID, CrystalNekoNyaPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DismountPassengerPayload.ID, DismountPassengerPayload.CODEC);
        // PlayerLeadByPlayerPayload 仅保留 S2C 注册：服务端主动推送拴绳状态，不接受客户端上报
        PayloadTypeRegistry.clientboundPlay().register(PlayerLeadByPlayerPayload.ID, PlayerLeadByPlayerPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PluginDetectPayload.ID, PluginDetectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PluginDetectPayload.ID, PluginDetectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TTSSendPayload.ID, TTSSendPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChatStreamPayload.ID, ChatStreamPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NekoChatDisplayPayload.ID, NekoChatDisplayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NekoInfoSyncPayload.ID, NekoInfoSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenPlotScreenPayload.ID, OpenPlotScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenNekoInfoScreenPayload.ID, OpenNekoInfoScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GenomeDataPayload.ID, GenomeDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GenomeDataPayload.ID, GenomeDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ToNekoManagementDataPayload.ID, ToNekoManagementDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NekoExpressAnimPayload.ID, NekoExpressAnimPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToNekoActionPayload.ID, ToNekoActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NekoMultiToolModePayload.ID, NekoMultiToolModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClimbWallPayload.ID, ClimbWallPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NekoStealthPayload.ID, NekoStealthPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LegwearAdjustPayload.ID, LegwearAdjustPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LegwearDyePayload.ID, LegwearDyePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LegwearPullUpPayload.ID, LegwearPullUpPayload.CODEC);
        // 玩味的踩
        PayloadTypeRegistry.serverboundPlay().register(StompActionPayload.ID, StompActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StompAnimPayload.ID, StompAnimPayload.CODEC);
    }
}
