package org.cneko.toneko.common.mod.commands.arguments;
import org.cneko.toneko.common.mod.entities.INeko;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.util.PlayerUtil;

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
        return ((INeko) player).isNeko();
    }

    /**
     * 在 executes 阶段校验 ownedNeko() 声明的属主约束。
     * Brigadier 的 parse(StringReader) 拿不到命令发送者，因此属主校验统一在执行期调用本方法完成。
     *
     * @return 校验通过的目标猫娘；校验失败（发送者不是其主人，或来源不是玩家）时向发送者反馈并返回 null
     */
    public static ServerPlayer checkOwned(CommandContext<CommandSourceStack> context, String argName) {
        ServerPlayer neko = context.getArgument(argName, ServerPlayer.class);
        ServerPlayer sender = context.getSource().getPlayer();
        if (sender == null) {
            return null;
        }
        if (!((INeko) neko).hasOwner(sender.getUUID())) {
            sender.sendSystemMessage(translatable("command.toneko.player.notOwner", neko.getName().getString()));
            return null;
        }
        return neko;
    }

    @Override
    public String toString() {
        return "NekoArgument";
    }
}
