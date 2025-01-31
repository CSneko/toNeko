package org.cneko.toneko.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.cneko.toneko.bukkit.commands.NekoCommand;
import org.cneko.toneko.bukkit.commands.ToNekoAdminCommand;
import org.cneko.toneko.bukkit.commands.ToNekoCommand;
import org.cneko.toneko.bukkit.events.ChatEvent;
import org.cneko.toneko.bukkit.events.NetworkingEvents;
import org.cneko.toneko.bukkit.events.PlayerConnectionEvents;
import org.cneko.toneko.bukkit.events.WorldEvents;
import org.cneko.toneko.bukkit.msic.Metrics;
import org.cneko.toneko.bukkit.util.BukkitSchedulerPool;
import org.cneko.toneko.bukkit.util.FoliaSchedulerPoolImpl;
import org.cneko.toneko.bukkit.util.PlaceHolderUtil;
import org.cneko.toneko.bukkit.util.PlayerUtil;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.impl.FabricLanguageImpl;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class ToNeko extends JavaPlugin {
    public static JavaPlugin INSTANCE;
    @Override
    public void onEnable() {
        // common start
        FileUtil.CreatePath("plugins/toNeko");
        ConfigUtil.CONFIG_FILE = "plugins/toNeko/config.yml";
        LanguageUtil.INSTANCE = new FabricLanguageImpl();
        Bootstrap.bootstrap();
        Messaging.GET_PLAYER_UUID_INSTANCE = PlayerUtil::getPlayerUUIDByName;
        // common end

        // scheduler start
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            //Folia platform
            SchedulerPoolProvider.INSTANCE = new FoliaSchedulerPoolImpl();
        }catch (Exception ignored){
            //Non folia platform
            SchedulerPoolProvider.INSTANCE = new BukkitSchedulerPool();
        }
        // scheduler end

        // util start
        INSTANCE = this;
        new Metrics(this, 19899);
        PlaceHolderUtil.init();
        // util end

        // command start
        ToNekoCommand.init();
        ToNekoAdminCommand.init();
        NekoCommand.init();
        //QuirkCommand.init();
        // command end

        // event start
        ChatEvent.init();
        PlayerConnectionEvents.init();
        WorldEvents.init();
        NetworkingEvents.init();
        // event end

        // api event start // TODO 完成
        Messaging.PREFIX_EVENT_INSTANCE = (name, prefix) -> {};
        Messaging.SEND_MESSAGE_INSTANCE = (name, msg, modify) -> {
            ChatEvent.sendMessage(msg);
        };
        Messaging.NEKO_MODIFY_INSTANCE = ChatEvent::modify;
        Messaging.ON_FORMAT_INSTANCE = (message, playerName, nickname, prefix, chatFormat) -> message;
        // api event end
    }

    @Override
    public void onDisable() {
        super.onDisable();
        NekoQuery.NekoData.saveAll();
    }
}
