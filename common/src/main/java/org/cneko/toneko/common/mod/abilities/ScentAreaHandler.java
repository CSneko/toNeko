package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.misc.CauldronSpoilageData;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 变质水气味云 + 炼药锅静态气味源：
 * 喷溅/滞留水落地后形成限时气味区域；装满变质水的炼药锅本身也会持续影响周围生物。
 * 每 1 秒扫描一次，范围与吸引力随变质程度线性缩放。
 */
public class ScentAreaHandler {
    private record AreaKey(ResourceKey<Level> dimension, BlockPos pos) {}
    private record Area(int spoilage, String wearer, long expireTick, boolean lingering) {}

    private static final Map<AreaKey, Area> AREAS = new ConcurrentHashMap<>();
    private static int scanCounter = 0;

    public static void createArea(ServerLevel level, BlockPos pos, int spoilage, String wearer, boolean lingering) {
        if (spoilage <= 0) return;
        long duration = lingering ? 600L : 200L; // 滞留 30s，喷溅 10s
        AREAS.put(new AreaKey(level.dimension(), pos.immutable()),
                new Area(spoilage, wearer, level.getGameTime() + duration, lingering));
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.4, 0.4, 0.4, 0.02);
    }

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isScentEnabled()) return;
        if (++scanCounter % 20 != 0) return; // 每秒扫描

        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            // 限时气味云
            AREAS.entrySet().removeIf(entry -> {
                if (!entry.getKey().dimension().equals(level.dimension())) return false;
                Area area = entry.getValue();
                if (area.expireTick() <= now) return true;
                if (area.spoilage() <= 0) return true;
                applyAreaEffects(level, entry.getKey().pos(), area.spoilage());
                return false;
            });

            // 炼药锅静态气味源
            if (!ConfigUtil.isScentCauldronEnabled()) continue;
            CauldronSpoilageData data = CauldronSpoilageData.get(level);
            for (Map.Entry<BlockPos, CauldronSpoilageData.SpoiledWater> entry : data.getSpoilageMap().entrySet()) {
                BlockPos pos = entry.getKey();
                int spoilage = entry.getValue().level();
                if (spoilage <= 0) continue;
                var state = level.getBlockState(pos);
                if (!state.is(Blocks.WATER_CAULDRON) || state.getValue(LayeredCauldronBlock.LEVEL) <= 0) continue;
                applyAreaEffects(level, pos, spoilage);
            }
        }
    }

    private static void applyAreaEffects(ServerLevel level, BlockPos pos, int spoilage) {
        double radius = scentRadius(spoilage);
        AABB aabb = new AABB(pos).inflate(radius);

        // 吸引：亡灵（浓郁）——没有目标时朝气源移动
        if (spoilage >= 80 && ConfigUtil.isScentZombieAttractEnabled()) {
            for (Zombie z : level.getEntitiesOfClass(Zombie.class, aabb, e -> e.getTarget() == null && e.isAlive())) {
                z.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
            }
        }

        // 吸引：猫 / 豹猫（轻微，好奇靠近）
        if (ConfigUtil.isScentCatAttractEnabled()) {
            for (PathfinderMob c : level.getEntitiesOfClass(PathfinderMob.class, aabb,
                    e -> (e instanceof Cat || e instanceof Ocelot) && e.isAlive())) {
                if (c.getNavigation().isDone() && c.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 4) {
                    c.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.6);
                }
                c.getLookControl().setLookAt(pos.getX(), pos.getY(), pos.getZ(), 30.0f, 30.0f);
            }
        }

        // 驱赶：家畜（明显）
        if (spoilage >= 40 && ConfigUtil.isScentAnimalRepelEnabled()) {
            flee(level, pos, radius, e -> e instanceof Animal && !(e instanceof Cat) && !(e instanceof Ocelot));
        }
        // 驱赶：村民（浓郁）
        if (spoilage >= 60 && ConfigUtil.isScentVillagerRepelEnabled()) {
            flee(level, pos, radius, e -> e instanceof Villager);
        }
        // 驱赶：昆虫（难以忽视）
        if (spoilage >= 80 && ConfigUtil.isScentSpiderRepelEnabled()) {
            flee(level, pos, radius, e -> e instanceof Spider || e instanceof CaveSpider || e instanceof Silverfish);
        }
    }

    private static double scentRadius(int spoilage) {
        return 8.0 + (spoilage / 100.0) * 40.0;
    }

    private static void flee(ServerLevel level, BlockPos pos, double radius, Predicate<Entity> matcher) {
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, new AABB(pos).inflate(radius), matcher)) {
            if (!mob.isAlive()) continue;
            double dx = mob.getX() - pos.getX();
            double dz = mob.getZ() - pos.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.01) { dx = 1; dz = 0; len = 1; }
            double fx = mob.getX() + dx / len * 8.0;
            double fz = mob.getZ() + dz / len * 8.0;
            mob.getNavigation().moveTo(fx, mob.getY(), fz, 1.1);
        }
    }
}
