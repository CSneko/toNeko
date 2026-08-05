package org.cneko.toneko.neoforge.msic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.cneko.toneko.common.mod.util.ChestLootTableRegistry;

import java.lang.reflect.Field;
import java.util.List;

/**
 * NeoForge 平台的箱子战利品注入入口。
 * 通过 NeoForge 原生的 {@link LootTableLoadEvent} 事件，
 * 将调用委托给 common 模块的 {@link ChestLootTableRegistry}。
 */
public class ChestLootInjection {

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ChestLootInjection::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        LootTable table = event.getTable();

        // 使用临时 Builder 构建本模组的战利品池
        LootTable.Builder builder = LootTable.lootTable();
        ChestLootTableRegistry.addToTable(name, builder);
        LootTable temp = builder.build();

        // 通过反射提取临时表中的 LootPool，添加到 NeoForge 的事件表中
        try {
            Field poolsField = LootTable.class.getDeclaredField("pools");
            poolsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<LootPool> pools = (List<LootPool>) poolsField.get(temp);
            for (LootPool pool : pools) {
                table.addPool(pool);
            }
        } catch (ReflectiveOperationException ignored) {
            // 反射失败则静默跳过，不影响游戏运行
        }
    }
}
