package com.crystalneko.tonekofabric.event;

import com.crystalneko.ctlibPublic.File.JsonConfiguration;
import com.crystalneko.ctlibPublic.File.YamlConfiguration;
import com.crystalneko.ctlibPublic.inGame.chatPrefix;
import com.crystalneko.ctlibPublic.network.httpGet;
import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.crystalneko.tonekofabric.libs.base.translatable;

public class playerChat {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    public static void init(){
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            // 使用 CompletableFuture 异步处理聊天消息
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 执行耗时操作
                onChatMessage(sender,message.getContent());
            }, executorService);
            return false;
        });
    }
    public static void onChatMessage(PlayerEntity player, Text message) {
        if (message != null) {
            //获取玩家信息
            MinecraftServer server = player.getServer();
            String worldName = base.getWorldName(player.getWorld());
            String playerName = base.getPlayerName(player);
            Text newMessage = modifyMessage(message,worldName,playerName,false,server);
            // 发送消息给所有在线玩家
            sendMsg(newMessage,server);
        }
    }
    private static void sendMsg(Text message,MinecraftServer server){
        // 发送消息给所有在线玩家
        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(message);
        });
    }
    private static Text modifyMessage(Text message, String worldName, String playerName, Boolean isAI,MinecraftServer server) {
        lp.build();
        base.start(worldName);
        if (message == null || message.getString().isEmpty()) {
            return null;
        }

        //猫娘列表
        String[] nekoList = sqlite.readAllValueInAColumn(worldName + "Nekos", "neko");

        String stringMessage = message.getString();
        // 判断是否有主人
        if (sqlite.checkValueExists(worldName + "Nekos", "neko", playerName)) {
            String owner = sqlite.getColumnValue(worldName + "Nekos", "owner", "neko", playerName);
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


            if(!isAI){
                //读取配置文件
                YamlConfiguration config = null;
                try {
                    config = new YamlConfiguration(Path.of("ctlib/toneko/config.yml"));
                } catch (IOException e) {
                    System.out.println("无法加载配置文件:" + e.getMessage());
                }
                //如果不启用AI,则不执行以下代码
                if(config != null && config.getBoolean("AI.enable")) {
                    //如果不是AI发送，则检测是否有提到AI的名称
                    for (String str : nekoList) {
                        if (stringMessage.contains(str)) {
                            //对于存在的，进行处理
                            String type = sqlite.getColumnValue(worldName + "Nekos", "type", "neko", str);
                            if (base.getOwner(str, worldName).equalsIgnoreCase(playerName) && type != null && type.equalsIgnoreCase("AI")) {
                                //获取语言
                                String language = config.getString("language");
                                //获取API
                                String API = config.getString("AI.API");
                                //获取提示词
                                String prompt = config.getString("AI.prompt");
                                prompt = prompt.replaceAll("%name%",str);
                                prompt = prompt.replaceAll("%owner%",owner);
                                //替换用户输入中的&符号
                                String rightMsg = stringMessage.replaceAll("&", "and");
                                //构建链接
                                String url = API.replaceAll("%text%", rightMsg);
                                url = url.replaceAll("%prompt%", prompt);
                                //获取数据
                                JsonConfiguration response = null;
                                try {
                                    response = httpGet.getJson(url, null);
                                } catch (IOException e) {
                                    System.out.println("无法获取json:"+e.getMessage());
                                }
                                String AIMsg;
                                //读取响应
                                if(response != null) {
                                    if (language.equalsIgnoreCase("zh_cn")) {
                                        AIMsg = response.getString("response");
                                    } else {
                                        AIMsg = response.getString("source_response");
                                    }
                                    if (AIMsg != null) {
                                        //对AI的消息进行修改
                                        Text textAIMsg = modifyMessage(Text.of(AIMsg), worldName, playerName, true, server);
                                        //再次发送消息
                                        sendMsg(textAIMsg, server);
                                    }
                                }

                            }
                        }
                    }
                }
            }
        } else {
            stringMessage = playerName + "§b >> §7" + stringMessage;
        }
        return Text.of(stringMessage);
    }


    public static String replaceChar(String str, char oldChar, String newStr, double probability) {
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

    private static String replaceBlocks(String message,String neko,String worldName){
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