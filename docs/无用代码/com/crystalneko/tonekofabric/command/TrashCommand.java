package com.crystalneko.tonekofabric.command;

import com.crystalneko.tonekofabric.api.Messages;
import com.crystalneko.tonekofabric.util.TextUtil;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

public class TrashCommand {
    public static int set(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        assert player != null;
        String playerName = TextUtil.getPlayerName(player);
        Messages.setTrash(playerName,true);
        player.sendMessage(Messages.translatable("command.totrash.set",playerName));
        return 1;
    }
}
