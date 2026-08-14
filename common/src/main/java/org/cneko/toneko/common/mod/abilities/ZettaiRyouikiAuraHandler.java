package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ZettaiRyouiki;
import org.cneko.toneko.common.util.ConfigUtil;

/**
 * 绝对领域光环：玩家穿着丝袜（领域 B 级及以上）时，周期性对周围敌对生物施加魅惑减速。
 * 纯增益设计（对所有玩家生效，无耗能、无负面）：B/A/S 级用 amplifier 0/1/2 表达强度，
 * S 级彩蛋给玩家自身小幅速度加成。经 LegwearUtil 识别（Fabric+Trinkets 的 legs/socks 槽与盔甲槽均识别）。
 */
public class ZettaiRyouikiAuraHandler {
    private static int tickCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isZettaiRyouikiAuraEnabled()) return;
        int intervalTicks = ConfigUtil.getZettaiRyouikiAuraIntervalTicks();
        if (intervalTicks <= 0 || ++tickCounter % intervalTicks != 0) return;

        int radius = (int) ConfigUtil.getZettaiRyouikiAuraRadius();
        int duration = ConfigUtil.getZettaiRyouikiAuraDurationTicks();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;
            ItemStack legs = LegwearUtil.getWornLegwear(player);
            if (!LegwearItem.isLegwear(legs)) continue;
            // B/A/S 分级；b/c/none/full（连裤袜全覆盖无领域）跳过
            int amplifier = switch (ZettaiRyouiki.compute(legs)) {
                case "s" -> 2;
                case "a" -> 1;
                case "b" -> 0;
                default -> -1;
            };
            if (amplifier < 0) continue;
            // 纯增益：只魅惑敌对生物（Monster 不含猫娘 NPC，不会误伤）
            player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(radius))
                    .forEach(mob -> mob.addEffect(new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.BEWITCHED_EFFECT),
                            duration, amplifier, true, false)));
            // S 级彩蛋：给玩家自身小幅速度加成
            if (amplifier == 2) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, true, false));
            }
        }
    }
}
