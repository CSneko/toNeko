package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;

@SuppressWarnings("UnstableApiUsage")
public class ToNekoCommand {
    public static void init(){
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = INSTANCE.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS,event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("toneko")
                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_HELP))
                    .executes(ToNekoCommand::helpCommand)
                            .then(Commands.literal("help")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_HELP))
                                    .executes(ToNekoCommand::helpCommand)
                            )
                            .then(Commands.literal("player")
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .requires(s -> check(s, Permissions.COMMAND_TONEKO_PLAYER))
                                            .executes(ToNekoCommand::playerCommand)
                                    )
                            ).then(Commands.literal("remove")
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .requires(s -> check(s, Permissions.COMMAND_TONEKO_REMOVE))
                                            .executes(ToNekoCommand::removeCommand)
                                    )
                            )
                            .then(Commands.literal("block")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_BLOCK))
                                    .then(Commands.argument("neko", ArgumentTypes.player())
                                        .then(Commands.literal("add")
                                            .then(Commands.argument("block", StringArgumentType.string())
                                                    .then(Commands.argument("replace",StringArgumentType.string())
                                                            .then(Commands.argument("method",StringArgumentType.string())
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
                                        .then(Commands.literal("remove")
                                            .then(Commands.argument("block", StringArgumentType.string())
                                                    .executes(ToNekoCommand::removeBlock)
                                            )
                                        )
                                    )
                            )
                            .then(Commands.literal("aliases")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_ALIAS))
                                    .then(Commands.argument("neko", ArgumentTypes.player())
                                        //-------------------------------------add---------------------------------------
                                        .then(Commands.literal("add")
                                            .then(Commands.argument("aliases", StringArgumentType.string())
                                                    .executes(ToNekoCommand::aliasesAdd)
                                            )
                                        ).then(Commands.literal("remove")
                                            .then(Commands.argument("aliases", StringArgumentType.string())
                                                    .executes(ToNekoCommand::aliasesRemove)
                                            )
                                        )
                                    )
                            )
                            .then(Commands.literal("xp")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_XP))
                                    .then(Commands.argument("neko", ArgumentTypes.player())
                                            .executes(ToNekoCommand::xp)
                                    )
                            )
                    .build()
            );
        });
    }

    public static int xp(CommandContext<CommandSourceStack> context) {
        try {
            Player player = (Player) context.getSource().getSender();
            Player nekoPlayer = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
            if (neko.hasOwner(player.getUniqueId())) {
                sendTransTo(player, "command.toneko.xp",nekoPlayer.getName(), neko.getXp(player.getUniqueId()));
            } else {
                sendTransTo(player, "messages.toneko.notOwner");
            }
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int aliasesRemove(CommandContext<CommandSourceStack> context) {
        try {
            Player player = (Player) context.getSource().getSender();
            Player nekoPlayer = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
            if (neko.hasOwner(player.getUniqueId())) {
                String aliases = StringArgumentType.getString(context, "aliases");
                neko.addAlias(player.getUniqueId(), aliases);
                sendTransTo(player, "command.toneko.aliases.remove",aliases);
            } else {
                sendTransTo(player, "messages.toneko.notOwner",nekoPlayer.getName());
            }
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int aliasesAdd(CommandContext<CommandSourceStack> context) {
        try{
            Player player = (Player) context.getSource().getSender();
            Player nekoPlayer = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
            if(neko.hasOwner(player.getUniqueId())){
                String aliases = StringArgumentType.getString(context, "aliases");
                neko.removeAlias(player.getUniqueId(), aliases);
                sendTransTo(player,"command.toneko.aliases.add",aliases);
            }else {
                sendTransTo(player,"messages.toneko.notOwner",nekoPlayer.getName());
            }
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int removeBlock(CommandContext<CommandSourceStack> context) {
        try{
            Player player = (Player) context.getSource().getSender();
            Player nekoPlayer = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
            if (!neko.hasOwner(player.getUniqueId())) {
                sendTransTo(player, "messages.toneko.notOwner",nekoPlayer.getName());
                return 1;
            }
            neko.removeOwner(player.getUniqueId());
            sendTransTo(player,"command.toneko.remove");
            neko.save();
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int addBlock(CommandContext<CommandSourceStack> context) {
        try {
            final Player player = (Player) context.getSource().getSender();
            Player nekoPlayer = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
            //获取关键信息
            String block = context.getArgument("block", String.class); //屏蔽词
            String replace = context.getArgument("replace", String.class); //替换词
            String method = context.getArgument("method", String.class); //all or word

            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());

            if (!neko.hasOwner(player.getUniqueId())) {
                sendTransTo(player,"messages.toneko.notOwner",nekoPlayer.getName());
                return 1;
            }
            // 添加屏蔽词
            neko.addBlock(block, replace, method);
            sendTransTo(player,"messages.toneko.block.add");
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int removeCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        Player nekoPlayer;
        try {
            nekoPlayer = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
        if (neko.hasOwner(player.getUniqueId())){
            neko.removeOwner(player.getUniqueId());
            neko.save();
            sendTransTo(player,"command.toneko.remove", nekoPlayer.getName());
        }else {
            sendTransTo(player,"messages.toneko.notOwner", nekoPlayer.getName());
        }
        return 1;
    }

    public static int playerCommand(CommandContext<CommandSourceStack> context) {
        Player nekoPlayer = null;
        try {
            nekoPlayer = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
        Player player = (Player) context.getSource().getSender();
        if (neko.isNeko()){
            if (neko.hasOwner(player.getUniqueId())){
                sendTransTo(player,"command.toneko.player.alreadyOwner",nekoPlayer.getName());
            }else {
                neko.addOwner(player.getUniqueId());
                sendTransTo(player,"command.toneko.player.success", nekoPlayer.getName());
            }
        }else {
            sendTransTo(player,"command.toneko.player.notNeko", nekoPlayer.getName());
        }
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.toneko.help");
        return 1;
    }
}
