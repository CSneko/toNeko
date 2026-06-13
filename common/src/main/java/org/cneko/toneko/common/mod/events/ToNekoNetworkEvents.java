package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.cneko.ai.core.AIResponse;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.genetics.api.IGeneticEntity;
import org.cneko.toneko.common.mod.commands.ToNekoCommand;
import org.cneko.toneko.common.mod.items.GeneEditorItem;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.packets.*;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.mod.util.TickTaskQueue;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
        ServerPlayNetworking.registerGlobalReceiver(CrystalNekoNyaPayload.ID, ToNekoNetworkEvents::onCrystalNekoNya);
        ServerPlayNetworking.registerGlobalReceiver(DismountPassengerPayload.ID, ToNekoNetworkEvents::onDismountPassenger);
        ServerPlayNetworking.registerGlobalReceiver(PlayerLeadByPlayerPayload.ID,ToNekoNetworkEvents::onPlayerLeadByPlayer);
        ServerPlayNetworking.registerGlobalReceiver(PluginDetectPayload.ID,(a,b)->{});// 什么也不干
        ServerPlayNetworking.registerGlobalReceiver(ToNekoActionPayload.ID, ToNekoNetworkEvents::onToNekoAction);
        ServerPlayNetworking.registerGlobalReceiver(GenomeDataPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();

            // 防作弊校验 1：必须拥有修改权限 (只有发包带有 canEdit 且手持物品，或者有管理员权限才行)
            boolean holdingEditor = player.getMainHandItem().getItem() instanceof GeneEditorItem
                    || player.getOffhandItem().getItem() instanceof GeneEditorItem;

            if (!holdingEditor && (!player.hasPermissions(2) && !PermissionUtil.has(player, Permissions.GENETICS_EDIT))) {
                player.sendSystemMessage(Component.literal("§c安全拒绝：你没有手持基因编辑器，无法修改基因！"));
                return;
            }

            context.server().execute(() -> {
                // 防作弊校验 2：实体必须存在且在有效范围内
                Entity target = player.level().getEntity(payload.entityId());

                if (target != null && target.distanceToSqr(player) < 64 && target instanceof IGeneticEntity geneticEntity) {
                    // 核心逻辑：应用 NBT 并强制重计算表现型
                    geneticEntity.getGenome().load(payload.genomeNbt());
                    geneticEntity.expressTraits();

                    player.sendSystemMessage(Component.literal("§a基因重组成功！实体的属性已实时更新。"));
                }
            });
        });
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

    public static void onCrystalNekoNya(CrystalNekoNyaPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            if (neko instanceof CrystalNekoEntity cneko) {
                cneko.syncNya();
            }
        });
    }

    public static void onDismountPassenger(DismountPassengerPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        player.getPassengers().forEach(Entity::stopRiding);
        // 显式同步乘客列表到客户端，避免客户端未及时更新的问题
        player.connection.send(new ClientboundSetPassengersPacket(player));
    }

    public static void onChatWithNeko(ChatWithNekoPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            // 如果没有开启 AI，则不执行
            if (!ConfigUtil.isAIEnabled()){
                context.player().sendSystemMessage(Component.translatable("messages.toneko.ai.not_enabled"));
            } else {
                ServerPlayer player = context.player();
                AIUtil.sendMessage(neko.getUUID(), player.getUUID(), neko.generateAIPrompt(context.player()), payload.message(), response -> {
                    if (ConfigUtil.isAIShowThink() && response.hasThink()){
                        ServerLevel world = (ServerLevel) neko.level();
                        // 使用多行 ArmorStand 显示思考过程，顺序逐行显示
                        int totalDelay = spawnFloatingText(neko, response, world);
                        // 在所有行动画完成后，再发送最终消息
                        TickTaskQueue task = new TickTaskQueue();
                        task.addTask(totalDelay, () -> {
                            String r = Messaging.format(response.getResponse(), neko,
                                    Collections.singletonList(LanguageUtil.prefix), ConfigUtil.getChatFormat());
                            player.sendSystemMessage(Component.literal(r));
                        });
                        TickTasks.add(task);
                    } else {
                        String r = Messaging.format(response.getResponse(), neko,
                                Collections.singletonList(LanguageUtil.prefix), ConfigUtil.getChatFormat());
                        context.player().sendSystemMessage(Component.literal(r));
                    }
                    // 如果启用了TTS
                    if (ConfigUtil.isAITTSEnabled()){
                        ServerPlayNetworking.send(player, new TTSSendPayload(response.getResponse()));
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

        List<String> lines = null;
        if (response.getThink() != null) {
            lines = splitText(response.getThink(), 40);
        }
        List<ArmorStand> armorStands = new ArrayList<>();

        int gap = 1; // 行间间隔时间（tick）
        int cumulativeDelay = 0;

        if (lines != null) {
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
        }

        // 调整移除任务：在最后一行移动后100tick移除
        if (lines != null && !lines.isEmpty()) {
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
        int syncDuration = 0;
        if (lines != null) {
            syncDuration = !lines.isEmpty() ? (cumulativeDelay - gap + lines.getLast().length() + 100) : 0;
        }
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
        // 每个 tick 显示一个新字符
        for (int i = 0; i < totalLength; i++) {
            int currentIndex = i;
            queue.addTask(delayOffset + i, () -> {
                // 显示从0到当前索引的子串
                String currentText = fullLine.substring(0, currentIndex + 1);
                stand.setCustomName(Component.literal(currentText).withStyle(ChatFormatting.GOLD));
            });
        }
        TickTasks.add(queue);
    }



    public static void onBreed(NekoMatePayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            Entity mate = findNearbyEntityByUuid(context.player(),UUID.fromString(payload.mateUuid()),10);
            if (mate instanceof INeko m){
                if (neko != m) {
                    if (neko.isNekoBaby() || m.isNekoBaby()) {
                        neko.triggerLoliAlarm(context.player());
                        int i = new java.util.Random().nextInt(25);
                        context.player().sendSystemMessage(Component.translatable("message.toneko.neko.breed_fail_baby." + i));
                    } else {
                        neko.tryMating((ServerLevel) context.player().level(), m);
                    }
                }
            }
        });
    }

    public static void onSetPose(NekoPosePayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> {
            if (!EntityPoseManager.contains(neko)){
                EntityPoseManager.setPose(neko, payload.pose());
            }else {
                EntityPoseManager.remove(neko);
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
        processNekoInteractive(context.player(), payload.uuid(), neko -> neko.followOwner(context.player()));
    }

    public static void onGiftItem(GiftItemPayload payload, ServerPlayNetworking.Context context) {
        processNekoInteractive(context.player(), payload.uuid(), neko -> neko.giftItem(context.player(), payload.slot()));
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
        var quirks = player.getQuirks();
        quirks.clear();
        quirks.addAll(payload.getQuirks().stream().map(QuirkRegister::getById).toList());
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

    // ========== ToNeko Management GUI handlers ==========

    public static void onToNekoAction(ToNekoActionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        context.server().execute(() -> {
            try {
                switch (payload.action()) {
                    case "send_request" -> handleGuiSendRequest(player, payload.targetUuid());
                    case "accept" -> handleGuiAccept(player, payload.targetUuid());
                    case "deny" -> handleGuiDeny(player, payload.targetUuid());
                    case "add_alias" -> handleGuiAddAlias(player, payload.targetUuid(), payload.value1());
                    case "remove_alias" -> handleGuiRemoveAlias(player, payload.targetUuid(), payload.value1());
                    case "add_block" -> handleGuiAddBlock(player, payload.targetUuid(), payload.value1(), payload.value2(), payload.value3());
                    case "remove_block" -> handleGuiRemoveBlock(player, payload.targetUuid(), payload.value1());
                    case "remove_owner" -> handleGuiRemoveOwner(player, payload.targetUuid());
                    case "refresh" -> handleGuiRefresh(player);
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cGUI action failed: " + e.getMessage()));
            }
        });
    }

    private static void handleGuiSendRequest(ServerPlayer player, String targetUuid) {
        ServerPlayer neko = player.getServer().getPlayerList().getPlayer(UUID.fromString(targetUuid));
        if (neko == null) return;
        if (!neko.isNeko()) {
            player.sendSystemMessage(Component.translatable("command.toneko.player.notNeko", neko.getName().getString()));
            return;
        }
        if (neko.hasOwner(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("command.toneko.player.alreadyOwner", neko.getName().getString()));
            return;
        }
        ToNekoCommand.getOwnerMap().put(player, neko);
        player.sendSystemMessage(Component.translatable("command.toneko.player.send_request", neko.getName().getString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        MutableComponent component = Component.translatable("command.toneko.player.request", player.getName().getString()).withStyle(ChatFormatting.GOLD);
        MutableComponent denyButton = Component.translatable("misc.toneko.deny").withStyle(ChatFormatting.RED);
        denyButton.setStyle(denyButton.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/toneko deny " + player.getName().getString())));
        MutableComponent acceptButton = Component.translatable("misc.toneko.accept").withStyle(ChatFormatting.GREEN);
        acceptButton.setStyle(acceptButton.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/toneko accept " + player.getName().getString())));
        component.append(acceptButton);
        component.append(denyButton);
        neko.sendSystemMessage(component);
    }

    private static void handleGuiAccept(ServerPlayer player, String ownerUuid) {
        Map<Player, Player> ownerMap = ToNekoCommand.getOwnerMap();
        Player owner = player.getServer().getPlayerList().getPlayer(UUID.fromString(ownerUuid));
        if (owner == null) return;
        if (ownerMap.containsKey(owner) && ownerMap.get(owner).equals(player)) {
            player.addOwner(owner.getUUID(), new INeko.Owner(new java.util.ArrayList<>(), 0));
            player.sendSystemMessage(Component.translatable("command.toneko.accept", owner.getName()).withStyle(ChatFormatting.GREEN));
            owner.sendSystemMessage(Component.translatable("command.toneko.player.accept", player.getName()).withStyle(ChatFormatting.GREEN));
            ownerMap.remove(owner);
        } else {
            player.sendSystemMessage(Component.translatable("command.toneko.not_request"));
        }
    }

    private static void handleGuiDeny(ServerPlayer player, String ownerUuid) {
        Map<Player, Player> ownerMap = ToNekoCommand.getOwnerMap();
        Player owner = player.getServer().getPlayerList().getPlayer(UUID.fromString(ownerUuid));
        if (owner == null) return;
        if (ownerMap.containsKey(owner) && ownerMap.get(owner).equals(player)) {
            player.sendSystemMessage(Component.translatable("command.toneko.deny", owner.getName()).withStyle(ChatFormatting.RED));
            owner.sendSystemMessage(Component.translatable("command.toneko.player.deny", player.getName()).withStyle(ChatFormatting.RED));
            ownerMap.remove(owner);
        } else {
            player.sendSystemMessage(Component.translatable("command.toneko.not_request"));
        }
    }

    private static void handleGuiAddAlias(ServerPlayer player, String nekoUuid, String alias) {
        ServerPlayer neko = player.getServer().getPlayerList().getPlayer(UUID.fromString(nekoUuid));
        if (neko == null) return;
        if (!neko.hasOwner(player.getUUID())) return;
        neko.getOwner(player.getUUID()).getAliases().add(alias);
        player.sendSystemMessage(Component.translatable("command.toneko.aliases.add", alias));
    }

    private static void handleGuiRemoveAlias(ServerPlayer player, String nekoUuid, String alias) {
        ServerPlayer neko = player.getServer().getPlayerList().getPlayer(UUID.fromString(nekoUuid));
        if (neko == null) return;
        if (!neko.hasOwner(player.getUUID())) return;
        neko.getOwner(player.getUUID()).getAliases().remove(alias);
        player.sendSystemMessage(Component.translatable("command.toneko.aliases.remove", alias));
    }

    private static void handleGuiAddBlock(ServerPlayer player, String nekoUuid, String block, String replace, String method) {
        ServerPlayer neko = player.getServer().getPlayerList().getPlayer(UUID.fromString(nekoUuid));
        if (neko == null) return;
        if (!neko.hasOwner(player.getUUID())) return;
        INeko.BlockedWord.BlockMethod bm = INeko.BlockedWord.BlockMethod.fromString(method);
        if (bm == null) bm = INeko.BlockedWord.BlockMethod.WORD;
        neko.addBlockedWord(new INeko.BlockedWord(block, replace, bm));
        player.sendSystemMessage(Component.translatable("messages.toneko.block.add"));
    }

    private static void handleGuiRemoveBlock(ServerPlayer player, String nekoUuid, String block) {
        ServerPlayer neko = player.getServer().getPlayerList().getPlayer(UUID.fromString(nekoUuid));
        if (neko == null) return;
        if (!neko.hasOwner(player.getUUID())) return;
        neko.removeBlockedWord(block);
        player.sendSystemMessage(Component.translatable("messages.toneko.block.remove"));
    }

    private static void handleGuiRemoveOwner(ServerPlayer player, String nekoUuid) {
        ServerPlayer neko = player.getServer().getPlayerList().getPlayer(UUID.fromString(nekoUuid));
        if (neko == null) return;
        neko.removeOwner(player.getUUID());
        player.sendSystemMessage(Component.translatable("command.toneko.remove", neko.getName().getString()));
    }

    private static void handleGuiRefresh(ServerPlayer player) {
        CompoundTag data = ToNekoCommand.buildManagementData(player);
        ServerPlayNetworking.send(player, new ToNekoManagementDataPayload(data));
    }
}

