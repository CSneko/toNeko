package org.cneko.toneko.fabric.events;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.fabric.util.TextUtil;

public class PlayerConnectionEvents {
    public static void init(){
        ServerPlayConnectionEvents.JOIN.register(PlayerConnectionEvents::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionEvents::onPlayerQuit);
    }

    public static void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
        if(NekoQuery.isNeko(player.getUuid())){
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.addPrivatePrefix(name, LanguageUtil.prefix);
        }
    }

    public static void onPlayerQuit(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer server) {
        ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
        if(NekoQuery.isNeko(player.getUuid())){
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.removePrivatePrefix(name, LanguageUtil.prefix);
        }
    }


}
