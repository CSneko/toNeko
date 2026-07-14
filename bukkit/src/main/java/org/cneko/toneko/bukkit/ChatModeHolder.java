package org.cneko.toneko.bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local chat mode storage to avoid importing ToNekoNetworkEvents (which references NekoEntity).
 * Updated by ChatModePayload handler in NetworkingEvents.
 */
public class ChatModeHolder {
    private static final Map<UUID, Boolean> MODES = new ConcurrentHashMap<>();

    public static void setAreaChat(UUID player, boolean area) {
        MODES.put(player, area);
    }

    public static boolean isAreaChat(UUID player) {
        return MODES.getOrDefault(player, false);
    }
}
