package org.cneko.toneko.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.fabric.commands.*;
import org.cneko.toneko.fabric.events.ChatEvent;
import org.cneko.toneko.fabric.events.PlayerConnectionEvents;
import org.cneko.toneko.fabric.events.PlayerInteractionEvent;
import org.cneko.toneko.fabric.events.PlayerTickEvent;
import org.cneko.toneko.fabric.impl.FabricConfigImpl;
import org.cneko.toneko.fabric.impl.FabricLanguageImpl;
import org.cneko.toneko.fabric.items.ToNekoArmorMaterials;
import org.cneko.toneko.fabric.items.ToNekoItems;
import org.cneko.toneko.fabric.misc.ToNekoAttributes;
import org.cneko.toneko.fabric.network.packets.EntityPosePayload;
import org.cneko.toneko.fabric.util.PermissionUtil;

public class ToNeko implements ModInitializer {
    @Override
    public void onInitialize() {
        // 初始化语言和配置
        LanguageUtil.INSTANCE = new FabricLanguageImpl();
        ConfigUtil.INSTANCE = new FabricConfigImpl();
        ConfigUtil.preload();
        // 注册命令
        ToNekoCommand.init();
        ToNekoAdminCommand.init();
        NekoCommand.init();
        // QuirkCommand.init();
        // 注册装备
        ToNekoArmorMaterials.init();
        // 注册物品
        ToNekoItems.init();
        TwwdfCommand.init();
        // 注册属性
        ToNekoAttributes.init();
        // 注册网络数据包
        PayloadTypeRegistry.playS2C().register(EntityPosePayload.ID, EntityPosePayload.CODEC);
        // 启动事件
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModMeta.INSTANCE.setServer(server);
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
            // 通用的启动
            Bootstrap.bootstrap();
        });
    }
}
