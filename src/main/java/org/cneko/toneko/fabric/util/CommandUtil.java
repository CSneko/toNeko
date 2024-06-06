package org.cneko.toneko.fabric.util;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static org.cneko.toneko.fabric.util.TextUtil.translatable;
public class CommandUtil {
    public static final SuggestionProvider<ServerCommandSource> getOnlinePlayers = (context, builder) -> {
        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            String playerTabList = player.getName().toString();
            //替换字符
            String output = playerTabList.replace("literal{", "").replace("}", "");
            builder.suggest(output);
        }
        return builder.buildFuture();
    };
    // 没有权限
    public static int noPS(ServerCommandSource player){
        player.sendMessage(translatable("command.toneko.noPermission"));
        return 1;
    }
    // 没有权限
    public static int noPS(PlayerEntity player){
        player.sendMessage(translatable("command.toneko.noPermission"));
        return 1;
    }
}
