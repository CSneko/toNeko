package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.gal.common.util.pack.ExternalPack;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import java.nio.file.Path;
import java.util.*;

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
                            .then(literal("data")
                                    .executes(ToNekoAdminCommand::reloadData)
                            )
                            .then(literal("config")
                                    .executes(ToNekoAdminCommand::reloadConfig)
                            )
                   )
                   .then(literal("data")
                           .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_DATA))
                           .then(literal("loadedCount")
                                   .executes(ToNekoAdminCommand::dataLoadedCount)
                           )
                           .then(literal("allCount")
                                   .executes(ToNekoAdminCommand::dataAllCount)
                           )
                           .then(literal("deleteRubbish")
                                   .executes(ToNekoAdminCommand::dataDeleteRubbish)
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

            dispatcher.register(literal("testgal")
                    .then(argument("loc", StringArgumentType.string())
                            .then(argument("file", StringArgumentType.string())
                                    .executes(context->{
                                        ExternalPack.addResource(ResourceLocation.parse(StringArgumentType.getString(context, "loc")), Path.of(StringArgumentType.getString(context, "file")));
                                        return 1;
                                    })
                            )
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

    public static int dataDeleteRubbish(CommandContext<CommandSourceStack> context) {
        // 记录时间
        long startTime = System.currentTimeMillis();
        // 扫描服务器内所有世界的实体数据（包括未生成的）并生成列表
        Set<UUID> uuids = new HashSet<>();
        context.getSource().getServer().getAllLevels().forEach(level -> {
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof INeko neko) {
                    uuids.add(neko.getEntity().getUUID());
                }
            });
        });
        NekoQuery.NekoData.executor.submit(()->{
            uuids.addAll(PlayerUtil.getPlayerUUIDs(context.getSource().getServer()));
            // 删除符合条件的
            int deletedCount = NekoQuery.NekoData.deleteIf(neko -> !uuids.contains(neko.uuid));
            // 计算时间（秒）
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            context.getSource().sendSystemMessage(translatable("command.tonekoadmin.data.delete_rubbish", deletedCount, elapsedTime));
        });
        return 1;
    }


    public static int dataAllCount(CommandContext<CommandSourceStack> context) {
        NekoQuery.NekoData.asyncGetAllNekoCount(count -> context.getSource().sendSystemMessage(translatable("command.tonekoadmin.data.all_count", count)));
        return 1;
    }

    public static int dataLoadedCount(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.data.loaded_count", NekoQuery.NekoData.getNekoCount()));
        return 1;
    }


    public static int reloadData(CommandContext<CommandSourceStack> context) {
        long startTime = System.currentTimeMillis(); // 记录开始时间

        NekoQuery.NekoData.saveAllAsync(() -> {
            // NekoQuery.NekoData.loadAll(); // 这...这，大可不必
            NekoQuery.NekoData.removeAll();
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000; // 计算耗时（秒）
            context.getSource().sendSystemMessage(translatable("command.tonekoadmin.reload.data", elapsedTime)); // 将耗时传递给方法
        });
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
        reloadData(context);
        return 1;
    }

    public static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.help"));
        return 1;
    }


    public static int set(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer nekoP;
        try {
            nekoP = EntityArgument.getPlayer(context, "neko");
        } catch (CommandSyntaxException e) {
            return 0;
        }
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoP.getUUID());
        if (neko==null){
            return 0;
        }
        boolean isNeko = context.getArgument("is", Boolean.class);
        if(isNeko){
            neko.setNeko(true);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.true", nekoP.getName().getString()));
        }else {
            neko.setNeko(false);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.false", nekoP.getName().getString()));
        }
        return 1;
    }
}
