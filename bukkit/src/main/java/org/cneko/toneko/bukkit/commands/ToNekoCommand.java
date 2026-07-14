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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cneko.toneko.bukkit.util.PayloadSender;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;

@SuppressWarnings("UnstableApiUsage")
public class ToNekoCommand {
    // Neko UUID → Requester UUID
    private static final Map<UUID, UUID> PENDING_REQUESTS = new ConcurrentHashMap<>();
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
                            .then(Commands.literal("gui")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_GUI))
                                    .executes(ToNekoCommand::gui)
                            )
                            .then(Commands.literal("accept")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_ACCEPT))
                                    .then(Commands.argument("neko", ArgumentTypes.player())
                                            .executes(ToNekoCommand::accept)
                                    )
                            )
                            .then(Commands.literal("deny")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKO_DENY))
                                    .then(Commands.argument("neko", ArgumentTypes.player())
                                            .executes(ToNekoCommand::deny)
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
            }else if (PENDING_REQUESTS.containsKey(nekoPlayer.getUniqueId())){
                sendTransTo(player,"command.toneko.player.alreadyOwner",nekoPlayer.getName());
            }else {
                PENDING_REQUESTS.put(nekoPlayer.getUniqueId(), player.getUniqueId());
                sendTransTo(player,"command.toneko.player.send_request", nekoPlayer.getName());
                if (nekoPlayer.isOnline()) {
                    nekoPlayer.sendMessage(Component.text("§6" + player.getName() + " §e想成为你的主人！用 /toneko accept " + player.getName() + " 接受"));
                }
            }
        }else {
            sendTransTo(player,"command.toneko.player.notNeko", nekoPlayer.getName());
        }
        return 1;
    }

    public static int deny(CommandContext<CommandSourceStack> context) {
        // Player being denied (the neko who received the request)
        Player nekoPlayer = (Player) context.getSource().getSender();
        Player requester;
        try {
            requester = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) { return 0; }
        PENDING_REQUESTS.remove(nekoPlayer.getUniqueId());
        sendTransTo(nekoPlayer, "command.toneko.deny", requester.getName());
        return 1;
    }

    public static int accept(CommandContext<CommandSourceStack> context) {
        // Player being accepted as owner (the neko who received the request)
        Player nekoPlayer = (Player) context.getSource().getSender();
        Player requester;
        try {
            requester = context.getArgument("neko", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) { return 0; }
        UUID requesterId = PENDING_REQUESTS.remove(nekoPlayer.getUniqueId());
        if (requesterId == null || !requesterId.equals(requester.getUniqueId())) {
            sendTransTo(nekoPlayer, "messages.toneko.no_request", requester.getName());
            return 1;
        }
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
        neko.addOwner(requester.getUniqueId());
        neko.save();
        sendTransTo(nekoPlayer, "command.toneko.accept", requester.getName());
        if (requester.isOnline()) {
            sendTransTo(requester, "command.toneko.accept.neko", nekoPlayer.getName());
        }
        return 1;
    }

    public static int gui(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        if (org.cneko.toneko.bukkit.api.ClientStatus.isInstalled(player)) {
            // Open management screen with simple data: isNeko(bool) + pendingReqCount(int) + ownedNekoCount(int)
            try {
                var bos = new java.io.ByteArrayOutputStream();
                var out = new java.io.DataOutputStream(bos);
                out.writeBoolean(NekoQuery.isNeko(player.getUniqueId()));
                int pendingCount = 0;
                for (UUID u : PENDING_REQUESTS.keySet()) {
                    if (u.equals(player.getUniqueId())) pendingCount++;
                }
                out.writeInt(pendingCount);
                int ownedCount = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    NekoQuery.Neko n = NekoQuery.getNeko(p.getUniqueId());
                    if (n.isNeko() && n.hasOwner(player.getUniqueId())) ownedCount++;
                }
                out.writeInt(ownedCount);
                out.flush();
                PayloadSender.sendManagementData(player, bos.toByteArray());
            } catch (Exception ignored) {}
        } else {
            sendTransTo(player, "messages.toneko.mod_required");
        }
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.toneko.help");
        return 1;
    }
}
