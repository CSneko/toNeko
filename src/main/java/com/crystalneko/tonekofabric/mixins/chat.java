package com.crystalneko.tonekofabric.mixins;

import com.crystalneko.ctlibPublic.sql.sqlite;
import com.mojang.brigadier.ParseResults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.SignedCommandArguments;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class chat{
    @Shadow @Final private ServerPlayerEntity player;

    @Shadow protected abstract ServerCommandSource method_45002(SignedCommandArguments par1, ServerCommandSource par2);

    @Shadow public abstract void requestTeleport(double x, double y, double z, float yaw, float pitch);

    @Shadow protected abstract CompletableFuture method_46366(PublicPlayerSession par1, Executor par2);

    @Shadow protected abstract void setTextToBook(List<FilteredMessage> messages, UnaryOperator<String> postProcessor, ItemStack book);

    @Shadow public abstract void requestTeleport(double x, double y, double z, float yaw, float pitch, Set<PositionFlag> flags);

    @Shadow protected abstract ParseResults<ServerCommandSource> parse(String command);

    @Shadow public abstract boolean accepts(Packet<?> packet);

    // 使用 @Inject 注解插入代码到原始的方法中
    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    public void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo info) {
        //取消消息
        info.cancel();
        MinecraftServer server = player.getServer();
        // 对消息进行处理或者取消
        Text message = Text.of(packet.chatMessage());
        if (message != null) {
            Text newMessage = modifyMessage(message,player);
            if (newMessage != null) {
                // 发送消息给所有在线玩家
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    player.sendMessage(newMessage);
                });

            } else {
                // 取消该消息
                info.cancel();
            }
        }
    }

    // 用来修改聊天消息
    private Text modifyMessage(Text message, PlayerEntity player) {
        String worldName = player.getWorld().asString();
        sqlite.addColumn(worldName + "Nekos","neko");
        sqlite.addColumn(worldName + "Nekos","owner");
        sqlite.addColumn(worldName + "Nekos","aliases");
        if (message == null || message.getString().isEmpty()) {
            return null;
        }
        String stringMessage = message.getString();
        String playerName = player.getName().getString();
        playerName = playerName.replace("literal{", "").replace("}", "");
        // 判断是否有主人
        if (sqlite.checkValueExists(worldName + "Nekos", "neko", playerName)) {
            String owner = sqlite.getColumnValue(worldName + "Nekos", "owner", "neko", player.getName().getString());
            // 替换主人名称
            String ownerText = Text.translatable("chat.neko.owner").getString();
            if (owner != null && !owner.isEmpty()) {
                stringMessage = stringMessage.replaceAll(owner,ownerText);
            }
            //获取别名
            String strAliases = sqlite.getColumnValue(worldName + "Nekos","aliases","neko",playerName);
            String[] aliases = strAliases.split(",");
            //替换别名
            for (String value : aliases) {
                stringMessage = stringMessage.replaceAll(value, ownerText);
            }

            // 随机将",，"替换为"喵~"
            String nya = Text.translatable("chat.neko.nya").getString();
            stringMessage = replaceChar(stringMessage, ',', nya, 0.4);
            stringMessage = replaceChar(stringMessage, '，', nya, 0.4);
            stringMessage = stringMessage + nya;
            stringMessage = Text.translatable("chat.neko.prefix") + playerName + "§b >> §7" + stringMessage;

            return Text.of(stringMessage);
        } else {
            stringMessage = playerName + "§b >> §7" + stringMessage;
            return Text.of(stringMessage);
        }
    }


    public String replaceChar(String str, char oldChar, String newStr, double probability) {
        StringBuilder builder = new StringBuilder(str);
        Random random = new Random();

        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == oldChar && random.nextDouble() <= probability) {
                builder.replace(i, i + 1, newStr);
                i += newStr.length() - 1;
            }
        }

        return builder.toString();
    }
}