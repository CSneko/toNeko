package org.cneko.toneko.bukkit.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.cneko.toneko.bukkit.ToNeko;
import org.cneko.toneko.common.api.NekoQuery;

import java.util.*;

import java.util.List;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;

/**
 * Bukkit equivalents of NekoCollectorItem and NekoPotionItem.
 * - Collector: charges up near neko players, produces potion at 5000 progress
 * - Potion: drink to become neko (+100 energy if already neko)
 */
public class NekoItems implements Listener {
    private static final NamespacedKey COLLECTOR_KEY = new NamespacedKey(INSTANCE, "neko_collector");
    private static final NamespacedKey CATNIP_KEY = new NamespacedKey(INSTANCE, "catnip");
    private static final NamespacedKey CATNIP_SEED_KEY = new NamespacedKey(INSTANCE, "catnip_seed");
    private static final NamespacedKey POTION_KEY = new NamespacedKey(INSTANCE, "neko_potion");
    private static final NamespacedKey PROGRESS_KEY = new NamespacedKey(INSTANCE, "neko_progress");
    private static final float MAX_PROGRESS = 5000f;
    private static final Set<Location> CATNIP_CROPS = Collections.synchronizedSet(new HashSet<>());

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new NekoItems(), ToNeko.INSTANCE);
        // Tick collectors in inventory every 2 seconds
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(INSTANCE, t -> tickCollectors(), 40L, 40L);
    }

    // === Item creation ===

    public static void giveCollector(Player player) {
        ItemStack item = CraftEngineIntegration.createItem("neko_collector", createCollector(0), player);
        player.getInventory().addItem(item);
    }

    public static void giveCatnip(Player player) {
        ItemStack item = CraftEngineIntegration.createItem("catnip", createCatnipItem(), player);
        player.getInventory().addItem(item);
    }

    public static void giveCatnipSeed(Player player) {
        ItemStack item = CraftEngineIntegration.createItem("catnip_seed", createCatnipSeedItem(), player);
        player.getInventory().addItem(item);
    }

    public static void givePotion(Player player) {
        ItemStack item = CraftEngineIntegration.createItem("neko_potion", createPotionItem(), player);
        player.getInventory().addItem(item);
    }

    private static ItemStack createPotionItem() {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§5猫娘药水 §7(Neko Potion)"));
        meta.getPersistentDataContainer().set(POTION_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCatnipItem() {
        ItemStack item = new ItemStack(Material.GREEN_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§a猫薄荷 §7(Catnip)"));
        meta.getPersistentDataContainer().set(CATNIP_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCatnipSeedItem() {
        ItemStack item = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§a猫薄荷种子"));
        meta.getPersistentDataContainer().set(CATNIP_SEED_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCollector(float progress) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§d猫猫收集器 §7(Neko Collector)"));
        meta.getPersistentDataContainer().set(COLLECTOR_KEY, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(PROGRESS_KEY, PersistentDataType.FLOAT, progress);
        // lore shows progress
        updateCollectorLore(meta, progress);
        item.setItemMeta(meta);
        return item;
    }

    private static void updateCollectorLore(ItemMeta meta, float progress) {
        meta.lore(List.of(Component.text(String.format("§a收集进度: §f%.0f / %.0f", progress, MAX_PROGRESS))));
    }

    // === Collector tick ===

    private static void tickCollectors() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (NekoQuery.isNeko(p.getUniqueId())) continue; // nekos don't use collector
            for (int i = 0; i < p.getInventory().getSize(); i++) {
                ItemStack item = p.getInventory().getItem(i);
                if (item == null || !item.hasItemMeta()) continue;
                ItemMeta meta = item.getItemMeta();
                if (!meta.getPersistentDataContainer().has(COLLECTOR_KEY)) continue;

                // Count neko players within 3 blocks
                int catAbility = 0;
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other == p) continue;
                    if (!p.getWorld().equals(other.getWorld())) continue;
                    if (p.getLocation().distanceSquared(other.getLocation()) > 9) continue; // 3^2
                    NekoQuery.Neko n = NekoQuery.getNeko(other.getUniqueId());
                    if (n.isNeko()) {
                        // Approximate neko ability: level * 0.5
                        catAbility += (int) (n.getLevel() * 0.5);
                    }
                }
                if (catAbility == 0) continue;

                // Accumulate progress
                float oldProgress = meta.getPersistentDataContainer().getOrDefault(PROGRESS_KEY, PersistentDataType.FLOAT, 0f);
                float nekoDegreeBonus = 1.0f; // Bukkit approximate
                float newProgress = oldProgress + (catAbility / 100f) * nekoDegreeBonus;

                if (newProgress >= MAX_PROGRESS) {
                    // Reset and drop potion
                    meta.getPersistentDataContainer().set(PROGRESS_KEY, PersistentDataType.FLOAT, 0f);
                    updateCollectorLore(meta, 0);
                    item.setItemMeta(meta);
                    p.getInventory().setItem(i, item);
                    p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(Material.POTION) {{
                        ItemMeta m = getItemMeta();
                        m.displayName(Component.text("§5猫娘药水 §7(Neko Potion)"));
                        m.getPersistentDataContainer().set(POTION_KEY, PersistentDataType.BOOLEAN, true);
                        setItemMeta(m);
                    }});
                } else {
                    meta.getPersistentDataContainer().set(PROGRESS_KEY, PersistentDataType.FLOAT, newProgress);
                    updateCollectorLore(meta, newProgress);
                    item.setItemMeta(meta);
                    p.getInventory().setItem(i, item); // write back to inventory
                }
            }
        }
    }

    // === Use catnip ===

    @EventHandler
    public void onUseCatnip(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(CATNIP_KEY)) return;

        event.setCancelled(true);
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if (neko.isNeko()) {
            neko.setNekoEnergy(neko.getNekoEnergy() + 30f);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 200, 0));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NAUSEA, 200, 0));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0);
            player.sendMessage(Component.text("§a喵~ 能量恢复了30点！"));
        }
        // Consume exactly 1
        item.setAmount(item.getAmount() - 1);
    }

    // === Catnip crop ===

    @EventHandler
    public void onPlantSeed(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(CATNIP_SEED_KEY)) {
            CATNIP_CROPS.add(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onHarvestCrop(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!CATNIP_CROPS.contains(block.getLocation())) return;
        if (block.getType() != Material.WHEAT) return;
        event.setDropItems(false); // always suppress vanilla drops
        CATNIP_CROPS.remove(block.getLocation());

        if (block.getBlockData() instanceof Ageable age && age.getAge() >= age.getMaximumAge()) {
            // Fully grown: drop catnip + seeds
            block.getWorld().dropItemNaturally(block.getLocation(), createCatnipItem());
            block.getWorld().dropItemNaturally(block.getLocation(), createCatnipSeedItem());
            if (Math.random() < 0.7) {
                block.getWorld().dropItemNaturally(block.getLocation(), createCatnipSeedItem());
            }
        } else {
            // Not grown: drop seeds only
            block.getWorld().dropItemNaturally(block.getLocation(), createCatnipSeedItem());
        }
    }

    // === Drink potion ===

    @EventHandler
    public void onUsePotion(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(POTION_KEY)) return;

        event.setCancelled(true);
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());

        if (neko.isNeko()) {
            // Already neko: restore 100 energy (matches mod)
            neko.setNekoEnergy(neko.getNekoEnergy() + 100);
            player.sendMessage(Component.text("§a能量恢复了100点！"));
        } else {
            // Become neko
            NekoQuery.setNeko(player.getUniqueId(), true);
            // Title effect (matches mod)
            player.showTitle(Title.title(
                    Component.text("§d哼!哼!喵喵喵喵!"),
                    Component.text("§7你已变成猫娘！")
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        item.setAmount(item.getAmount() - 1);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
    }
}
