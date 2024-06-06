package com.crystalneko.toneko.chat;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.api.NekoQuery;
import com.crystalneko.toneko.api.events.ChatEvents;
import com.crystalneko.tonekocommon.Stats;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.ctlib.common.network.HttpGet.SimpleHttpGet;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.crystalneko.toneko.ToNeko.*;
import static org.bukkit.Bukkit.getServer;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;
import static com.crystalneko.toneko.ToNeko.config;
public class NekoChatListener implements Listener{
    private final File dataFile = new File( "plugins/toNeko/nekos.yml");
    private final YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

    /*
    代码逻辑：
    玩家发送消息 -> 处理监听事件 -> 处理消息 -> 处理AI信息 -> AI发送消息
     */
    public void bootstrap(){
        //注册玩家聊天监听器
        try {
            //使用Paper的聊天监听器
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            logger.info(ToNeko.getMessage("folia.use.chatEvent"));
            getServer().getPluginManager().registerEvent(AsyncChatEvent.class,this, EventPriority.NORMAL, (listener, event) -> onPlayerChatPaper((AsyncChatEvent) event), pluginInstance);
        } catch (ClassNotFoundException e) {
            getServer().getPluginManager().registerEvent(org.bukkit.event.player.AsyncPlayerChatEvent.class,this, EventPriority.NORMAL, (listener, event) -> onPlayerChat((org.bukkit.event.player.AsyncPlayerChatEvent) event),pluginInstance);
        }
    }

    public void sendMessage(String playerName, String prefix, String formattedMessage) {
        String format = config.getString("chat.format");
        if(format == null)return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(format.replace("${prefix}", prefix).replace("${name}",playerName).replace("${msg}", formattedMessage));
        }
    }

    public void sendMessageToPlayers(String player, String prefix, String message, boolean isAI) {
        NekoQuery data = new NekoQuery(player,this.data);

        if (data.hasOwner()) {
            String owner = data.getOwner();
            List<String> aliases = data.getAlias();

            String catMessage = catChatMessage(message, owner, aliases);
            catMessage = replaceBlocks(catMessage, player);
            message = catMessage;
        }

        sendMessage(player, prefix, message);

        if(!isAI && config.getBoolean("AI.enable")){
            // 创建一个List来存储所有type为AI的条目
            List<String> nekoList = new ArrayList<>();
            YamlConfiguration yData = data.getData();

            // 遍历所有键值对
            for (String key : yData.getKeys(false)) {
                // 检查是否有"type"键以及是否为"AI"
                if (yData.contains(key + ".type") && yData.getString(key + ".type").equals("AI")) {
                    // 将符合条件的条目加入到List中
                    nekoList.add(key);
                }
            }

            for (String str : nekoList) {
                if (message.contains(str)) {
                    final String tempMessageFinalCopy = message;
                    SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
                        NekoQuery aiData = new NekoQuery(str,this.data);
                        String owner = aiData.getOwner();
                        if(owner != null && owner.equalsIgnoreCase(player)){
                            //获取语言
                            String language = config.getString("language");
                            //获取API
                            String API = config.getString("AI.API");
                            //获取提示词
                            String prompt = config.getString("AI.prompt")
                                    .replaceAll("%name%",str)
                                    .replaceAll("%owner%",owner);
                            //替换用户输入中的&符号
                            String rightMsg = tempMessageFinalCopy.replaceAll("&", "and");
                            //构建链接
                            final String url = API.replaceAll("%text%", rightMsg).replaceAll("%prompt%", prompt);

                            JsonConfiguration response = null;

                            try {
                                response = SimpleHttpGet.getJson(url, null);
                            } catch (Exception e) {
                                ToNeko.pluginInstance.getLogger().warning("无法获取json:" + e.getMessage());
                            }

                            //读取响应
                            if(response != null) {
                                String finalMessage;

                                if (language.equalsIgnoreCase("zh_cn")) {
                                    finalMessage = response.getString("response");
                                } else {
                                    finalMessage = response.getString("source_response");
                                }

                                if (finalMessage != null) {
                                    sendMessageToPlayers(str,"["+getMessage("chat.neko.prefix")+"]", finalMessage, true);
                                }
                            }
                        }
                    });
                }
            }

        }
    }


    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        ChatEvents.OnChat onChatEvents = new ChatEvents.OnChat(player, message, event);
        if(onChatEvents.isCancelled())return;
        String publicPrefix = ChatPrefix.getAllPublicPrefixValues();
        String privatePrefix = ChatPrefix.getPrivatePrefix(player.getName());
        String prefix = publicPrefix + privatePrefix;
        if(prefix.equalsIgnoreCase("[§a无前缀§f§r]")){
            prefix = "";
        }
        sendMessageToPlayers(player.getName(), prefix, message, false);
        // 发送统计信息
        Stats.meowInChat(player.getName(), message);
    }

    public void onPlayerChatPaper(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = MiniMessage.miniMessage().serialize(event.message());
        ChatEvents.OnChat onChatEvents = new ChatEvents.OnChat(player, message, event);
        if(onChatEvents.isCancelled())return;
        String publicPrefix = ChatPrefix.getAllPublicPrefixValues();
        String privatePrefix = ChatPrefix.getPrivatePrefix(player.getName());
        String prefix = publicPrefix + privatePrefix;
        if(prefix.equalsIgnoreCase("[§a无前缀§f§r]")){
            prefix = "";
        }
        sendMessageToPlayers(player.getName(), prefix, message, false);
        // 发送统计信息
        Stats.meowInChat(player.getName(), message);
    }

    public String catChatMessage(String message, String owner, List<String> aliases){
        //将玩家名称替换为主人
        message = message.replaceAll(owner, ToNeko.getMessage("other.owner"));
        //将别名替换为主人
        for (String value : aliases) {
            message = message.replaceAll(value, ToNeko.getMessage("other.owner"));
        }
        //随机将",，"替换为"喵~"
        message = replaceChar(message, ',',ToNeko.getMessage("other.nya"),0.4);
        message = replaceChar(message, '，',ToNeko.getMessage("other.nya"),0.4);
        //将最后替换成"喵~"
        if (!message.endsWith(ToNeko.getMessage("other.nya"))) {
            if (message.endsWith(".")) {
                message = replaceChar(message, '.', ToNeko.getMessage("other.nya"), 1.0);
            } else {
                message = message + ToNeko.getMessage("other.nya");
            }
        }
        return message;
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

    private String replaceBlocks(String message,String neko){
        //检查值是否存在
            //读取数据
            String block = sqlite.getColumnValue("nekoblockword", "block", "neko", neko);
            String replace = sqlite.getColumnValue("nekoblockword", "replace", "neko", neko);
            String method = sqlite.getColumnValue("nekoblockword", "method", "neko", neko);
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

        return message;
    }
}
