package org.cneko.toneko.bukkit;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.cneko.toneko.bukkit.api.ClientStatus;
import org.cneko.toneko.bukkit.commands.NekoCommand;
import org.cneko.toneko.bukkit.commands.ToNekoAdminCommand;
import org.cneko.toneko.bukkit.commands.ToNekoCommand;
import org.cneko.toneko.bukkit.events.ChatEvent;
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
import org.cneko.toneko.common.mod.packets.PluginDetectPayload;
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
        // command end

        // event start
        ChatEvent.init();
        PlayerConnectionEvents.init();
        WorldEvents.init();
        // event end

        this.getServer().getMessenger().registerIncomingPluginChannel(
                this,
                "toneko:detect",
                this::onClientResponse
        );
        this.getServer().getMessenger().registerOutgoingPluginChannel(
                this,
                "toneko:detect"
        );

    }

    @Override
    public void onDisable() {
        super.onDisable();
        NekoQuery.NekoData.saveAll();
    }

    private void onClientResponse(String channel, Player player, byte[] data) {
        if (channel.equalsIgnoreCase("toneko:detect")) {
            ClientStatus.setInstalled(player,true); // 标记玩家已安装模组
        }
    }
}
