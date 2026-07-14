package org.cneko.toneko.common.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Data model for neko owner relationships. Used by Bukkit module for chat formatting. */
public class NekoDataModel {
    public static class Owner {
        private final UUID uuid;
        private final List<String> aliases;

        public Owner(UUID uuid, List<String> aliases) {
            this.uuid = uuid;
            this.aliases = aliases != null ? aliases : new ArrayList<>();
        }

        public UUID getUuid() { return uuid; }
        public List<String> getAliases() { return aliases; }
    }
}
