package org.cneko.toneko.neoforge.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.neoforge.fabric.util.PermissionUtil;
import org.cneko.toneko.neoforge.fabric.util.PlayerUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.neoforge.fabric.util.CommandUtil.getOnlinePlayers;
import static org.cneko.toneko.neoforge.fabric.util.TextUtil.translatable;

public class ToNekoAdminCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("tonekoadmin")
                    .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN))
                    .then(literal("set")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_SET))
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    .executes(ToNekoAdminCommand::set)
                            )

                    )
                    .then(literal("reload")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_RELOAD))
                            .executes(ToNekoAdminCommand::reload)
                    )
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_HELP))
                            .executes(ToNekoAdminCommand::help)
                    )
            );
        });
    }

    public static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.help"));
        return 1;
    }

    public static int reload(CommandContext<CommandSourceStack> context) {
        // 重新加载配置文件和语言文件
        ConfigUtil.load();
        LanguageUtil.load();
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.reload"));
        return 1;
    }

    public static int set(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String nekoName = StringArgumentType.getString(context, "neko");
        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(nekoName).getUUID());
        boolean isNeko = neko.isNeko();
        if(isNeko){
            // 如果是猫猫，则设置为非猫猫
            neko.setNeko(false);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.false", nekoName));
        }else {
            neko.setNeko(true);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.true", nekoName));
        }
        return 1;
    }
}
