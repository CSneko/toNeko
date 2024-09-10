package org.cneko.toneko.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ToNekoNetworkEvents {
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, ToNekoNetworkEvents::onQuirkQueryNetWorking);
        ServerPlayNetworking.registerGlobalReceiver(GiftItemPayload.ID, ToNekoNetworkEvents::onGiftItem);
        ServerPlayNetworking.registerGlobalReceiver(FollowOwnerPayload.ID, ToNekoNetworkEvents::onFollowOwner);
    }

    public static void onFollowOwner(FollowOwnerPayload followOwnerPayload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), UUID.fromString(followOwnerPayload.uuid()), neko -> {
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
        NekoEntity nekoEntity = findNearbyEntityByUuid(player, targetUuid);
        if(nekoEntity != null){
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

