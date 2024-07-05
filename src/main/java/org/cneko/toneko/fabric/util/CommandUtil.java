package org.cneko.toneko.fabric.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.toneko.common.quirks.Quirk;
import org.cneko.toneko.common.quirks.QuirkRegister;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    public static CompletableFuture<Suggestions> getQuirksSuggestions(CommandContext<ServerCommandSource> source, SuggestionsBuilder builder) {
        // 获取quirks
        List<Quirk> quirks = QuirkRegister.getQuirks();
        for (Quirk quirk : quirks) {
            builder.suggest(quirk.getId());
        }
        return builder.buildFuture();
    }
}
