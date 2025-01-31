package org.cneko.toneko.common.mod.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class NekoArgument implements ArgumentType<ServerPlayer> {
    private final boolean requireOwned;


    private NekoArgument(boolean requireOwned) {
        this.requireOwned = requireOwned;
    }

    // 仅校验目标是否是 Neko
    public static NekoArgument neko() {
        return new NekoArgument(false);
    }
    // 校验目标是否是 Neko 并且是主人
    public static NekoArgument ownedNeko() {
        return new NekoArgument(true);
    }


    @Override
    public ServerPlayer parse(StringReader reader) throws CommandSyntaxException {
        // 1. 获取输入的玩家名称
        String playerName = reader.readString();

        // 2. 获取目标玩家
        ServerPlayer targetPlayer = (ServerPlayer) PlayerUtil.getPlayerByName(playerName);
        if (targetPlayer == null) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                    .create("玩家 " + playerName + " 不存在或未在线");
        }

        // 3. 校验是否是 Neko
        if (!checkNeko(targetPlayer)) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                    .create(translatable("command.toneko.player.notNeko", playerName).getString());
        }


        // 返回目标玩家对象
        return targetPlayer;
    }

    private boolean checkNeko(Player player) {
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        return neko.isNeko();
    }

    @Override
    public String toString() {
        return "NekoArgument";
    }
}