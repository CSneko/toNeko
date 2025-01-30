package org.cneko.toneko.common.mod;

import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.util.scheduled.FabricSchedulerPoolImpl;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

public class ModBootstrap {
    public static void bootstrap() {
        Messaging.GET_PLAYER_UUID_INSTANCE = PlayerUtil::getPlayerUUIDByName;
        SchedulerPoolProvider.INSTANCE = new FabricSchedulerPoolImpl();
    }
}
