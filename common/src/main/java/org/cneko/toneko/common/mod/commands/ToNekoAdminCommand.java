package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import java.io.File;
import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;
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
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_HELP))
                            .executes(ToNekoAdminCommand::help)
                    )
            );
        });
    }

    public static int dataDeleteRubbish(CommandContext<CommandSourceStack> context) {
        // 在异步进行
        NekoQuery.NekoData.executor.submit(() -> {
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
