package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class NekoCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("neko")
                    .requires(CommandSourceStack::isPlayer)
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_HELP))
                            .executes(NekoCommand::helpCommand)
                    )
                    .then(literal("jump")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_JUMP))
                            .executes(NekoCommand::jumpCommand)
                    )
                    .then(literal("vision")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_VISION))
                            .executes(NekoCommand::visionCommand)
                    )
                    .then(literal("speed")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_SPEED))
                            .executes(NekoCommand::speedCommand)
                    )
                    .then(literal("lie")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_LIE))
                            .executes(NekoCommand::lieCommand)
                    )
                    .then(literal("getDown")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_GET_DOWN))
                            .executes(NekoCommand::getDownCommand)
                    )
                    .then(literal("nickname")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_NICKNAME))
                            .then(argument("nickname", StringArgumentType.greedyString())
                                    .executes(NekoCommand::nicknameCommand)
                            )
                    )
                    .then(literal("removeNickname")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_NICKNAME))
                            .executes(NekoCommand::removeNicknameCommand)
                    )
                    .then(literal("level")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_LEVEL))
                            .executes(NekoCommand::levelCommand)
                    )
                    .then(literal("lore")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_LORE))
                            .then(argument("lore", StringArgumentType.greedyString())
                                    .executes(NekoCommand::loreCommand)
                            )
                    )
                    .then(literal("ride")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_RIDE))
                            .executes(NekoCommand::rideCommand)
                    )
                    .then(literal("rideHead")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_NEKO_RIDE_HEAD))
                            .executes(NekoCommand::rideHeadCommand)
                    )
            );
        });
    }

    private static int removeNicknameCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer neko = context.getSource().getPlayer();
        neko.setNickName("");
        return 1;
    }

    private static void handleRiding(Entity entity, LivingEntity target, boolean isRideHead) {
        if (target != null && target != entity) {
            if (isRideHead) {
                target.startRiding(entity, true);
            } else {
                entity.startRiding(target, true);
            }
        }

        Entity senderEntity = isRideHead ? entity : target;
        if (senderEntity instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetPassengersPacket(senderEntity));
        }
    }

    public static int rideCommand(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        ServerLevel world = (ServerLevel) entity.level();
        float radius = 3.0f;
        LivingEntity target = EntityUtil.findNearestEntityInRange(entity, world, radius);

        handleRiding(entity, target, false);
        return 1;
    }

    private static int rideHeadCommand(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        ServerLevel world = (ServerLevel) entity.level();
        float radius = 3.0f;
        LivingEntity target = EntityUtil.findNearestEntityInRange(entity, world, radius);

        handleRiding(entity, target, true);
        return 1;
    }


    // 原始方法 getDownCommand
    public static int getDownCommand(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        setEntityPose(entity, Pose.SWIMMING, context.getSource());
        return 1;
    }

    // 原始方法 lieCommand
    public static int lieCommand(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        setEntityPose(entity, Pose.SLEEPING, context.getSource());
        return 1;
    }

    // 设置实体的姿态
    private static void setEntityPose(Entity entity, Pose pose, CommandSourceStack source) {
        if (EntityPoseManager.contains(entity)) {
            EntityPoseManager.remove(entity);
            sendPosePacket(entity, pose, false);
        } else {
            EntityPoseManager.setPose(entity, pose);
            sendPosePacket(entity, pose, true);
        }
    }

    // 发送姿态包
    private static void sendPosePacket(Entity entity, Pose pose, boolean isSet) {
        if (entity instanceof ServerPlayer player) {
            ServerPlayNetworking.send(player, new EntityPosePayload(pose,entity.getUUID().toString(), isSet));
        }
    }


    public static int loreCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (!player.isNeko()){
            context.getSource().getPlayer().sendSystemMessage(translatable("command.neko.not_neko"));
        }else {
            // 获取玩家手中的物品
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                context.getSource().getPlayer().sendSystemMessage(translatable("command.neko.lore.no_item"));
                return 1;
            }
            String lore = StringArgumentType.getString(context, "lore");
            Component text = Component.nullToEmpty(lore);
            List<Component> loreList = new ArrayList<>();
            loreList.add(text);
            // 给物品添加lore
            stack.set(DataComponents.LORE,new ItemLore(loreList));
        }
        return 1;
    }

    public static int levelCommand(CommandContext<CommandSourceStack> context) {
        INeko neko = context.getSource().getPlayer();
        if(neko.isNeko()){
            double level = neko.getNekoLevel();
            // 保留小数点后两位
            level = Math.round(level * 100) / 100.0;
            context.getSource().getPlayer().sendSystemMessage(translatable("command.neko.level.success", level));
        }else{
            context.getSource().getPlayer().sendSystemMessage(translatable("command.neko.not_neko"));
        }
        return 1;
    }

    public static int nicknameCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer neko = context.getSource().getPlayer();
        String nickname = StringArgumentType.getString(context, "nickname");
        // 设置昵称
        neko.setNickName(nickname);
        neko.sendSystemMessage(translatable("command.neko.nickname.success", nickname));
        return 1;
    }


    public static int speedCommand(CommandContext<CommandSourceStack> context) {
        return giveEffect(context, MobEffects.MOVEMENT_SPEED);
    }

    public static int visionCommand(CommandContext<CommandSourceStack> context) {
        return giveEffect(context, MobEffects.NIGHT_VISION);
    }

    public static int jumpCommand(CommandContext<CommandSourceStack> context) {
        return giveEffect(context, MobEffects.JUMP);
    }

    public static int giveEffect(CommandContext<CommandSourceStack> context, Holder<MobEffect> effect) {
        ServerPlayer player = context.getSource().getPlayer();
        if(!player.isNeko()){
            player.sendSystemMessage(translatable("command.neko.not_neko"));
            return 1;
        }else if (player.getNekoEnergy()<100){
            player.sendSystemMessage(translatable("command.neko.effect.not_enough_energy"));
        }
        // 消耗能量
        player.setNekoEnergy(player.getNekoEnergy()-100);
        // 猫猫等级
        double nekoDegree = player.getAttributeValue(ToNekoAttributes.NEKO_DEGREE);
        // 获取玩家等级来计算效果
        double level = player.getNekoLevel();
        // (等级+猫猫等级)开方/2
        int effectLevel = (int) (Math.sqrt(level+nekoDegree)/2.00);

        // 对((((等级+1)的开方)乘以(玩家的经验值的开方))/(玩家生命值/4))*20*(猫猫等级+1)/2来计算效果时间
        int time = (int)(((((Math.sqrt(level+1)) * (Math.sqrt(player.totalExperience+1))) / (player.getHealth()/4)))*100 * (nekoDegree+1)/2);

        // 最大等级为10,时间为一小时
        effectLevel = Math.min(effectLevel, 10);
        time = Math.min(time, 20*3600);
        player.addEffect(new MobEffectInstance(effect, time, effectLevel));
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.neko.help"));
        return 1;
    }
}
