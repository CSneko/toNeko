package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.ai.core.AIResponse;
import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.MateWithCrystalNekoPayload;
import org.cneko.toneko.common.mod.packets.PlayerLeadByPlayerPayload;
import org.cneko.toneko.common.mod.packets.PluginDetectPayload;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.mod.util.TickTaskQueue;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        processNekoInteractive(context.player(), mateWithCrystalNekoPayload.uuid(), neko -> {
            if (neko instanceof CrystalNekoEntity cneko){
                cneko.tryMating((ServerLevel) neko.level(), context.player());
            }
        });
    }

    public static void onChatWithNeko(ChatWithNekoPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            // 如果没有开启 AI，则不执行
            if (!ConfigUtil.isAIEnabled()){
                context.player().sendSystemMessage(Component.translatable("messages.toneko.ai.not_enabled"));
            } else {
                ServerPlayer player = context.player();
                AIUtil.sendMessage(neko.getUUID(), player.getUUID(), neko.generateAIPrompt(context.player()), payload.message(), response -> {
                    if (response.hasThink()){
                        ServerLevel world = (ServerLevel) neko.level();
                        // 使用多行 ArmorStand 显示思考过程，顺序逐行显示
                        int totalDelay = spawnFloatingText(neko, response, world);
                        // 在所有行动画完成后，再发送最终消息
                        TickTaskQueue task = new TickTaskQueue();
                        task.addTask(totalDelay, () -> {
                            String r = Messaging.format(response.getResponse(), neko.getCustomName().getString(), "",
                                    Collections.singletonList(LanguageUtil.prefix), ConfigUtil.getChatFormat());
                            player.sendSystemMessage(Component.literal(r));
                        });
                        TickTasks.add(task);
                    } else {
                        String r = Messaging.format(response.getResponse(), neko.getCustomName().getString(), "",
                                Collections.singletonList(LanguageUtil.prefix), ConfigUtil.getChatFormat());
                        context.player().sendSystemMessage(Component.literal(r));
                    }
                });
            }
        });
    }

    // 将完整文本分割成若干行，每行固定 lineLength 长度字符
    public static List<String> splitText(String text, int lineLength) {
        List<String> lines = new ArrayList<>();
        int currentLength = 0;
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 如果是换行符，立即分割
            if (c == '\n') {
                lines.add(currentLine.toString());
                currentLine.setLength(0);  // 清空当前行
                currentLength = 0;
                continue;
            }

            // 判断字符的长度（英文字符为1，其他字符为2）
            int charLength = (Character.isLetterOrDigit(c) || c == ' ' || c == '.' || c == ',' || c == '?' || c == '!' || c == ';' || c == ':' || c == '"') ? 1 : 2;

            // 如果添加当前字符后，超过行长度，分割当前行
            if (currentLength + charLength > lineLength) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);  // 清空当前行
                currentLength = 0;
            }

            // 添加当前字符
            currentLine.append(c);
            currentLength += charLength;
        }

        // 添加最后一行（如果有的话）
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }


    /**
     * 在目标实体上方生成多行 ArmorStand，每一行动画顺序依次显示。
     * @return 返回总的延时（tick数），用于后续任务调度（例如发送最终消息、清除文本）。
     */
    private static int spawnFloatingText(NekoEntity neko, AIResponse response, ServerLevel world) {
        double baseY = neko.getY() + neko.getBbHeight() + 0.5;
        double baseX = neko.getX();
        double baseZ = neko.getZ();

        List<String> lines = splitText(response.getThink(), 40);
        List<ArmorStand> armorStands = new ArrayList<>();

        int gap = 1; // 行间间隔时间（tick）
        int cumulativeDelay = 0;

        for (int i = 0; i < lines.size(); i++) {
            // 所有行生成在相同Y坐标
            ArmorStand lineStand = new ArmorStand(world, baseX, baseY, baseZ);
            lineStand.setInvisible(true);
            lineStand.setNoGravity(true);
            lineStand.setMarker(true);
            lineStand.setCustomNameVisible(true);
            lineStand.setCustomName(Component.literal(""));
            world.addFreshEntity(lineStand);
            armorStands.add(lineStand);

            String line = lines.get(i);
            animateLine(lineStand, line, cumulativeDelay);

            // 在行动画结束后移动所有已存在的盔甲架
            int currentIndex = i;
            int lineEndTime = cumulativeDelay + line.length();

            TickTaskQueue moveUpQueue = new TickTaskQueue();
            moveUpQueue.addTask(lineEndTime, () -> {
                for (int j = 0; j <= currentIndex; j++) {
                    ArmorStand as = armorStands.get(j);
                    as.setPos(as.getX(), as.getY() + 0.3, as.getZ());
                }
            });
            TickTasks.add(moveUpQueue);

            cumulativeDelay += line.length() + gap;
        }

        // 调整移除任务：在最后一行移动后100tick移除
        if (!lines.isEmpty()) {
            int lastLineLength = lines.getLast().length();
            int lastLineEndTime = cumulativeDelay - gap + lastLineLength;
            int removalTime = lastLineEndTime + 100;

            TickTaskQueue removalQueue = new TickTaskQueue();
            removalQueue.addTask(removalTime, () -> {
                for (ArmorStand as : armorStands) {
                    as.remove(Entity.RemovalReason.DISCARDED);
                }
            });
            TickTasks.add(removalQueue);
        }

        // 同步任务：持续到移除前
        TickTaskQueue syncQueue = new TickTaskQueue();
        int syncDuration = !lines.isEmpty() ? (cumulativeDelay - gap + lines.getLast().length() + 100) : 0;
        syncQueue.addRepeatingTask(0, syncDuration, () -> {
            double currentBaseX = neko.getX();
            double currentBaseZ = neko.getZ();
            for (ArmorStand as : armorStands) {
                // 仅同步X和Z坐标，Y坐标由移动任务维护
                as.setPos(currentBaseX, as.getY(), currentBaseZ);
            }
        });
        TickTasks.add(syncQueue);

        return cumulativeDelay;
    }

    /**
     * 对指定的 ArmorStand 进行逐字打字机效果动画。
     * @param stand 目标 ArmorStand
     * @param fullLine 完整文本内容
     * @param delayOffset 延时偏移（tick），即从这个 tick 开始显示该行的动画
     */
    private static void animateLine(ArmorStand stand, String fullLine, int delayOffset) {
        int totalLength = fullLine.length();
        TickTaskQueue queue = new TickTaskQueue();
        // 每个 tick 显示一个新字符（可根据需求调整）
        for (int i = 0; i < totalLength; i++) {
            int currentIndex = i;
            queue.addTask(delayOffset + i, () -> {
                // 显示从0到当前索引的子串
                String currentText = fullLine.substring(0, currentIndex + 1);
                stand.setCustomName(Component.literal(currentText));
            });
        }
        TickTasks.add(queue);
    }



    public static void onBreed(NekoMatePayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            Entity mate = findNearbyEntityByUuid(context.player(),UUID.fromString(payload.mateUuid()),10);
            if (mate instanceof INeko m){
                if (neko != m)
                    neko.tryMating((ServerLevel) context.player().level(), m);
            }
        });
    }

    public static void onSetPose(NekoPosePayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
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
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
           Entity entity = findNearbyEntityByUuid(context.player(),UUID.fromString(payload.vehicleUuid()),5);
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
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            neko.followOwner(context.player());
        });
    }

    public static void onGiftItem(GiftItemPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            neko.giftItem(context.player(), payload.slot());
        });
    }

    private static void processNekoInteractive(ServerPlayer player, String uuid, EntityFinder finder) {
        // 检查uuid是否合法
        try {
            UUID targetUuid = UUID.fromString(uuid);
            // 寻找目标实体
            NekoEntity nekoEntity = findNearbyNekoByUuid(player, targetUuid,NekoEntity.DEFAULT_FIND_RANGE);
            // 如果实体与玩家太远，则不执行
            if(nekoEntity != null && !(nekoEntity.distanceToSqr(player) > 64)){
                // 处理代码
                finder.find(nekoEntity);
            }
        }catch (Exception ignored){
            return;
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
     *
     * @param player     查找的玩家。
     * @param targetUuid 目标实体的UUID。
     * @return 找到的实体，如果没有找到则返回null。
     */
    private static Entity findNearbyEntityByUuid(ServerPlayer player, UUID targetUuid, double range) {
        ServerLevel world = (ServerLevel) player.level();

        return world.getEntity(targetUuid);

    }

    public static @Nullable NekoEntity findNearbyNekoByUuid(ServerPlayer player, UUID targetUuid,double range) {
        Entity entity = findNearbyEntityByUuid(player,targetUuid,range);
        if (entity instanceof NekoEntity nekoEntity){
            return nekoEntity;
        }else {
            return null;
        }
    }
}

