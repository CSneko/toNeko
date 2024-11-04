package org.cneko.toneko.common.mod;

import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.mod.util.PlayerUtil;

public class ModBootstrap {
    public static void bootstrap() {
        Messaging.GET_PLAYER_UUID_INSTANCE = PlayerUtil::getPlayerUUIDByName;
    }
}
