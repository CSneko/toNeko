package com.crystalneko.tonekofabric.command;

import com.crystalneko.tonekofabric.libs.base;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class command{
    private final String worldName;
    public command(){
        //获取世界名称
        worldName = "world";
        //注册命令
        initCommand();
    }

     //--------------------------------------------------------注册命令---------------------------------------------------

    public void initCommand() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("toneko")
                    //----------------------------------------player-------------------------------------
                    .then(literal("player")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)  //获取玩家列表
                                    .executes(context -> {
                                        return ToNekoCommand.Player(context);
                                    })
                            )
                    )
                    //--------------------------------------------------aliases--------------------------------------
                    .then(literal("aliases")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)  //获取玩家列表
                                    //-------------------------------------add---------------------------------------
                                    .then(literal("add")
                                            .then(argument("aliases", StringArgumentType.string())
                                                    .executes(context -> {
                                                        return ToNekoCommand.AliasesAdd(context);
                                                    })
                                            )
                                    ).then(literal("remove")
                                            .then(argument("aliases", StringArgumentType.string())
                                                    .executes(context -> {
                                                        return ToNekoCommand.AliasesRemove(context);
                                                    })
                                            )
                                     )
                            )
                    )
                    //--------------------------------------------------item-----------------------------------------
                    .then(literal("item")
                            .executes(context -> {
                                return ToNekoCommand.item(context);
                            })
                    )

                    //----------------------------------------无参数-----------------------------------------
                    .executes(context -> {
                        context.getSource().sendMessage(base.getStringLanguage("message.toneko.help", new String[]{""}));
                        return 1;
                    })
            );
        });
    }

    //-------------------------------------------------------获取在线玩家----------------------------------------------
    private static final SuggestionProvider<ServerCommandSource> getOnlinePlayers = (context, builder) -> {
        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            String playerTabList = player.getName().toString();
            //替换字符
            String output = playerTabList.replace("literal{", "").replace("}", "");
            builder.suggest(output);
        }
        return builder.buildFuture();
    };



}

