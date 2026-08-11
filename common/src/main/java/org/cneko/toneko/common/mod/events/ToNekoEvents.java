package org.cneko.toneko.common.mod.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.abilities.ClimbWallHandler;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.api.events.WorldEvents;
import org.cneko.toneko.common.mod.ai.actions.NekoActionExecutor;
import org.cneko.toneko.common.mod.ai.proactive.NekoProactiveManager;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.items.NekoEnergyBatteryItem;
import org.cneko.toneko.common.mod.items.NekoEnergyBurstItem;
import org.cneko.toneko.common.mod.items.NineLivesCharmItem;
import org.cneko.toneko.common.mod.items.GuideBookItem;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.quirks.ModQuirk;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.List;
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
        UseBlockCallback.EVENT.register(CommonPlayerInteractionEvent::useBlock);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CommonPlayerInteractionEvent::onDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof INeko || !(entity instanceof Monster)) return;
            Entity killer = source.getEntity();
            if (killer instanceof INeko neko && neko.isNeko()) {
                double xp = Math.max(1, entity.getMaxHealth() / 2.0);
                NekoLevelRegistry.combat().addRaw(neko, xp);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            // 玩家死亡：附近猫娘（主人优先）触发一次 AI 发言
            if (entity instanceof ServerPlayer player) {
                triggerNekoMourn(player);
            }
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                return NineLivesCharmItem.tryPreventDeath(player);
            }
            return true;
        });
        AttackEntityCallback.EVENT.register(CommonPlayerInteractionEvent::onAttackEntity);
        ServerTickEvents.START_SERVER_TICK.register(CommonPlayerEvent::startTick);
        ServerTickEvents.START_SERVER_TICK.register(NekoEnergyBurstItem::tickComboBossBars);
        ServerTickEvents.START_SERVER_TICK.register(NekoEnergyBatteryItem::dischargeAllPlayers);
        ServerTickEvents.START_SERVER_TICK.register(ClimbWallHandler::onServerTick);
        ServerWorldEvents.UNLOAD.register(CommonWorldEvent::onWorldUnLoad);
        WorldEvents.ON_WEATHER_CHANGE.register(CommonWorldEvent::onWeatherChange);
        EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
            CommonPlayerEvent.startSleep(entity, pos);
            if (entity instanceof INeko neko && neko.isNeko()) {
                NekoLevelRegistry.homestead().addRaw(neko, 50.0);
            }
        });
        EntitySleepEvents.STOP_SLEEPING.register(CommonPlayerEvent::stopSleep);
        NekoMultiToolEvents.init();
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

    /**
     * 玩家死亡时，附近猫娘（该玩家的主人猫娘优先，否则最近）触发一次 AI 发言；
     * 概率开关 ai.proactive.trigger.player_death.chance（0 = 关闭）。
     */
    private static void triggerNekoMourn(ServerPlayer player) {
        float chance = NekoProactiveManager.getChance("player_death");
        if (chance <= 0 || player.getRandom().nextFloat() >= chance) return;
        Level level = player.level();
        // 16 格内猫娘：主人关系优先，其次最近（findNekoEntitiesInRange 含幽灵猫娘）
        List<NekoEntity> nekos = EntityUtil.findNekoEntitiesInRange(player, level, 16);
        NekoEntity chosen = nekos.stream()
                .filter(n -> n.hasOwner(player.getUUID()))
                .findFirst()
                .orElse(nekos.stream().findFirst().orElse(null));
        if (chosen == null) return;

        // 主人死亡 vs 普通玩家死亡：只陈述事实，不写死情绪——说什么由 AI 根据自身人设自由决定；
        // 发言广播到 64 格内（消息无目标）；文案本地化
        boolean isOwner = chosen.hasOwner(player.getUUID());
        String message = LanguageUtil.translatable(isOwner
                        ? "misc.toneko.ai.player_death.owner"
                        : "misc.toneko.ai.player_death.other",
                new Object[]{player.getName().getString()});
        String hintPrefix = LanguageUtil.translatable("misc.toneko.ai.history.hint",
                new Object[]{player.getName().getString()});
        final NekoEntity neko = chosen;

        AIUtil.sendMessage(neko.getAIStorageId(), player.getUUID(),
                neko.generateAIPrompt(player), message, response -> {
            player.getServer().execute(() -> {
                if (neko.isRemoved()) return;
                String displayText = NekoActionExecutor.process(neko, player, response.getResponse());
                Messaging.sendNekoChatInRange(neko, neko, displayText, 64.0);
            });
        }, true, hintPrefix);
    }



    public static void onPlayerJoin(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();

        // 首次进服或丢失手册：自动给予猫猫手册
        boolean hasGuideBook = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (GuideBookItem.isOurGuideBook(player.getInventory().getItem(i))) {
                hasGuideBook = true;
                break;
            }
        }
        if (!hasGuideBook) {
            ItemStack guideBook = GuideBookItem.createGuideBookStack();
            if (!guideBook.isEmpty()) {
                player.getInventory().add(guideBook);
            }
        }

        if(player.isNeko()){
            // 修复quirks
            player.fixQuirks();
            String name = TextUtil.getPlayerName(player);
            for (Quirk quirk : player.getQuirks()){
                if (quirk instanceof ModQuirk mq){
                    mq.onJoin(player);
                }
            }
            // Welcome broadcast
            if (ConfigUtil.isWelcomeMessageEnabled()) {
                String nick = player.getNickName().isEmpty() ? name : player.getNickName();
                String msg = ConfigUtil.getWelcomeMessage().replace("%s", nick);
                server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
            }
        }
    }

    public static void onPlayerQuit(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer server) {
        ServerPlayer player = serverPlayNetworkHandler.getPlayer();
        if(player.isNeko()){
            String name = TextUtil.getPlayerName(player);
        }
    }



}
