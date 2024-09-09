package org.cneko.toneko.fabric;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.api.PlayerInstallToNeko;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.events.*;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.common.mod.quirks.ModQuirk;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.quirks.Quirk;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ToNekoEvents {
    public static void init() {
        if(ConfigUtil.CHAT_ENABLE) {
            ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
                CommonChatEvent.onChatMessage(message, sender, params);
                return false;
            });
        }
        ServerPlayConnectionEvents.JOIN.register(ToNekoEvents::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(ToNekoEvents::onPlayerQuit);
        UseEntityCallback.EVENT.register(CommonPlayerInteractionEvent::useEntity);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CommonPlayerInteractionEvent::onDamage);
        AttackEntityCallback.EVENT.register(CommonPlayerInteractionEvent::onAttackEntity);
        ServerTickEvents.START_SERVER_TICK.register(CommonPlayerTickEvent::startTick);
        ServerWorldEvents.UNLOAD.register(CommonWorldEvent::onWorldUnLoad);
        ServerPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, ToNekoEvents::onQuirkQueryNetWorking);
        ServerPlayNetworking.registerGlobalReceiver(GiftItemPayload.ID, ToNekoEvents::onGiftItem);
    }

    public static void onGiftItem(GiftItemPayload payload, ServerPlayNetworking.Context context) {
        // 寻找目标实体
        NekoEntity nekoEntity = findNearbyEntityByUuid(context.player(),UUID.fromString(payload.uuid()));
        if(nekoEntity != null){
            nekoEntity.giftItem(context.player(), payload.slot());
        }
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


    public static void onPlayerJoin(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        if(neko.isNeko()){
            // 修复quirks
            neko.fixQuirks();
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.addPrivatePrefix(name, LanguageUtil.prefix);
            for (Quirk quirk : neko.getQuirks()){
                if (quirk instanceof ModQuirk mq){
                    mq.onJoin(player);
                }
            }
        }
    }

    public static void onPlayerQuit(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        if(neko.isNeko()){
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.removePrivatePrefix(name, LanguageUtil.prefix);
        }
        // 保存猫娘数据
        neko.save();
        NekoQuery.NekoData.removeNeko(player.getUUID());
    }


    /**
     * 根据UUID查找附近的特定实体。
     * @param player 查找的玩家。
     * @param targetUuid 目标实体的UUID。
     * @return 找到的实体，如果没有找到则返回null。
     */
    public static @Nullable NekoEntity findNearbyEntityByUuid(ServerPlayer player, UUID targetUuid) {
        // 确定搜索范围，这里以玩家为中心，半径为64个方块
        double range = 64;
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
                if (entity instanceof NekoEntity nekoEntity) {
                    return nekoEntity; // 找到了目标实体
                }
            }
        }

        return null; // 没有找到目标实体
    }
}
