package org.cneko.toneko.bukkit.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.cneko.toneko.bukkit.ChatModeHolder;
import org.cneko.toneko.bukkit.util.PayloadSender;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.*;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class ChatEvent implements Listener {
    private static final Random RANDOM = new Random();
    private static final double AREA_RANGE = 64.0;

    public static void init(){
        Bukkit.getServer().getPluginManager().registerEvents(new ChatEvent(), INSTANCE);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        String message = event.signedMessage().message();

        // 1. Preprocess: pet phrases (nya insertion) if player is neko
        if (neko.isNeko()) {
            String phrase = translatable(org.cneko.toneko.common.util.LanguageUtil.phrase);
            message = Messaging.PhraseProcessor.runPetPhrases(message, phrase);
        }

        // 2. Modify: owner aliases, blocked words
        String nickname = neko.getNickName();
        message = modify(message, neko);
        message = format(message, player, nickname);

        // 3. Route: area vs global
        // CHAT_MODES is stored in common; we store a local copy to avoid importing NekoEntity-referencing classes
        boolean areaMode = org.cneko.toneko.bukkit.ChatModeHolder.isAreaChat(player.getUniqueId());

        if (areaMode) {
            sendAreaMessage(player, message);
            processAreaChatAI(player, event.signedMessage().message());
        } else {
            sendGlobalMessage(message);
            processPrefixChatAI(player, event.signedMessage().message());
        }

        // 4. Stats & XP
        int count = Stats.getMeow(message);
        neko.addLevel((double) count / 1000.00);
        if (ConfigUtil.isStatsEnable()) Stats.meowInChat(player.getName(), count);
    }

    /** Send to all online players */
    private void sendGlobalMessage(String message) {
        Bukkit.getServer().sendMessage(Component.text(message));
        LOGGER.info(message.replaceAll("§[0-9a-fk-or]", ""));
    }

    /** Send only to nearby players (64-block radius) */
    private void sendAreaMessage(Player sender, String message) {
        Component msg = Component.text(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == sender || p.getLocation().getWorld() == sender.getLocation().getWorld()
                    && p.getLocation().distanceSquared(sender.getLocation()) <= AREA_RANGE * AREA_RANGE) {
                p.sendMessage(msg);
            }
        }
        LOGGER.info("[Area] " + sender.getName() + ": " + message.replaceAll("§[0-9a-fk-or]", ""));
    }

    /** Area chat: trigger AI for any message if a neko player is nearby (16 blocks) */
    private void processAreaChatAI(Player sender, String rawMessage) {
        if (!ConfigUtil.isAIEnabled() || rawMessage.isEmpty()) return;
        // Find nearest neko player within 16 blocks
        UUID nearestNeko = null;
        double nearestDist = 256; // 16^2
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == sender) continue;
            if (p.getLocation().getWorld() != sender.getLocation().getWorld()) continue;
            NekoQuery.Neko n = NekoQuery.getNeko(p.getUniqueId());
            if (n != null && n.isNeko()) {
                double d = p.getLocation().distanceSquared(sender.getLocation());
                if (d < nearestDist) {
                    nearestDist = d;
                    nearestNeko = p.getUniqueId();
                }
            }
        }
        if (nearestNeko == null) return;
        sendAIResponse(sender, nearestNeko, rawMessage);
    }

    /** Global chat: trigger AI only if message starts with configured prefix */
    private void processPrefixChatAI(Player sender, String rawMessage) {
        if (!ConfigUtil.isAIEnabled()) return;
        String prefix = ConfigUtil.getAIChatPrefix();
        if (prefix == null || prefix.isEmpty()) return;
        if (!rawMessage.startsWith(prefix)) return;
        String aiMsg = rawMessage.substring(prefix.length()).trim();
        if (aiMsg.isEmpty()) return;
        // Find nearest neko player within 16 blocks
        UUID nearestNeko = findNearestNeko(sender);
        if (nearestNeko == null) return;
        sendAIResponse(sender, nearestNeko, aiMsg);
    }

    private UUID findNearestNeko(Player sender) {
        UUID result = null;
        double best = 256;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == sender) continue;
            if (p.getLocation().getWorld() != sender.getLocation().getWorld()) continue;
            NekoQuery.Neko n = NekoQuery.getNeko(p.getUniqueId());
            if (n != null && n.isNeko()) {
                double d = p.getLocation().distanceSquared(sender.getLocation());
                if (d < best) { best = d; result = p.getUniqueId(); }
            }
        }
        return result;
    }

    /** Send message to AI and deliver reply to sender */
    private void sendAIResponse(Player sender, UUID nekoPlayerUuid, String message) {
        String prompt = ConfigUtil.getAIPrompt();
        Player nekoPlayer = Bukkit.getPlayer(nekoPlayerUuid);
        String nekoName = nekoPlayer != null ? nekoPlayer.getName() : "???";
        String formattedPrompt = prompt
                .replace("%neko_name%", nekoName)
                .replace("%player_name%", sender.getName());
        AIUtil.sendMessage(nekoPlayerUuid, sender.getUniqueId(), formattedPrompt, message, response -> {
            Bukkit.getScheduler().runTask(INSTANCE, () -> {
                String reply = response.getResponse();
                if (reply == null || reply.isEmpty()) return;
                String prefix = nekoName + " §d>> §f";
                sender.sendMessage(Component.text(prefix + reply));
                if (org.cneko.toneko.bukkit.api.ClientStatus.isInstalled(sender)) {
                    PayloadSender.sendChatHistory(sender, nekoPlayerUuid.toString(),
                            List.of("user:" + message, "assistant:" + reply));
                    if (ConfigUtil.isAITTSEnabled()) {
                        PayloadSender.sendTTS(sender, reply);
                    }
                }
            });
        });
    }

    // Matches the mod's Messaging.format() prefix format: [§a前缀§f§r]
    private static final String PREFIX_FORMAT = "[§a%s§f§r]";

    private String format(String message, Player player, String nickname){
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        String chatFormat = ConfigUtil.getChatFormat();

        // Build prefixes (matches mod's getChatPrefixes)
        StringBuilder prefixStr = new StringBuilder();
        if (neko.isNeko()) {
            prefixStr.append(String.format(PREFIX_FORMAT, LanguageUtil.prefix));
        }

        // Nickname with §6~§f wrapper if set (matches mod)
        String name = nickname.isEmpty() ? player.getName() : "§6~§f" + nickname;

        String result = chatFormat
                .replace("%prefix%", prefixStr.toString())
                .replace("%name%", name)
                .replace("%msg%", message)
                .replace("%c%", "§");

        // Area chat mode: add [区域] prefix (matches mod's CommonChatEvent)
        if (ChatModeHolder.isAreaChat(player.getUniqueId())) {
            result = "§a[§f" + translatable("messages.toneko.chat.mode.area") + "§a]§r " + result;
        }
        return result;
    }

    public static void sendMessage(String message){
        Bukkit.getServer().sendMessage(Component.text(message));
        LOGGER.info(message.replaceAll("§[0-9a-fk-or]", ""));
    }

    public static String modify(String message, NekoQuery.Neko neko){
        if (neko != null && neko.isNeko()){
            for (UUID ownerUuid : neko.getOwners()) {
                Player owner = Bukkit.getPlayer(ownerUuid);
                if (owner != null) {
                    message = message.replace(owner.getName(), translatable("misc.toneko.owner"));
                    for (String alias : neko.getAliases().getOrDefault(ownerUuid, List.of())) {
                        message = message.replace(alias, translatable("misc.toneko.owner"));
                    }
                }
            }
        }
        return message;
    }
}
