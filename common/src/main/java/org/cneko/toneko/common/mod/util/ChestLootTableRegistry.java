package org.cneko.toneko.common.mod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

/**
 * 将 toNeko 模组的物品注入到原版的箱子战利品表中。
 * 此类放在 common 模块中，包含所有战利品定义和添加逻辑。
 * 各平台只需通过 LootTableEvents.MODIFY 事件调用 {@link #addToTable(Identifier, LootTable.Builder)} 即可。
 * <p>
 * 注意：所有物品引用均通过 {@link BuiltInRegistries#ITEM} 运行时查找，
 * 以避免 common 模块中 ToNekoItems 静态字段未被平台模块赋值的问题。
 */
public final class ChestLootTableRegistry {

    private ChestLootTableRegistry() {}

    // ============ 原版箱子战利品表 ID 集合 ============

    private static final Set<Identifier> VILLAGE_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/village/village_plains_house"),
            Identifier.withDefaultNamespace("chests/village/village_desert_house"),
            Identifier.withDefaultNamespace("chests/village/village_savanna_house"),
            Identifier.withDefaultNamespace("chests/village/village_snowy_house"),
            Identifier.withDefaultNamespace("chests/village/village_taiga_house"),
            Identifier.withDefaultNamespace("chests/village/village_armorer"),
            Identifier.withDefaultNamespace("chests/village/village_butcher"),
            Identifier.withDefaultNamespace("chests/village/village_cartographer"),
            Identifier.withDefaultNamespace("chests/village/village_fisher"),
            Identifier.withDefaultNamespace("chests/village/village_fletcher"),
            Identifier.withDefaultNamespace("chests/village/village_mason"),
            Identifier.withDefaultNamespace("chests/village/village_shepherd"),
            Identifier.withDefaultNamespace("chests/village/village_tannery"),
            Identifier.withDefaultNamespace("chests/village/village_temple"),
            Identifier.withDefaultNamespace("chests/village/village_toolsmith"),
            Identifier.withDefaultNamespace("chests/village/village_weaponsmith")
    );

    private static final Set<Identifier> DUNGEON_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/simple_dungeon")
    );

    private static final Set<Identifier> MINESHAFT_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/abandoned_mineshaft")
    );

    private static final Set<Identifier> TREASURE_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/desert_pyramid"),
            Identifier.withDefaultNamespace("chests/jungle_temple"),
            Identifier.withDefaultNamespace("chests/shipwreck_treasure")
    );

    private static final Set<Identifier> STRONGHOLD_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/stronghold_corridor"),
            Identifier.withDefaultNamespace("chests/stronghold_crossing"),
            Identifier.withDefaultNamespace("chests/stronghold_library")
    );

    private static final Set<Identifier> ANCIENT_CITY_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/ancient_city"),
            Identifier.withDefaultNamespace("chests/ancient_city_ice_box")
    );

    private static final Set<Identifier> END_CITY_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/end_city_treasure")
    );

    private static final Set<Identifier> BASTION_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/bastion_treasure"),
            Identifier.withDefaultNamespace("chests/bastion_other"),
            Identifier.withDefaultNamespace("chests/bastion_bridge"),
            Identifier.withDefaultNamespace("chests/bastion_hoglin_stable")
    );

    private static final Set<Identifier> WOODLAND_MANSION_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/woodland_mansion")
    );

    private static final Set<Identifier> PILLAGER_OUTPOST_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/pillager_outpost")
    );

    private static final Set<Identifier> SHIPWRECK_SUPPLY_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/shipwreck_supply")
    );

    private static final Set<Identifier> RUINED_PORTAL_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/ruined_portal")
    );

    private static final Set<Identifier> BURIED_TREASURE_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/buried_treasure")
    );

    private static final Set<Identifier> NETHER_BRIDGE_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/nether_bridge")
    );

    private static final Set<Identifier> IGLOO_CHESTS = Set.of(
            Identifier.withDefaultNamespace("chests/igloo_chest")
    );

    // ============ 辅助：通过注册表运行时查找物品 ============

    /**
     * 通过注册名运行时查找物品。
     * 避免直接引用 common 模块中 ToNekoItems 的静态字段
     * （这些字段在 Fabric/NeoForge 平台模块中会被 shadow，永远为 null）。
     */
    private static Item item(String name) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("toneko", name));
    }

    // ============ 公开入口：由各平台的事件回调调用 ============

    /**
     * 根据战利品表 ID 向其中添加该模组的物品。
     * 由各平台（Fabric/NeoForge）的 LootTableEvents.MODIFY 回调调用。
     *
     * @param tableId 原版战利品表的 Identifier
     * @param builder 战利品表的 Builder
     */
    public static void addToTable(Identifier tableId, LootTable.Builder builder) {
        if (VILLAGE_CHESTS.contains(tableId)) {
            addVillageLoot(builder);
        } else if (DUNGEON_CHESTS.contains(tableId)) {
            addDungeonLoot(builder);
        } else if (MINESHAFT_CHESTS.contains(tableId)) {
            addMineshaftLoot(builder);
        } else if (TREASURE_CHESTS.contains(tableId)) {
            addTreasureLoot(builder);
        } else if (STRONGHOLD_CHESTS.contains(tableId)) {
            addStrongholdLoot(builder);
        } else if (ANCIENT_CITY_CHESTS.contains(tableId)) {
            addAncientCityLoot(builder);
        } else if (END_CITY_CHESTS.contains(tableId)) {
            addEndCityLoot(builder);
        } else if (BASTION_CHESTS.contains(tableId)) {
            addBastionLoot(builder);
        } else if (WOODLAND_MANSION_CHESTS.contains(tableId)) {
            addWoodlandMansionLoot(builder);
        } else if (PILLAGER_OUTPOST_CHESTS.contains(tableId)) {
            addPillagerOutpostLoot(builder);
        } else if (SHIPWRECK_SUPPLY_CHESTS.contains(tableId)) {
            addShipwreckSupplyLoot(builder);
        } else if (RUINED_PORTAL_CHESTS.contains(tableId)) {
            addRuinedPortalLoot(builder);
        } else if (BURIED_TREASURE_CHESTS.contains(tableId)) {
            addBuriedTreasureLoot(builder);
        } else if (NETHER_BRIDGE_CHESTS.contains(tableId)) {
            addNetherBridgeLoot(builder);
        } else if (IGLOO_CHESTS.contains(tableId)) {
            addIglooLoot(builder);
        }
    }

    // ============ 各箱子的战利品添加方法 ============

    private static void addVillageLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("catnip_seed"))
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .add(LootItem.lootTableItem(item("catnip"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_potion"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addDungeonLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.6F))
                .add(LootItem.lootTableItem(item("neko_potion"))
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .add(LootItem.lootTableItem(item("neko_ears"))
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_tail"))
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_paws"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("catnip_sandwich"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_energy_storage_small"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("music_disc_kawaii"))
                        .setWeight(1)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addMineshaftLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("neko_ingot"))
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .add(LootItem.lootTableItem(item("neko_potion"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("catnip"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
        );
    }

    private static void addTreasureLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("neko_diamond"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_crystal"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_energy_burst"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
        );
    }

    private static void addStrongholdLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.4F))
                .add(LootItem.lootTableItem(item("bazooka"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("gene_editor"))
                        .setWeight(1)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("contract"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("music_disc_kawaii"))
                        .setWeight(1)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_energy_storage_medium"))
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("plot_scroll"))
                        .setWeight(2)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addAncientCityLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.35F))
                .add(LootItem.lootTableItem(item("infinite_catnip"))
                        .setWeight(2)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_energy_storage_large_charged"))
                        .setWeight(2)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("furry_bohe"))
                        .setWeight(1)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("gene_editor"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addEndCityLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.4F))
                .add(LootItem.lootTableItem(item("gene_editor"))
                        .setWeight(2)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_diamond"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .add(LootItem.lootTableItem(item("neko_energy_storage_large"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addBastionLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.45F))
                .add(LootItem.lootTableItem(item("furry_bohe"))
                        .setWeight(2)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("bazooka"))
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_energy_storage_large"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_crystal"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
        );
    }

    private static void addWoodlandMansionLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("contract"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_collector"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_ears"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_tail"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addPillagerOutpostLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("bazooka"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("neko_crystal"))
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_potion"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
        );
    }

    private static void addShipwreckSupplyLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("neko_potion"))
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("catnip"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_ingot"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addRuinedPortalLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("neko_ingot"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .add(LootItem.lootTableItem(item("neko_crystal"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
        );
    }

    private static void addBuriedTreasureLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.4F))
                .add(LootItem.lootTableItem(item("neko_diamond"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_crystal"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        );
    }

    private static void addNetherBridgeLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.4F))
                .add(LootItem.lootTableItem(item("neko_crystal"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .add(LootItem.lootTableItem(item("neko_ingot"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
        );
    }

    private static void addIglooLoot(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(0.5F))
                .add(LootItem.lootTableItem(item("neko_potion"))
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(item("catnip"))
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
        );
    }
}
