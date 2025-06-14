package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.util.CommandUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;
import org.cneko.toneko.common.util.QuirkUtil;

import java.util.Collection;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
public class QuirkCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("quirk")
                .requires(source -> PermissionUtil.has(source,Permissions.COMMAND_QUIRK) && source.isPlayer())
                .then(literal("help")
                        .requires(source -> PermissionUtil.has(source,Permissions.COMMAND_QUIRK_HELP))
                        .executes(QuirkCommand::helpCommand)
                )
                .then(literal("add")
                        .requires(source -> PermissionUtil.has(source,Permissions.COMMAND_QUIRK_ADD))
                        .then(argument("quirk", StringArgumentType.string())
                                .suggests(CommandUtil::getQuirksSuggestions)
                                .executes(QuirkCommand::addOrRemoveQuirk)
                        )
                )
                .then(literal("remove")
                        .requires(source -> PermissionUtil.has(source,Permissions.COMMAND_QUIRK_REMOVE))
                        .then(argument("quirk", StringArgumentType.string())
                                .suggests(CommandUtil::getQuirksSuggestions)
                                .executes(QuirkCommand::addOrRemoveQuirk)
                        )
                )
                .then(literal("list")
                        .requires(source -> PermissionUtil.has(source,Permissions.COMMAND_QUIRK_LIST))
                        .executes(QuirkCommand::listQuirks)
                )
                .then(literal("gui")
                        .requires(source -> PermissionUtil.has(source,Permissions.COMMAND_QUIRK_GUI))
                        .executes(QuirkCommand::quirkGui)
                )
                .executes(QuirkCommand::quirkGui)
        ));
    }

    public static int quirkGui(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        // 打开设置屏幕
        ServerPlayNetworking.send(player, new QuirkQueryPayload(
                QuirkUtil.quirkToIds(player.getQuirks()),
                QuirkRegister.getQuirkIds().stream().toList(),true)
        );
        return 1;
    }

    public static int listQuirks(CommandContext<CommandSourceStack> context) {
       Player player = context.getSource().getPlayer();
        if(player.getQuirks().isEmpty()){
            context.getSource().sendSystemMessage(translatable("command.quirk.no_any_quirk"));
            return 1;
        }
        // 列出quirks
        Collection<Quirk> quirks = player.getQuirks();
        // 转换为id
        List<String> quirkIds = quirks.stream().map(Quirk::getId).toList();
        // 翻译
        List<Component> quirkTexts = quirkIds.stream().map(id -> translatable("quirk.toneko." + id)).toList();
        // 全部发送
        context.getSource().sendSystemMessage(translatable("command.quirk.list"));
        for (Component text : quirkTexts) {
            context.getSource().sendSystemMessage(text);
        }
        return 1;
    }

    public static int addOrRemoveQuirk(CommandContext<CommandSourceStack> context) {
        Player neko = context.getSource().getPlayer();
        String quirk = StringArgumentType.getString(context, "quirk");
        if(!QuirkRegister.hasQuirk(quirk)){
            context.getSource().sendSystemMessage(translatable("command.quirk.not_quirk"));
            return 1;
        }
        // 如果是添加
        context.getNodes().stream().filter(node -> node.getNode().getName().equals("add")).findFirst().ifPresent(node -> {
            if(neko.hasQuirk(QuirkRegister.getById(quirk))){
                context.getSource().sendSystemMessage(translatable("command.quirk.already_quirk"));
            }else {
                neko.addQuirk(QuirkRegister.getById(quirk));
                context.getSource().sendSystemMessage(translatable("command.quirk.add", quirk));
            }
        });
        // 删除
        context.getNodes().stream().filter(node -> node.getNode().getName().equals("remove")).findFirst().ifPresent(node -> {
            if(!neko.hasQuirk(QuirkRegister.getById(quirk))){
                context.getSource().sendSystemMessage(translatable("command.quirk.not_has_quirk"));
            }else {
                neko.removeQuirk(QuirkRegister.getById(quirk));
                context.getSource().sendSystemMessage(translatable("command.quirk.remove", quirk));
            }
        });
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.quirk.help"));
        return 1;
    }
}
