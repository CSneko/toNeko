package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.api.events.WorldEvents;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.quirks.ModQuirk;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.quirks.Quirk;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

public class ToNekoEvents {
    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (ConfigUtil.isChatEnable()) {
                CommonChatEvent.onChatMessage(message, sender, params);
                return false;
            }
                return true;
        });
        ServerPlayConnectionEvents.JOIN.register(ToNekoEvents::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(ToNekoEvents::onPlayerQuit);
        UseEntityCallback.EVENT.register(CommonPlayerInteractionEvent::useEntity);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CommonPlayerInteractionEvent::onDamage);
        AttackEntityCallback.EVENT.register(CommonPlayerInteractionEvent::onAttackEntity);
        ServerTickEvents.START_SERVER_TICK.register(CommonPlayerEvent::startTick);
        ServerWorldEvents.UNLOAD.register(CommonWorldEvent::onWorldUnLoad);
        WorldEvents.ON_WEATHER_CHANGE.register(CommonWorldEvent::onWeatherChange);
        EntitySleepEvents.START_SLEEPING.register(CommonPlayerEvent::startSleep);
        EntitySleepEvents.STOP_SLEEPING.register(CommonPlayerEvent::stopSleep);
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER,
                1,
                (factories) -> factories.add((trader, random) -> new MerchantOffer(
                        new ItemCost(Items.EMERALD, 2),
                        ToNekoItems.CATNIP_SEED.getDefaultInstance(),
                        10,
                        10,
                        1.1f
                ))
        );

    }



    public static void onPlayerJoin(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        if(neko.isNeko()){
            // 修复quirks
            neko.fixQuirks();
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.addPrivatePrefix(name, LanguageUtil.prefix);
            for (Quirk quirk : neko.getQuirks()){
                if (quirk instanceof ModQuirk mq){
                    mq.onJoin(player);
                }
            }
        }
    }

    public static void onPlayerQuit(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUUID());
        if(neko.isNeko()){
            String name = TextUtil.getPlayerName(player);
            ChatPrefix.removePrivatePrefix(name, LanguageUtil.prefix);
        }
        // 保存猫娘数据
        NekoQuery.NekoData.saveAndRemoveNeko(player.getUUID());
    }



}
