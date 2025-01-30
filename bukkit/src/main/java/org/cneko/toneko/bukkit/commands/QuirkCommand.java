package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cneko.toneko.bukkit.api.ClientStatus;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.quirks.Quirk;
import org.cneko.toneko.common.quirks.QuirkRegister;
import org.cneko.toneko.common.util.QuirkUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;

@SuppressWarnings("UnstableApiUsage")
public class QuirkCommand {
    public static CompletableFuture<Suggestions> getQuirksSuggestions(CommandContext<CommandSourceStack> source, SuggestionsBuilder builder) {
        // 获取quirks
        Collection<Quirk> quirks = QuirkRegister.getQuirks();
        for (Quirk quirk : quirks) {
            builder.suggest(quirk.getId());
        }
        return builder.buildFuture();
    }

    public static void init(){
        LifecycleEventManager<@NotNull Plugin> manager = INSTANCE.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS,event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("quirk")
                            .requires(s -> check(s, Permissions.COMMAND_QUIRK))
                            .executes(QuirkCommand::helpCommand)
                            .then(Commands.literal("help")
                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_HELP))
                                    .executes(QuirkCommand::helpCommand)
                            )
//                            .then(Commands.literal("add")
//                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_ADD))
//                                    .then(Commands.argument("quirk", StringArgumentType.string())
//                                            .suggests(QuirkCommand::getQuirksSuggestions)
//                                            .executes(QuirkCommand::addOrRemoveQuirk)
//                                    )
//                            )
//                            .then(Commands.literal("remove")
//                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_REMOVE))
//                                    .then(Commands.argument("quirk", StringArgumentType.string())
//                                            .suggests(QuirkCommand::getQuirksSuggestions)
//                                            .executes(QuirkCommand::addOrRemoveQuirk)
//                                    )
//                            )
//                            .then(Commands.literal("list")
//                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_LIST))
//                                    .executes(QuirkCommand::listQuirks)
//                            )
                            .then(Commands.literal("gui")
                                    //.requires(s -> (check(s, Permissions.COMMAND_QUIRK_GUI) && ClientStatus.isInstalled(s)))
                                    .executes(QuirkCommand::quirkGui)
                            )

                            .build()
            );
        });
    }

    private static int quirkGui(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = ((CraftPlayer)context.getSource().getSender()).getHandle();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        player.connection.send(new ClientboundCustomPayloadPacket(new QuirkQueryPayload(
                QuirkUtil.quirkToIds(neko.getQuirks()),
                QuirkRegister.getQuirkIds().stream().toList(),true)));
        return 1;
    }

    private static int listQuirks(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if(neko.getQuirks().isEmpty()){
            sendTransTo(player, "command.quirk.no_any_quirk");
            return 1;
        }

        return 1;
    }

    private static int addOrRemoveQuirk(CommandContext<CommandSourceStack> context) {
        return 1;
    }

    private static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.quirk.help");
        return 1;
    }
}
