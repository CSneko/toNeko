package org.cneko.toneko.common.mod.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;


import java.util.concurrent.CompletableFuture;

public class WordSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
    private final Types type;
    private WordSuggestionProvider(Types type){
        this.type = type;
    }
    public static WordSuggestionProvider blockWord(){
        return new WordSuggestionProvider(Types.BLOCK);
    }
    public static WordSuggestionProvider aliases(){
        return new WordSuggestionProvider(Types.ALIASES);
    }
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        CommandSourceStack source = context.getSource();
        ServerPlayer commander = source.getPlayer(); // 命令执行者（可能是主人）
        ServerPlayer neko = context.getArgument("neko", ServerPlayer.class);
        if (neko == null){
            return builder.buildFuture();
        }else {
            if (!isValidNeko(neko, commander)) return builder.buildFuture();
            if (type == Types.BLOCK) {
                neko.getBlockedWords().forEach(blockWord -> {
                    if (blockWord.replace().toLowerCase().startsWith(remaining)) {
                        builder.suggest(blockWord.replace());
                    }
                });
            } else if (type == Types.ALIASES) {
                neko.getOwner(commander.getUUID()).getAliases().forEach(alias -> {
                    if (alias.toLowerCase().startsWith(remaining)) {
                        builder.suggest(alias);
                    }
                });
            }
        }
        return builder.buildFuture();
    }

    private boolean isValidNeko(ServerPlayer neko, ServerPlayer commander) {
        // 服务器端检查是否为Neko
        if (!neko.isNeko()) return false;

        // 若需要检查主人关系
        return commander != null && neko.hasOwner(commander.getUUID());
    }

    enum Types{
        BLOCK,
        ALIASES
    }
}
