package org.cneko.toneko.bukkit.commands;

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
import org.cneko.toneko.bukkit.api.NekoStatus;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;

@SuppressWarnings("UnstableApiUsage")
public class ToNekoAdminCommand {
    public static void init() {
        LifecycleEventManager<@NotNull Plugin> manager = INSTANCE.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("tonekoadmin")
                            .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN))
                            .then(Commands.literal("help")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_HELP))
                                    .executes(ToNekoAdminCommand::helpCommand)
                            )
                            .executes(ToNekoAdminCommand::helpCommand)
                            .then(Commands.literal("set")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_SET))
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ToNekoAdminCommand::setCommand)
                                    )
                            )
                            .build()
            );
        });
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.tonekoadmin.help");
        return 1;
    }

    public static int setCommand(CommandContext<CommandSourceStack> context) {
        Player nekoPlayer;
        try {
            nekoPlayer = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        boolean isNeko = NekoQuery.isNeko(nekoPlayer.getUniqueId());
        if (isNeko){
            NekoStatus.setToNotNeko(nekoPlayer);
            sendTransTo(nekoPlayer,"command.tonekoadmin.set.false", nekoPlayer.getName());
        }else {
            NekoStatus.setToNeko(nekoPlayer);
            sendTransTo(nekoPlayer,"command.tonekoadmin.set.true", nekoPlayer.getName());
        }
        return 1;
    }
}
