package org.cneko.toneko.neoforge.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.neoforge.fabric.api.PlayerInstallToNeko;
import org.cneko.toneko.neoforge.fabric.util.TextUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TwwdfCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("twwdf")
                    .then(literal("twwdf")
                            .executes(TwwdfCommand::twwdfCommand)
                    )
                    .then(literal("c")
                            .then(argument("name", StringArgumentType.string())
                                    .executes(TwwdfCommand::cCommand)
                            )
                    )
            );
        });

    }

    public static int cCommand(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        context.getSource().sendSystemMessage(Component.nullToEmpty(PlayerInstallToNeko.get(name)+""));
        return 1;
    }

    /**
     * 别被这名称误导了，这个命令是验证玩家是否按照模组的
     * @param context
     * @return
     */
    public static int twwdfCommand(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        PlayerInstallToNeko.set(TextUtil.getPlayerName(player), true);
        return 1;
    }
}
