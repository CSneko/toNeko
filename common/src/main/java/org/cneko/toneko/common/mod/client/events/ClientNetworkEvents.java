package org.cneko.toneko.common.mod.client.events;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.mixin.client.ClientLevelAccessor;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.client.screens.*;
import org.cneko.toneko.common.mod.client.util.ClientConfig;
import org.cneko.toneko.common.mod.client.util.ClientPlayerUtil;
import org.cneko.toneko.common.mod.client.util.ClientTextUtil;
import org.cneko.toneko.common.mod.packets.*;
import org.cneko.toneko.common.mod.packets.interactives.ChatHistoryResponsePayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoEntityInteractivePayload;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientNetworkEvents {
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(EntityPosePayload.ID, (payload, context) -> context.client().execute(() -> setPose(payload,context)));

        ClientPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, (payload, context) ->{
            if (payload.isOpenScreen()) {
                // 打开屏幕
                context.client().execute(() -> {
                    // 打开设置屏幕
                    context.client().setScreen(new QuirkScreen(context.client().screen,payload.getQuirks(),payload.getAllQuirks()));
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NekoEntityInteractivePayload.ID, (payload, context) -> context.client().execute(() -> {
            // 通过uuid寻找猫娘
            String uuid = payload.uuid();
            if(uuid != null && !uuid.isEmpty()) {
                NekoEntity neko = findNearbyNekoByUuid(UUID.fromString(uuid),NekoEntity.DEFAULT_FIND_RANGE);
                if(neko != null) {
                    // 打开屏幕
                    context.client().setScreen(new InteractionScreen(Component.empty(),neko,Minecraft.getInstance().screen, NekoScreenRegistry.get(neko.getType())));
                }
            }
        }));


        ClientPlayNetworking.registerGlobalReceiver(PlayerLeadByPlayerPayload.ID, (payload, context) -> context.client().execute(()->{
            // 获取玩家（如果存在的话）
            Player holder = ClientPlayerUtil.getPlayerByUUID(UUID.fromString(payload.holder()));
            Player target = ClientPlayerUtil.getPlayerByUUID(UUID.fromString(payload.target()));
            // 拴上玩家
            if (target != null && holder != null) {
                target.setLeashedTo(holder,false);
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(TTSSendPayload.ID, ((payload, context) -> context.client().execute(()->{
            if (ConfigUtil.isAITTSEnabled()){
                AIUtil.playTTS(payload.text(),ConfigUtil.getAITTSVoice());
            }
        })));

        ClientPlayNetworking.registerGlobalReceiver(NekoInfoSyncPayload.ID,(payload,context)-> context.client().execute(()->{
            Player player = context.player();
            player.setNekoEnergy(payload.energy());
            if (player instanceof org.cneko.toneko.common.mod.entities.INeko neko) {
                neko.setNeko(payload.isNeko());
                neko.setNekoLevelFactorRaw("interaction", payload.interactionRaw());
                neko.setNekoLevelFactorRaw("combat", payload.combatRaw());
                neko.setNekoLevelFactorRaw("base", payload.baseRaw());
                neko.setNekoLevelFactorRaw("exploration", payload.explorationRaw());
                neko.setNekoLevelFactorRaw("fishing", payload.fishingRaw());
                neko.setNekoLevelFactorRaw("homestead", payload.homesteadRaw());
                neko.setNekoAge(payload.age());
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(OpenPlotScreenPayload.ID,  (payload, context) -> context.client().execute(() -> {
            context.client().setScreen(new PlotScrollScreen());
        }));

        ClientPlayNetworking.registerGlobalReceiver(OpenNekoInfoScreenPayload.ID, (payload, context) -> context.client().execute(() -> {
            NekoInfoScreen.open();
        }));

        ClientPlayNetworking.registerGlobalReceiver(ToNekoManagementDataPayload.ID, (payload, context) -> context.client().execute(() -> {
            Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen instanceof ToNekoManagementScreen tms) {
                tms.handleDataUpdate(payload.data());
            } else {
                Minecraft.getInstance().setScreen(new ToNekoManagementScreen(payload.data(), currentScreen));
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(GenomeDataPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // 打开 UI，把数据传进去
                context.client().setScreen(new GeneticsScreen(payload.entityId(), payload.genomeNbt(), payload.canEdit()));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ChatHistoryResponsePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ChatWithNekoScreen.receiveHistory(
                        UUID.fromString(payload.nekoUuid()), payload.messages());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ChatStreamPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ChatWithNekoScreen.receiveStreamChunk(
                        UUID.fromString(payload.nekoUuid()), payload.chunk(), payload.finished(), payload.error());
            });
        });

        // AI 回复显示消息：客户端按配置选择聊天栏显示或猫娘头顶气泡
        ClientPlayNetworking.registerGlobalReceiver(NekoChatDisplayPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = context.client();
                if (mc.level == null || mc.player == null) return; // 断线竞态防护
                if (ClientConfig.isBubbleMode()) {
                    NekoBubbleRenderer.show(payload.nekoUuid(), payload.text());
                } else {
                    mc.gui.getChat().addMessage(ClientTextUtil.parseLegacyFormatting(payload.text()));
                }
            });
        });

    }
    public static void setPose(EntityPosePayload payload, ClientPlayNetworking.Context context) {
        String uuid = payload.uuid();
        if (uuid==null){
            return;
        }
        LivingEntity entity;
        if (uuid.equalsIgnoreCase("self")){
            entity = context.player();
        }else {
            entity = findNearbyEntityByUuid(UUID.fromString(uuid),128);
        }
        Pose pose = payload.pose();
        boolean status = payload.status();
        if(status) {
            ClientEntityPoseManager.setPose(entity, pose);
        }else{
            ClientEntityPoseManager.remove(entity);
        }
    }


    /**
     * 根据UUID查找附近的特定实体。
     * @param targetUuid 目标实体的UUID。
     * @return 找到的实体，如果没有找到则返回null。
     */
    public static @Nullable NekoEntity findNearbyNekoByUuid(UUID targetUuid,double range) {
        if (findNearbyEntityByUuid(targetUuid,range) instanceof NekoEntity nekoEntity){
            return nekoEntity;
        }
        return null;
    }

    public static @Nullable LivingEntity findNearbyEntityByUuid(UUID targetUuid,double range) {
        // 直接按 UUID 从客户端实体表中哈希查找，无需范围扫描（range 参数仅作兼容保留）
        Level world = Minecraft.getInstance().player.level();
        if (world instanceof ClientLevel clientLevel
                && ((ClientLevelAccessor) clientLevel).invokeGetEntities().get(targetUuid) instanceof LivingEntity le) {
            return le; // 找到了目标实体
        }
        return null; // 没有找到目标实体
    }


}
