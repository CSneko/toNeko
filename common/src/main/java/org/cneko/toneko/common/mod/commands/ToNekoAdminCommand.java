package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class ToNekoAdminCommand {
    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEYS = (context, builder) -> {
        ConfigUtil.CONFIG_BUILDER.getKeys().forEach(builder::suggest);
        return builder.buildFuture();
    };
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("tonekoadmin")
                    .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN))
                    .then(literal("set")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_SET))
                            .then(argument("neko", EntityArgument.player())
                                    .then(argument("is",BoolArgumentType.bool())
                                            .executes(ToNekoAdminCommand::set)
                                    )
                            )

                    )
                    .then(literal("reload")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_RELOAD))
                            .executes(ToNekoAdminCommand::reload)
                            .then(literal("config")
                                    .executes(ToNekoAdminCommand::reloadConfig)
                            )
                   )
                    .then(literal("config")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_CONFIG))
                            .then(literal("get")
                                    .then(argument("key", StringArgumentType.string())
                                            .suggests(CONFIG_KEYS)
                                            .executes(ToNekoAdminCommand::getConfig)
                                    )
                            )
                            .then(literal("set")
                                    .then(argument("key", StringArgumentType.string())
                                            .suggests(CONFIG_KEYS)
                                            .then(argument("value", StringArgumentType.string())
                                                    .executes(ToNekoAdminCommand::setConfig)
                                            )
                                    )
                            )
                            .then(literal("reload")
                                    .executes(ToNekoAdminCommand::reloadConfig)
                            )
                    )
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_HELP))
                            .executes(ToNekoAdminCommand::help)
                    )
            );
        });
    }

    private static int setConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        try {
            // 检查类型
            final var builder = ConfigUtil.CONFIG_BUILDER;
            final var config = ConfigUtil.CONFIG;
            if(!config.contains(key)){
                context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.not_found", key));
                return 1;
            }
            final var type = builder.get(key).type();
            if (type == ConfigBuilder.Entry.Types.BOOLEAN &&(value.equals("true") || value.equals("false") || value.equals("1") || value.equals("0"))){
                config.set(key, Boolean.parseBoolean(value));
            } else if (type == ConfigBuilder.Entry.Types.NUMBER && value.matches("[0-9]+")) {
                config.set(key, Integer.parseInt(value));
            } else if (type == ConfigBuilder.Entry.Types.STRING) {
                config.set(key, value);
            }else {
                context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.type_error"));
                return 1;
            }
            config.save();
            context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.set", key, value));
        }catch (Exception ignored){}
        return 1;
    }

    private static int getConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        try {
            if (!ConfigUtil.CONFIG.contains(key)){
                context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.not_found", key));
            }
            Object value = ConfigUtil.CONFIG.get(key);
            context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.get", key, value));
        }catch (Exception ignored){}
        return 1;
    }



    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        // 重新加载配置文件和语言文件
        ConfigUtil.load();
        LanguageUtil.load();
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.reload"));
        return 1;
    }
    public static int reload(CommandContext<CommandSourceStack> context) {
        reloadConfig(context);
        return 1;
    }

    public static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.help"));
        return 1;
    }


    public static int set(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer neko;
        try {
            neko = EntityArgument.getPlayer(context, "neko");
        } catch (CommandSyntaxException e) {
            return 0;
        }
        boolean isNeko = context.getArgument("is", Boolean.class);
        if(isNeko){
            neko.setNeko(true);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.true", neko.getName().getString()));
        }else {
            neko.setNeko(false);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.false", neko.getName().getString()));
        }
        return 1;
    }
}
