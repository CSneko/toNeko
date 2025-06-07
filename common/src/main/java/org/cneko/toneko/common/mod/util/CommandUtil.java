package org.cneko.toneko.common.mod.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
public class CommandUtil {
    public static final SuggestionProvider<CommandSourceStack> getOnlinePlayers = (context, builder) -> {
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String playerTabList = player.getName().toString();
            //替换字符
            String output = playerTabList.replace("literal{", "").replace("}", "");
            builder.suggest(output);
        }
        return builder.buildFuture();
    };


    public static CompletableFuture<Suggestions> getQuirksSuggestions(CommandContext<CommandSourceStack> source, SuggestionsBuilder builder) {
        // 获取quirks
        Collection<Quirk> quirks = QuirkRegister.getQuirks();
        for (Quirk quirk : quirks) {
            builder.suggest(quirk.getId());
        }
        return builder.buildFuture();
    }
}
