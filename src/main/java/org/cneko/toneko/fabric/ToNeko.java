package org.cneko.toneko.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.fabric.commands.ToNekoCommand;
import org.cneko.toneko.fabric.events.ChatEvent;
import org.cneko.toneko.fabric.events.PlayerConnectionEvents;

public class ToNeko implements ModInitializer {
    @Override
    public void onInitialize() {
        Bootstrap.bootstrap();
        // 注册命令
        ToNekoCommand.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModMeta.instance.setServer(server);
            // 启动聊天监听器
            if(ConfigUtil.CHAT_ENABLE) ChatEvent.init();
            // 启动玩家连接监听器
            PlayerConnectionEvents.init();
        });
    }
}
