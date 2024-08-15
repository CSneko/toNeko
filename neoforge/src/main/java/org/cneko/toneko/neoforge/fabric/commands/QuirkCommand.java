package org.cneko.toneko.neoforge.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.util.CommandUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.quirks.Quirk;
import org.cneko.toneko.common.quirks.QuirkRegister;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
public class QuirkCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("quirk")
                .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK, source))
                .then(literal("help")
                        .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_HELP, source))
                        .executes(QuirkCommand::helpCommand)
                )
                .then(literal("add")
                        .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_ADD, source))
                        .then(argument("quirk", StringArgumentType.string())
                                .suggests(CommandUtil::getQuirksSuggestions)
                                .executes(QuirkCommand::addOrRemoveQuirk)
                        )
                )
                .then(literal("remove")
                        .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_REMOVE, source))
                        .then(argument("quirk", StringArgumentType.string())
                                .suggests(CommandUtil::getQuirksSuggestions)
                                .executes(QuirkCommand::addOrRemoveQuirk)
                        )
                )
                .then(literal("list")
                        .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_LIST, source))
                        .executes(QuirkCommand::listQuirks)
                )
        ));
    }

    public static int listQuirks(CommandContext<CommandSourceStack> context) {
        NekoQuery.Neko neko = NekoQuery.getNeko(context.getSource().getPlayer().getUUID());
        if(neko.getQuirks().isEmpty()){
            context.getSource().sendSystemMessage(translatable("command.quirk.no_any_quirk"));
            return 1;
        }
        // 列出quirks
        List<Quirk> quirks = neko.getQuirks();
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
        NekoQuery.Neko neko = NekoQuery.getNeko(context.getSource().getPlayer().getUUID());
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
