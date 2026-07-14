package org.cneko.toneko.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.cneko.toneko.bukkit.api.ClientStatus;
import org.cneko.toneko.bukkit.util.PayloadSender;
import org.cneko.toneko.common.api.NekoQuery;

/**
 * Manages neko energy regeneration on Bukkit.
 * Replicates the INeko.increaseEnergy() + serverNekoSlowTick() logic.
 */
public class EnergyManager {
    private static final double NEKO_DEGREE_BASE = 0.5; // approximate neko degree on Bukkit

    public static void init() {
        // Regen every 20 ticks (1 second) — matches serverNekoSlowTick
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(ToNeko.INSTANCE, t -> tick(), 1L, 20L);
        // Sync to modded clients every 2 seconds
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(ToNeko.INSTANCE, t -> syncAll(), 40L, 40L);
    }

    private static void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            NekoQuery.Neko neko = NekoQuery.getNeko(p.getUniqueId());
            if (!neko.isNeko()) continue;

            // Tick age (baby grows up over time, matches INeko)
            neko.tickAge();

            // Update max energy: base 1000 + level * 10 (matches INeko.updateNekoLevelModifiers)
            neko.setMaxNekoEnergy(1000f + (float) (neko.getLevel() * 10));

            // Regen if not full
            float energy = neko.getNekoEnergy();
            float max = neko.getMaxNekoEnergy();
            if (energy >= max) continue;

            // Increase: nekoAbility * 0.1.  nekoAbility ≈ level * nekoDegree
            double nekoDegree = NEKO_DEGREE_BASE + neko.getLevel() * 0.02;
            float increase = (float) (neko.getLevel() * nekoDegree * 0.1);

            // Nearby neko bonus (within 3 blocks): + nekoAbility * 0.05 each
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other == p) continue;
                if (!p.getWorld().equals(other.getWorld())) continue;
                if (p.getLocation().distanceSquared(other.getLocation()) > 9) continue; // 3^2
                NekoQuery.Neko on = NekoQuery.getNeko(other.getUniqueId());
                if (on != null && on.isNeko()) {
                    increase += (float) (on.getLevel() * (NEKO_DEGREE_BASE + on.getLevel() * 0.02) * 0.05);
                }
            }

            neko.setNekoEnergy(Math.min(energy + increase, max));
        }
    }

    /** Sync neko info to all online modded clients */
    private static void syncAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            NekoQuery.Neko neko = NekoQuery.getNeko(p.getUniqueId());
            if (neko == null || !neko.isNeko()) continue;
            if (ClientStatus.isInstalled(p)) {
                // Send full info: energy, level factors (approximated from neko level), isNeko, age
                double level = neko.getLevel();
                PayloadSender.sendNekoInfoSync(p, neko.getNekoEnergy(), neko.getMaxNekoEnergy(),
                        level * 0.4, level * 0.2, level * 0.4,
                        true, neko.getNekoAge());
            }
        }
    }
}
