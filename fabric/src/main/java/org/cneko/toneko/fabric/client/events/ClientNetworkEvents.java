package org.cneko.toneko.fabric.client.events;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.packets.VehicleStopRidePayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoEntityInteractivePayload;
import org.cneko.toneko.fabric.client.screens.NekoEntityInteractiveScreen;
import org.cneko.toneko.fabric.client.screens.QuirkScreen;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientNetworkEvents {
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(EntityPosePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                setPose(payload,context);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, (payload, context) ->{
            if (payload.isOpenScreen()) {
                // 打开屏幕
                context.client().execute(() -> {
                    // 打开设置屏幕
                    context.client().setScreen(new QuirkScreen(context.client().screen,payload.getQuirks(),payload.getAllQuirks()));
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NekoEntityInteractivePayload.ID, (payload, context) ->{
            context.client().execute(() -> {
                // 通过uuid寻找猫娘
                String uuid = payload.uuid();
                if(uuid != null && !uuid.isEmpty()) {
                    NekoEntity neko = findNearbyNekoByUuid(UUID.fromString(uuid),NekoEntity.DEFAULT_FIND_RANGE);
                    if(neko != null) {
                        // 打开屏幕
                        context.client().setScreen(new NekoEntityInteractiveScreen(neko));
                    }
                }
            });
        });

    }
    public static void setPose(EntityPosePayload payload, ClientPlayNetworking.Context context) {
        String uuid = payload.uuid();
        LivingEntity entity;
        if (uuid !=null){
            entity = findNearbyEntityByUuid(UUID.fromString(uuid),128);
            if (entity == null){
                entity = context.player();
            }
        }else {
            entity = context.player();
        }
        Pose pose = payload.pose();
        boolean status = payload.status();
        if(status) {
            ClientEntityPoseManager.setPose(entity, pose);
        }else ClientEntityPoseManager.remove(entity);
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
        // 确定搜索范围，这里以玩家为中心，半径为64个方块
        Player player = Minecraft.getInstance().player;
        AABB box = new AABB(player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range);

        Level world = player.level();
        // 遍历指定范围内的所有实体
        for (Entity entity : world.getEntities(player, box)) {
            if (entity.getUUID().equals(targetUuid)) {
                if (entity instanceof LivingEntity le) {
                    return le; // 找到了目标实体
                }else return null;
            }
        }

        return null; // 没有找到目标实体
    }


}