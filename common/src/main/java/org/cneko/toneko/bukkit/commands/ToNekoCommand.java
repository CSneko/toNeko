package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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
                            )
                    .build()
            );
        });
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
