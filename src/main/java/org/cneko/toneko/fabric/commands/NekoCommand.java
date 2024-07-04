package org.cneko.toneko.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.fabric.events.PlayerTickEvent;
import org.cneko.toneko.fabric.util.PermissionUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.cneko.toneko.fabric.util.CommandUtil.getOnlinePlayers;
import static org.cneko.toneko.fabric.util.TextUtil.translatable;

public class NekoCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("neko")
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source.getPlayer(), Permissions.COMMAND_NEKO_HELP))
                            .executes(NekoCommand::helpCommand)
                    )
                    .then(literal("jump")
                            .requires(source -> PermissionUtil.has(source.getPlayer(), Permissions.COMMAND_NEKO_JUMP))
                            .executes(NekoCommand::jumpCommand)
                    )
                    .then(literal("vision")
                            .requires(source -> PermissionUtil.has(source.getPlayer(), Permissions.COMMAND_NEKO_VISION))
                            .executes(NekoCommand::visionCommand)
                    )
                    .then(literal("speed")
                            .requires(source -> PermissionUtil.has(source.getPlayer(), Permissions.COMMAND_NEKO_SPEED))
                            .executes(NekoCommand::speedCommand)
                    )
                    .then(literal("lie")
                            .requires(source -> PermissionUtil.has(source.getPlayer(), Permissions.COMMAND_NEKO_LIE))
                            .executes(NekoCommand::lieCommand)
                    )
            );
        });
    }

    public static int lieCommand(CommandContext<ServerCommandSource> context) {
        PlayerEntity player = context.getSource().getPlayer();
        // 如果玩家没有躺下,把玩家设置为躺下,否则把玩家设置为正常
        if(PlayerTickEvent.lyingPlayers.contains(player)){
            PlayerTickEvent.lyingPlayers.remove(player);
        }else{
            PlayerTickEvent.lyingPlayers.add(player);
        }
        return 1;
    }

    public static int speedCommand(CommandContext<ServerCommandSource> context) {
        return giveEffect(context, StatusEffects.SPEED);
    }

    public static int visionCommand(CommandContext<ServerCommandSource> context) {
        return giveEffect(context, StatusEffects.NIGHT_VISION);
    }

    public static int jumpCommand(CommandContext<ServerCommandSource> context) {
        return giveEffect(context, StatusEffects.JUMP_BOOST);
    }

    public static int giveEffect(CommandContext<ServerCommandSource> context, StatusEffect effect) {
        PlayerEntity player = context.getSource().getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUuid());
        if(!neko.isNeko()){
            player.sendMessage(translatable("command.neko.not_neko"));
            return 1;
        }
        // 获取玩家等级来计算效果
        double level = neko.getLevel();
        // 等级开方/2
        int effectLevel = (int) (Math.sqrt(level)/2.00);
        // 对((((等级+1)的开方)乘以(玩家的经验值的开方))/(玩家生命值/4))*20来计算效果时间
        int time = ((int) (((Math.sqrt(level+1)) * (Math.sqrt(player.totalExperience+1))) / (player.getHealth()/4)))*100;
        player.addStatusEffect(new StatusEffectInstance(effect, time, effectLevel));
        return 1;
    }

    public static int helpCommand(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(translatable("command.neko.help"));
        return 1;
    }
}
