package org.cneko.toneko.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.impl.dimension.TaggedChoiceExtension;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.fabric.api.PlayerInstallToNeko;
import org.cneko.toneko.fabric.util.PermissionUtil;
import org.cneko.toneko.fabric.util.TextUtil;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

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

    public static int cCommand(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        context.getSource().sendMessage(Text.of(PlayerInstallToNeko.get(name)+""));
        return 1;
    }

    /**
     * 别被这名称误导了，这个命令是验证玩家是否按照模组的
     * @param context
     * @return
     */
    public static int twwdfCommand(CommandContext<ServerCommandSource> context) {
        PlayerEntity player = context.getSource().getPlayer();
        PlayerInstallToNeko.set(TextUtil.getPlayerName(player), true);
        return 1;
    }
}
