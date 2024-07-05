package org.cneko.toneko.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.fabric.commands.NekoCommand;
import org.cneko.toneko.fabric.commands.QuirkCommand;
import org.cneko.toneko.fabric.commands.ToNekoAdminCommand;
import org.cneko.toneko.fabric.commands.ToNekoCommand;
import org.cneko.toneko.fabric.events.ChatEvent;
import org.cneko.toneko.fabric.events.PlayerConnectionEvents;
import org.cneko.toneko.fabric.events.PlayerInteractionEvent;
import org.cneko.toneko.fabric.events.PlayerTickEvent;
import org.cneko.toneko.fabric.items.ToNekoItems;
import org.cneko.toneko.fabric.util.PermissionUtil;

public class ToNeko implements ModInitializer {
    @Override
    public void onInitialize() {
        Bootstrap.bootstrap();
        // 注册命令
        ToNekoCommand.init();
        ToNekoAdminCommand.init();
        NekoCommand.init();
        QuirkCommand.init();
        // 注册物品
        ToNekoItems.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModMeta.instance.setServer(server);
            // 启动聊天监听器
            if(ConfigUtil.CHAT_ENABLE) ChatEvent.init();
            // 启动玩家连接监听器
            PlayerConnectionEvents.init();
            // 启动玩家活动监听器
            PlayerTickEvent.init();
            // 启动右键监听器
            PlayerInteractionEvent.init();
            // 注册权限
            PermissionUtil.init();
        });
    }
}
