package org.cneko.toneko.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.api.quirk.QuirkRegister;
import org.cneko.toneko.fabric.util.PermissionUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.cneko.toneko.fabric.util.TextUtil.translatable;
public class QuirkCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("quirk")
                    .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK, source))
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_HELP, source))
                            .executes(QuirkCommand::helpCommand)
                    )
                    .then(literal("add")
                            .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_ADD, source))
                            .then(argument("quirk", StringArgumentType.string())
                                    .executes(QuirkCommand::addOrRemoveQuirk)
                            )
                    )
                    .then(literal("remove")
                            .requires(source -> PermissionUtil.has(Permissions.COMMAND_QUIRK_ADD, source))
                            .then(argument("quirk", StringArgumentType.string())
                                    .executes(QuirkCommand::addOrRemoveQuirk)
                            )
                    )
            );
        });
    }

    public static int addOrRemoveQuirk(CommandContext<ServerCommandSource> context) {
        NekoQuery.Neko neko = NekoQuery.getNeko(context.getSource().getPlayer().getUuid());
        String quirk = StringArgumentType.getString(context, "quirk");
        if(!QuirkRegister.hasQuirk(quirk)){
            context.getSource().sendMessage(translatable("command.quirk.not_quirk"));
            return 1;
        }
        // 如果是添加
        context.getNodes().stream().filter(node -> node.getNode().getName().equals("add")).findFirst().ifPresent(node -> {
            if(neko.hasQuirk(QuirkRegister.getById(quirk))){
                context.getSource().sendMessage(translatable("command.quirk.already_quirk"));
            }else {
                neko.addQuirk(QuirkRegister.getById(quirk));
                context.getSource().sendMessage(translatable("command.quirk.add", quirk));
            }
        });
        // 删除
        context.getNodes().stream().filter(node -> node.getNode().getName().equals("remove")).findFirst().ifPresent(node -> {
            if(!neko.hasQuirk(QuirkRegister.getById(quirk))){
                context.getSource().sendMessage(translatable("command.quirk.not_has_quirk"));
            }else {
                neko.removeQuirk(QuirkRegister.getById(quirk));
                context.getSource().sendMessage(translatable("command.quirk.remove", quirk));
            }
        });
        neko.save();
        return 1;
    }

    public static int helpCommand(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(translatable("command.quirk.help"));
        return 1;
    }
}
