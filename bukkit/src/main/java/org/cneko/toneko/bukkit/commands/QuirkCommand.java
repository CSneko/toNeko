package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cneko.toneko.bukkit.util.PayloadSender;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;

@SuppressWarnings("UnstableApiUsage")
public class QuirkCommand {
    public static CompletableFuture<Suggestions> getQuirksSuggestions(CommandContext<CommandSourceStack> source, SuggestionsBuilder builder) {
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
                            .then(Commands.literal("list")
                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_LIST))
                                    .executes(QuirkCommand::listQuirks)
                            )
                            .then(Commands.literal("add")
                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_ADD))
                                    .then(Commands.argument("quirk", StringArgumentType.word())
                                            .suggests(QuirkCommand::getQuirksSuggestions)
                                            .executes(QuirkCommand::addQuirk)
                                    )
                            )
                            .then(Commands.literal("remove")
                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_REMOVE))
                                    .then(Commands.argument("quirk", StringArgumentType.word())
                                            .suggests(QuirkCommand::getQuirksSuggestions)
                                            .executes(QuirkCommand::removeQuirk)
                                    )
                            )
                            .then(Commands.literal("gui")
                                    .requires(s -> check(s, Permissions.COMMAND_QUIRK_GUI))
                                    .executes(QuirkCommand::guiCommand)
                            )
                            .build()
            );
        });
    }

    private static int listQuirks(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if (neko.getQuirks().isEmpty()) {
            sendTransTo(player, "command.quirk.no_any_quirk");
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6Quirks: ");
        for (String q : neko.getQuirks()) {
            Quirk quirk = QuirkRegister.getById(q);
            sb.append(quirk != null ? quirk.getId() : q).append(", ");
        }
        player.sendMessage(Component.text(sb.substring(0, sb.length() - 2)));
        return 1;
    }

    private static int addQuirk(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String qid = StringArgumentType.getString(context, "quirk");
        Quirk quirk = QuirkRegister.getById(qid);
        if (quirk == null) {
            sendTransTo(player, "command.quirk.not_found", qid);
            return 1;
        }
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if (!neko.isNeko()) {
            sendTransTo(player, "command.neko.not_neko");
            return 1;
        }
        neko.getQuirks().add(qid);
        neko.save();
        sendTransTo(player, "command.quirk.add", quirk.getId());
        return 1;
    }

    private static int removeQuirk(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String qid = StringArgumentType.getString(context, "quirk");
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if (!neko.isNeko()) {
            sendTransTo(player, "command.neko.not_neko");
            return 1;
        }
        neko.getQuirks().remove(qid);
        neko.save();
        sendTransTo(player, "command.quirk.remove", qid);
        return 1;
    }

    private static int guiCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if (org.cneko.toneko.bukkit.api.ClientStatus.isInstalled(player)) {
            var allQuirks = QuirkRegister.getQuirkIds().stream().collect(Collectors.toList());
            PayloadSender.sendQuirkResponse(player,
                    new ArrayList<>(neko.getQuirks()), allQuirks, true);
        } else {
            sendTransTo(player, "messages.toneko.mod_required");
        }
        return 1;
    }

    private static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.quirk.help");
        return 1;
    }
}
