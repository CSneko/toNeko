package org.cneko.toneko.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.ModMeta;
import org.cneko.toneko.common.mod.commands.NekoCommand;
import org.cneko.toneko.common.mod.commands.QuirkCommand;
import org.cneko.toneko.common.mod.commands.ToNekoAdminCommand;
import org.cneko.toneko.common.mod.commands.ToNekoCommand;
import org.cneko.toneko.common.mod.events.ToNekoEvents;
import org.cneko.toneko.common.mod.events.ToNekoNetworkEvents;
import org.cneko.toneko.common.mod.impl.FabricConfigImpl;
import org.cneko.toneko.common.mod.impl.FabricLanguageImpl;
import org.cneko.toneko.common.mod.items.ToNekoArmorMaterials;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.packets.ToNekoPackets;
import org.cneko.toneko.common.mod.packets.VehicleStopRidePayload;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.quirks.ToNekoQuirks;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.fabric.items.ToNekoItems;
import org.cneko.toneko.fabric.misc.ToNekoAttributes;

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
        QuirkCommand.init();

        // 注册装备
        ToNekoArmorMaterials.init();
        // 注册物品
        ToNekoItems.init();
        // 注册属性
        ToNekoAttributes.init();

        // 注册Quirks
        ToNekoQuirks.init();
        // 注册网络数据包
        ToNekoPackets.init();

        // 注册实体
        ToNekoEntities.init();

        // 启动事件
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModMeta.INSTANCE.setServer(server);
            // 启动监Event
            ToNekoEvents.init();
            ToNekoNetworkEvents.init();
            // 注册权限
            PermissionUtil.init();
            // 通用的启动
            Bootstrap.bootstrap();
        });
    }
}
