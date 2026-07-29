package org.cneko.toneko.fabric.msic;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import org.cneko.toneko.common.mod.util.ChestLootTableRegistry;

/**
 * Fabric 平台的箱子战利品注入入口。
 * 通过 Fabric Loot API v3 的 LootTableEvents.MODIFY 事件，
 * 将调用委托给 common 模块的 {@link ChestLootTableRegistry}。
 */
public class ChestLootInjection {

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, holder) -> {
            if (source.isBuiltin()) {
                ChestLootTableRegistry.addToTable(key.location(), tableBuilder);
            }
        });
    }
}
