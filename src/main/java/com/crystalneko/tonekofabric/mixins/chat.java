package com.crystalneko.tonekofabric.mixins;

import com.crystalneko.ctlibPublic.inGame.chatPrefix;
import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import com.mojang.brigadier.ParseResults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.SignedCommandArguments;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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

import static com.crystalneko.tonekofabric.libs.base.translatable;

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
        lp.build();
        String worldName = base.getWorldName(player.getWorld());
        base.start(worldName);
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
            String ownerText = translatable("chat.neko.owner").getString();
            if (owner != null && !owner.isEmpty()) {
                stringMessage = stringMessage.replaceAll(owner,ownerText);
            }
            //获取别名
            String strAliases = sqlite.getColumnValue(worldName + "Nekos","aliases","neko",playerName);
            if(strAliases != null) {
                String[] aliases = strAliases.split(",");
                //替换别名
                for (String value : aliases) {
                    stringMessage = stringMessage.replaceAll(value, ownerText);
                }
            }
            // 随机将",，"替换为"喵~"
            String nya = translatable("chat.neko.nya").getString();
            stringMessage = replaceChar(stringMessage, ',', nya, 0.4);
            stringMessage = replaceChar(stringMessage, '，', nya, 0.4);
            stringMessage = stringMessage + nya;

            //替换屏蔽词
            stringMessage = replaceBlocks(stringMessage,playerName,worldName);

            //获取聊天前缀
            String libPublicPrefix = chatPrefix.getAllPublicPrefixValues();
            String libPrefix = chatPrefix.getPrivatePrefix(playerName);
            if(libPrefix.equalsIgnoreCase("[§a无前缀§f§r]")){
                libPrefix = "";
            }
            String prefix = libPrefix + libPublicPrefix;
            stringMessage = prefix  + playerName + "§b >> §7" + stringMessage;

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

    private String replaceBlocks(String message,String neko,String worldName){
        //检查值是否存在
        if(sqlite.checkValueExists(worldName +"Nekos", "neko", neko)) {
            //读取数据
            String block = sqlite.getColumnValue(worldName +"Nekos", "block", "neko", neko);
            String replace = sqlite.getColumnValue(worldName +"Nekos", "replace", "neko", neko);
            String method = sqlite.getColumnValue(worldName +"Nekos", "method", "neko", neko);
            if(block != null) {
                //转换为数组
                String[] blocks = block.split(",");
                String[] replaces = replace.split(",");
                String[] methods = method.split(",");
                //获取数组长度
                int length = blocks.length;
                //判断是否存在all
                int allIndex = Arrays.binarySearch(methods, "all");
                //判断是否存在屏蔽词
                if(allIndex >= 0 && message.contains(blocks[allIndex])){
                    //直接替换屏蔽词
                    message = message.replaceAll(message,replaces[allIndex]);
                } else {
                    //循环替换
                    int i = 0;
                    while (i < length) {
                        message = message.replaceAll(blocks[i], replaces[i]);
                        i ++;
                    }
                }
            }
        }
        return message;
    }
}