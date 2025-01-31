package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.commands.arguments.NekoArgument;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import java.util.function.Predicate;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import static org.cneko.toneko.common.mod.util.CommandUtil.*;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
import static org.cneko.toneko.common.mod.util.PermissionUtil.has;
public class ToNekoCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("toneko")
                    //----------------------------------------player-------------------------------------
                    .then(literal("player")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_PLAYER))
                            .then(argument("neko", NekoArgument.neko())
                                    .executes(ToNekoCommand::playerCommand)
                            )
                    )
                    //--------------------------------------------------aliases--------------------------------------
                    .then(literal("aliases")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_ALIAS))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                    //-------------------------------------add---------------------------------------
                                    .then(literal("add")
                                            .then(argument("aliases", StringArgumentType.word())
                                                    .executes(ToNekoCommand::AliasesAdd)
                                            )
                                    ).then(literal("remove")
                                            .then(argument("aliases", StringArgumentType.word())
                                                    .executes(ToNekoCommand::AliasesRemove)
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------------block--------------------------------------------
                    .then(literal("block")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_BLOCK))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                    //--------------------------------add------------------------------------
                                    .then(literal("add")
                                            .then(argument("block",StringArgumentType.word())
                                                    .then(argument("replace",StringArgumentType.word())
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
                                            .then(argument("block",StringArgumentType.word())
                                                    .executes(ToNekoCommand::removeBlock)
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------xp------------------------------------------------
                    .then(literal("xp")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_XP))
                            .then(argument("neko",NekoArgument.ownedNeko())
                                    .executes(ToNekoCommand::xp)
                            )
                    )
                    //-------------------------------------------remove--------------------------------------
                    .then(literal("remove")
                            .requires(source -> has(source, Permissions.COMMAND_TONEKO_REMOVE))
                            .then(argument("neko",NekoArgument.ownedNeko())
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

    public static int help(CommandContext<CommandSourceStack> context) {
        final CommandSourceStack source = context.getSource();
        source.sendSystemMessage(translatable("command.toneko.help"));
        return 1;
    }

    public static int remove(CommandContext<CommandSourceStack> context) {
        try {
            final Player player = context.getSource().getPlayer();
            Player nekoP = context.getArgument("neko", ServerPlayer.class);
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoP.getUUID());
            neko.removeOwner(player.getUUID());
            player.sendSystemMessage(translatable("command.toneko.remove", nekoP.getName().getString()));
            neko.save();
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
            ServerPlayer nekoP = context.getArgument("neko", ServerPlayer.class); //猫娘
            String block = context.getArgument("block", String.class); //屏蔽词
            String replace = context.getArgument("replace", String.class); //替换词
            String method = context.getArgument("method", String.class); //all or word

            NekoQuery.Neko neko = NekoQuery.getNeko(nekoP.getUUID());

            // 添加屏蔽词
            neko.addBlock(block, replace, method);
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
            ServerPlayer nekoP = context.getArgument("neko", ServerPlayer.class); //猫娘的名称
            String block = context.getArgument("block", String.class); //屏蔽词

            NekoQuery.Neko neko = NekoQuery.getNeko(nekoP.getUUID());
            neko.removeBlock(block);
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
            ServerPlayer nekoP = context.getArgument("neko", ServerPlayer.class);
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoP.getUUID());
            player.sendSystemMessage(translatable("command.toneko.xp", nekoP.getName().getString(), neko.getXp(player.getUUID())));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int AliasesRemove(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer player = context.getSource().getPlayer();
            NekoQuery.Neko neko = NekoQuery.getNeko(context.getArgument("neko", ServerPlayer.class).getUUID());
            String aliases = StringArgumentType.getString(context, "aliases");
            neko.removeAlias(player.getUUID(), aliases);
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
            NekoQuery.Neko neko = NekoQuery.getNeko(context.getArgument("neko", ServerPlayer.class).getUUID());
            String aliases = StringArgumentType.getString(context, "aliases");
            neko.addAlias(player.getUUID(), aliases);
            player.sendSystemMessage(translatable("command.toneko.aliases.add", aliases));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }

    public static int playerCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer(); // 命令发送者
            ServerPlayer nekoP = context.getArgument("neko", ServerPlayer.class);
            String nekoName = nekoP.getName().getString();
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoP.getUUID());
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
            neko.addOwner(player.getUUID());
            player.sendSystemMessage(translatable("command.toneko.player.success", nekoName));
            return 1;
        }catch (Exception e){
            Bootstrap.LOGGER.error(e);
            return 1;
        }
    }
}
