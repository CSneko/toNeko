package com.crystalneko.tonekofabric.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.crystalneko.tonekofabric.api.Messages.translatable;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class command{
    public command(){
        //获取世界名称
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
                                    .executes(ToNekoCommand::Player)
                            )
                    )
                    //--------------------------------------------------aliases--------------------------------------
                    .then(literal("aliases")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)  //获取玩家列表
                                    //-------------------------------------add---------------------------------------
                                    .then(literal("add")
                                            .then(argument("aliases", StringArgumentType.string())
                                                    .executes(ToNekoCommand::AliasesAdd)
                                            )
                                    ).then(literal("remove")
                                            .then(argument("aliases", StringArgumentType.string())
                                                    .executes(ToNekoCommand::AliasesRemove)
                                            )
                                     )
                            )
                    )
                    //--------------------------------------------------item-----------------------------------------
                    .then(literal("item")
                            .executes(ToNekoCommand::item)
                    )
                    //-----------------------------------------------block--------------------------------------------
                    .then(literal("block")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    //--------------------------------add------------------------------------
                                    .then(literal("add")
                                            .then(argument("block",StringArgumentType.string())
                                                    .then(argument("replace",StringArgumentType.string())
                                                            .then(argument("method",StringArgumentType.string())
                                                                    .suggests((context, builder) -> {
                                                                        builder.suggest("all");
                                                                        builder.suggest("word");
                                                                        return builder.buildFuture();
                                                                    })
                                                                    .executes(ToNekoCommand::addBlock)
                                                            )
                                                    )
                                            )
                                    )
                                    //----------------------------remove----------------------------------
                                    .then(literal("remove")
                                            .then(argument("block",StringArgumentType.string())
                                                    .executes(ToNekoCommand::removeBlock)
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------xp------------------------------------------------
                    .then(literal("xp")
                            .then(argument("neko",StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    .executes(ToNekoCommand::xp)
                            )
                    )
                    //-------------------------------------------remove--------------------------------------
                    .then(literal("remove")
                            .then(argument("neko",StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    .executes(ToNekoCommand::remove)
                            )
                    )
                    .then(literal("everyone")
                            .executes(ToNekoCommand::everyone)
                    )
                    //-------------------------------------help---------------------------------------------------
                    .then(literal("help")
                                    .executes(context -> {
                                        context.getSource().sendMessage(translatable("command.toneko.help"));
                                        return 1;
                                    })
                    )

                    //----------------------------------------无参数-----------------------------------------
                    .executes(context -> {
                        context.getSource().sendMessage(translatable("command.toneko.help"));
                        return 1;
                    })
            );
            //-------------------------------------------------neko-------------------------------------------------
             dispatcher.register(literal("neko")
                     //------------------------------------------help---------------------------------------
                     .then(literal("help")
                             .executes(context -> {
                                 context.getSource().sendMessage(translatable("command.neko.help"));
                                 return 1;
                             })
                     )
                     //----------------------------------------jump-------------------------------------
                     .then(literal("jump")
                             .executes(NekoCommand::jump)
                     )
                     //----------------------------------------vision---------------------------------
                     .then(literal("vision")
                             .executes(NekoCommand::vision)
                     )
             );
             //---------------------------------------------aineko-------------------------------------------------------
            dispatcher.register(literal("aineko")
                    //--------------------------------------help---------------------------------------------
                    .then(literal("help")
                            .executes(context -> {
                                context.getSource().sendMessage(translatable("command.aineko.help"));
                                return 1;
                            })
                    )
                    //--------------------------------------add----------------------------------------------
                    .then(literal("add")
                            .then(argument("neko",StringArgumentType.string())
                                    .executes(AINekoCommand::add)
                            )
                    )
                    //----------------------------------------remove--------------------------------------------
                    .then(literal("remove")
                            .then(argument("neko",StringArgumentType.string())
                                    .executes(AINekoCommand::remove)
                            )
                    )
            );
            // -------------------------------------------trash-------------------------------------------------
            dispatcher.register(literal("totrash")
                    //------------------------------------------set------------------------------------------
                    .then(literal("set")
                            .executes(TrashCommand::set)
                    ));
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

