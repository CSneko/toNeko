package com.crystalneko.tonekofabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.cneko.ctlib.mod.common.util.ModMeta;

import java.io.File;
import java.util.logging.Logger;

public class Meta extends ModMeta{
    private final File dataFolder = new File("ctlib/toneko");
    private final Logger logger = Logger.getLogger("ToNeko");
    private final Description description = new Description();
    private final ServerInfo serverInfo = new ServerInfo();
    private MinecraftServer server;
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public int getPlayerAmount() {
        try {
            if (server != null) {
                return server.getPlayerManager().getPlayerList().size();
            } else {
                // 如果无法获取服务器实例，则返回默认值或者进行其他处理
                return 0;
            }
        }catch (Exception ignored){
            return 0;
        }
    }

    @Override
    public Description getDescription() {
        return description;
    }

    @Override
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
    public class Description extends ModMeta.Description{
        @Override
        public String getName() {
            return "ToNeko";
        }

        @Override
        public String getVersion() {
            String modId = "toneko";
            FabricLoader loader = FabricLoader.getInstance();
            ModContainer mod = loader.getModContainer(modId).orElse(null);
            if (mod != null) {
                return mod.getMetadata().getVersion().getFriendlyString();
            } else {
                return "0.0.1";
            }
        }
    }
    public class ServerInfo extends ModMeta.ServerInfo{

        @Override
        public boolean getOnlineMode() {
            return server.isOnlineMode();
        }

        @Override
        public String getVersion() {
            return server.getVersion();
        }

        @Override
        public String getName() {
            FabricLoader loader = FabricLoader.getInstance();
            return loader.getModContainer("fabric-loader").get().getMetadata().getName();
        }
    }
}
