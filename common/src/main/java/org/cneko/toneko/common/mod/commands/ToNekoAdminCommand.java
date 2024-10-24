package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.NekoSkin;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.common.mod.util.CommandUtil.getOnlinePlayers;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

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
                            .then(literal("data")
                                    .executes(ToNekoAdminCommand::reloadData)
                            )
                   )
//                    .then(literal("skin")
//                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_SKIN))
//                            .then(literal("list")
//                                    .executes(ToNekoAdminCommand::skinList)
//                            )
//                            .then(literal("add")
//                                    .then(argument("skin", StringArgumentType.string())
//                                            .executes(ToNekoAdminCommand::skinAdd)
//                                    )
//                            )
//                            .then(literal("remove")
//                                    .then(argument("skin", StringArgumentType.string())
//                                            .executes(ToNekoAdminCommand::skinRemove)
//                                    )
//                            )
//                    )
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_HELP))
                            .executes(ToNekoAdminCommand::help)
                    )
            );
        });
    }

    public static int skinAdd(CommandContext<CommandSourceStack> context) {
        String skin = StringArgumentType.getString(context, "skin");
        NekoSkin.addSkin(skin);
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.skin.add", skin));
        return 1;
    }

    public static int skinRemove(CommandContext<CommandSourceStack> context) {
        String skin = StringArgumentType.getString(context, "skin");
        NekoSkin.removeSkin(skin);
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.skin.remove", skin));
        return 1;
    }

    public static int skinList(CommandContext<CommandSourceStack> context) {
        String skins = NekoSkin.getSkins().stream().reduce((a, b) -> a + "\n" + b).toString();
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.skin.list", skins));
        return 1;
    }

    public static int reloadData(CommandContext<CommandSourceStack> context) {
        // 重新加载猫娘数据
        NekoQuery.NekoData.saveAll();
        NekoQuery.NekoData.loadAll();
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.reload.data"));
        return 1;
    }

    public static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.help"));
        return 1;
    }

    public static int reload(CommandContext<CommandSourceStack> context) {
        // 重新加载配置文件和语言文件
        ConfigUtil.load();
        LanguageUtil.load();
        reloadData(context);
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
