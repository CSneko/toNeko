package org.cneko.toneko.common.mod.abilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.function.Predicate;

/**
 * 气味生态：气味分档吸引/驱赶。
 * 轻微=猫好奇靠近；明显=家畜避开；浓郁=村民嫌弃 + 亡灵寻味而来；难以忽视=昆虫退避。
 * （狼犬追踪由 ScentWolfTrackingHandler 处理）
 */
public class ScentEcologyHandler {
    private static int scanCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (++scanCounter % 20 != 0) return; // 每秒扫描

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;
            int scent = ScentUtil.getIntensity(LegwearUtil.getWornLegwear(player));
            if (scent <= 0) continue;

            ServerLevel level = (ServerLevel) player.level();

            // 吸引：亡灵（僵尸/尸壳/溺尸/僵尸村民，浓郁）
            if (scent >= 80 && ConfigUtil.isScentZombieAttractEnabled()) {
                for (Zombie z : level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(scentRadius(scent)),
                        e -> e.getTarget() == null && e.isAlive())) {
                    z.setTarget(player);
                }
            }

            // 吸引：猫 / 豹猫（轻微，好奇靠近）
            if (ConfigUtil.isScentCatAttractEnabled()) {
                for (PathfinderMob c : level.getEntitiesOfClass(PathfinderMob.class, player.getBoundingBox().inflate(scentRadius(scent)),
                        e -> (e instanceof Cat || e instanceof Ocelot) && e.isAlive())) {
                    if (c.getNavigation().isDone() && c.distanceToSqr(player) > 4) {
                        c.getNavigation().moveTo(player, 0.6);
                    }
                    c.getLookControl().setLookAt(player, 30.0f, 30.0f);
                }
            }

            // 驱赶：家畜（明显）
            if (scent >= 40 && ConfigUtil.isScentAnimalRepelEnabled()) {
                flee(level, player, scentRadius(scent), e -> e instanceof Animal && !(e instanceof Cat) && !(e instanceof Ocelot));
            }
            // 驱赶：村民（浓郁）
            if (scent >= 60 && ConfigUtil.isScentVillagerRepelEnabled()) {
                flee(level, player, scentRadius(scent), e -> e instanceof Villager);
            }
            // 驱赶：昆虫（蜘蛛/洞穴蜘蛛/蠹虫，难以忽视）
            if (scent >= 80 && ConfigUtil.isScentSpiderRepelEnabled()) {
                flee(level, player, scentRadius(scent), e -> e instanceof Spider || e instanceof CaveSpider || e instanceof Silverfish);
            }
        }
    }

    /** 气味半径：随强度在 8~48 格间线性缩放 */
    private static double scentRadius(int scent) {
        return 8.0 + (scent / 100.0) * 40.0;
    }

    /** 让范围内匹配的生物远离玩家 */
    private static void flee(ServerLevel level, ServerPlayer player, double radius, Predicate<Entity> matcher) {
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, player.getBoundingBox().inflate(radius), matcher)) {
            if (!mob.isAlive()) continue;
            double dx = mob.getX() - player.getX();
            double dz = mob.getZ() - player.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.01) { dx = 1; dz = 0; len = 1; }
            double fx = mob.getX() + dx / len * 8.0;
            double fz = mob.getZ() + dz / len * 8.0;
            mob.getNavigation().moveTo(fx, mob.getY(), fz, 1.1);
        }
    }
}
