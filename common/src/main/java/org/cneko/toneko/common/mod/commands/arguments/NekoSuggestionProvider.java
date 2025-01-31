package org.cneko.toneko.common.mod.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.api.NekoQuery;

import java.util.concurrent.CompletableFuture;

public class NekoSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
    private final boolean requireOwned;

    public NekoSuggestionProvider(boolean requireOwned) {
        this.requireOwned = requireOwned;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        CommandSourceStack source = context.getSource();
        ServerPlayer commander = source.getPlayer(); // 命令执行者（可能是主人）

        // 遍历所有在线玩家，生成符合条件的建议
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (!isValidNeko(player, commander, requireOwned)) continue;

            String playerName = player.getName().getString();
            if (playerName.toLowerCase().startsWith(remaining)) {
                builder.suggest(playerName);
            }
        }

        return builder.buildFuture();
    }

    private boolean isValidNeko(ServerPlayer target, ServerPlayer commander, boolean checkOwnership) {
        // 服务器端检查是否为Neko
        NekoQuery.Neko neko = NekoQuery.getNeko(target.getUUID());
        if (!neko.isNeko()) return false;

        // 若需要检查主人关系
        if (checkOwnership) {
            return commander != null && neko.hasOwner(commander.getUUID());
        }

        return true;
    }
}