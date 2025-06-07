package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.commands.arguments.CustomStringArgument;
import org.cneko.toneko.common.mod.commands.arguments.NekoArgument;
import org.cneko.toneko.common.mod.commands.arguments.NekoSuggestionProvider;
import org.cneko.toneko.common.mod.commands.arguments.WordSuggestionProvider;
import org.cneko.toneko.common.mod.entities.INeko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
import static org.cneko.toneko.common.mod.util.PermissionUtil.has;
public class ToNekoCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("toneko")
                    // 不允许终端执行
                    .requires(CommandSourceStack::isPlayer)
                    //----------------------------------------player-------------------------------------
                    .then(literal("player")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_PLAYER))
                            .then(argument("neko", NekoArgument.neko())
                                    .suggests(new NekoSuggestionProvider(false))
                                    .executes(ToNekoCommand::playerCommand)
                            )
                    )
                    //--------------------------------------accept-----------------------------------------
                    .then(literal("accept")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_ACCEPT))
                            .then(argument("owner", EntityArgument.player())
                                    .executes(ToNekoCommand::acceptCommand)
                            )
                    )
                    //-----------------------------------------deny-----------------------------------------
                    .then(literal("deny")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_DENY))
                            .then(argument("owner",EntityArgument.player())
                                    .executes(ToNekoCommand::denyCommand)
                            )
                    )
                    //--------------------------------------------------aliases--------------------------------------
                    .then(literal("aliases")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_ALIAS))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                            .suggests(new NekoSuggestionProvider(true))
                                    //-------------------------------------add---------------------------------------
                                    .then(literal("add")
                                            .then(argument("aliases", CustomStringArgument.blockWord())
                                                    .executes(ToNekoCommand::AliasesAdd)
                                            )
                                    ).then(literal("remove")
                                            .then(argument("aliases", StringArgumentType.word())
                                                    .suggests(WordSuggestionProvider.aliases())
                                                    .executes(ToNekoCommand::AliasesRemove)
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------------block--------------------------------------------
                    .then(literal("block")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_BLOCK))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                            .suggests(new NekoSuggestionProvider(true))
                                    //--------------------------------add------------------------------------
                                    .then(literal("add")
                                            .then(argument("block",CustomStringArgument.blockWord())
                                                    .then(argument("replace",CustomStringArgument.replaceWord())
                                                            .then(argument("method",StringArgumentType.word())
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
                                            .then(argument("block",CustomStringArgument.blockWord())
                                                    .executes(ToNekoCommand::removeBlock)
                                                    .suggests(WordSuggestionProvider.blockWord())
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------xp------------------------------------------------
                    .then(literal("xp")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_XP))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                    .suggests(new NekoSuggestionProvider(true))
                                    .executes(ToNekoCommand::xp)
                            )
                    )
                    //-------------------------------------------remove--------------------------------------
                    .then(literal("remove")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_REMOVE))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                    .suggests(new NekoSuggestionProvider(true))
                                    .executes(ToNekoCommand::remove)
                            )
                    )
                    //-------------------------------------help---------------------------------------------------
                    .then(literal("help")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_HELP))
                            .executes(ToNekoCommand::help)
                    )

                    //----------------------------------------无参数-----------------------------------------
                    .executes(ToNekoCommand::help)
            );
        });
    }

    public static int playerCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer(); // 命令发送者
            ServerPlayer neko = context.getArgument("neko", ServerPlayer.class);
            String nekoName = neko.getName().getString();
            assert player != null;
            if (!neko.isNeko()) {
                // 不是猫娘
                player.sendSystemMessage(translatable("command.toneko.player.notNeko", nekoName));
                return 1;
            }
            if (neko.hasOwner(player.getUUID())) {
                // 已经是主人
                player.sendSystemMessage(translatable("command.toneko.player.alreadyOwner", nekoName));
                return 1;
            }
            ownerMap.put(player,neko);
            player.sendSystemMessage(Component.translatable("command.toneko.player.send_request", nekoName).withStyle(ChatFormatting.LIGHT_PURPLE));
            MutableComponent component = Component.translatable("command.toneko.player.request", player.getName().getString()).withStyle(ChatFormatting.GOLD);
            MutableComponent denyButton = Component.translatable("misc.toneko.deny").withStyle(ChatFormatting.RED);
            denyButton.setStyle(denyButton.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/toneko deny "+player.getName().getString())));
            MutableComponent acceptButton = Component.translatable("misc.toneko.accept").withStyle(ChatFormatting.GREEN);
            acceptButton.setStyle(acceptButton.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/toneko accept "+player.getName().getString())));
            component.append(acceptButton);
            component.append(denyButton);
            neko.sendSystemMessage(component);
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    private static int denyCommand(CommandContext<CommandSourceStack> context) {
        return acceptOwner(context,false);
    }

    private static int acceptCommand(CommandContext<CommandSourceStack> context) {
        return acceptOwner(context,true);
    }
    private static Map<Player,Player> ownerMap = new HashMap<>(); // K为主人，V为Neko
    private static int acceptOwner(CommandContext<CommandSourceStack> context,boolean accept){
        Player neko = context.getSource().getPlayer();
        Player owner = null;
        try {
            owner = EntityArgument.getPlayer(context, "owner");
        } catch (CommandSyntaxException ignored) {
        }
        if (ownerMap.containsKey(owner) && ownerMap.get(owner).equals(neko)){
            if (accept){
                neko.addOwner(owner.getUUID(),new INeko.Owner(new ArrayList<>(), 0));
                neko.sendSystemMessage(Component.translatable("command.toneko.accept", owner.getName()).withStyle(ChatFormatting.GREEN));
                owner.sendSystemMessage(Component.translatable("command.toneko.player.accept", neko.getName()).withStyle(ChatFormatting.GREEN));
            }else {
                neko.sendSystemMessage(Component.translatable("command.toneko.accept", owner.getName()).withStyle(ChatFormatting.RED));
                owner.sendSystemMessage(Component.translatable("command.toneko.player.deny", neko.getName()).withStyle(ChatFormatting.RED));
            }
            ownerMap.remove(owner);
        }else {
            neko.sendSystemMessage(translatable("command.toneko.not_request"));
        }
        return 1;
    }

    public static int help(CommandContext<CommandSourceStack> context) {
        final CommandSourceStack source = context.getSource();
        source.sendSystemMessage(translatable("command.toneko.help"));
        return 1;
    }

    public static int remove(CommandContext<CommandSourceStack> context) {
        try {
            final Player player = context.getSource().getPlayer();
            Player neko = context.getArgument("neko", ServerPlayer.class);
            neko.removeOwner(player.getUUID());
            player.sendSystemMessage(translatable("command.toneko.remove", neko.getName().getString()));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int addBlock(CommandContext<CommandSourceStack> context) {
        try {
            final CommandSourceStack source = context.getSource();
            final Player player = source.getPlayer();
            //获取关键信息
            ServerPlayer neko = context.getArgument("neko", ServerPlayer.class); //猫娘
            String block = context.getArgument("block", String.class); //屏蔽词
            String replace = context.getArgument("replace", String.class); //替换词
            String method = context.getArgument("method", String.class); //all or word


            // 添加屏蔽词
            neko.addBlockedWord(new INeko.BlockedWord(block,replace, INeko.BlockedWord.BlockMethod.fromString(method)));
            player.sendSystemMessage(translatable("messages.toneko.block.add"));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int removeBlock(CommandContext<CommandSourceStack> context) {
        try {
            final CommandSourceStack source = context.getSource();
            final Player player = source.getPlayer();
            ServerPlayer neko = context.getArgument("neko", ServerPlayer.class); //猫娘的名称
            String block = context.getArgument("block", String.class); //屏蔽词

            neko.removeBlockedWord(block);
            player.sendSystemMessage(translatable("messages.toneko.block.remove"));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int xp(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            ServerPlayer neko = context.getArgument("neko", ServerPlayer.class);
            player.sendSystemMessage(translatable("command.toneko.xp", neko.getName().getString(), neko.getXpWithOwner(player.getUUID())));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int AliasesRemove(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer player = context.getSource().getPlayer();
            ServerPlayer neko =context.getArgument("neko", ServerPlayer.class);
            String aliases = StringArgumentType.getString(context, "aliases");
            neko.getOwner(player.getUUID()).getAliases().remove(aliases);
            player.sendSystemMessage(translatable("command.toneko.aliases.remove", aliases));
        return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int AliasesAdd(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            ServerPlayer neko = context.getArgument("neko", ServerPlayer.class);
            String aliases = StringArgumentType.getString(context, "aliases");
            neko.getOwner(player.getUUID()).getAliases().add(aliases);
            player.sendSystemMessage(translatable("command.toneko.aliases.add", aliases));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }
}
