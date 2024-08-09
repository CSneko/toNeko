package org.cneko.toneko.neoforge.fabric.events;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.neoforge.fabric.api.PlayerInstallToNeko;
import org.cneko.toneko.neoforge.fabric.util.TextUtil;

public class PlayerConnectionEvents {
    public static void init(){
        ServerPlayConnectionEvents.JOIN.register(PlayerConnectionEvents::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionEvents::onPlayerQuit);
    }

    public static void onPlayerJoin(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        if(NekoQuery.isNeko(player.getUUID())){
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.addPrivatePrefix(name, LanguageUtil.prefix);
        }
    }

    public static void onPlayerQuit(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        if(NekoQuery.isNeko(player.getUUID())){
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.removePrivatePrefix(name, LanguageUtil.prefix);
        }
        PlayerInstallToNeko.remove(TextUtil.getPlayerName(player));
    }


}
