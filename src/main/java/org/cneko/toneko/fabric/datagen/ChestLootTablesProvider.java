package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.items.ToNekoItems;

import java.util.function.BiConsumer;

import static org.cneko.toneko.common.Bootstrap.MODID;
/*
public class ChestLootTablesProvider extends SimpleFabricLootTableProvider {
    public static final Identifier NEKO_CHEST = new Identifier(MODID, "chests/neko_loot");
    public ChestLootTablesProvider(FabricDataOutput dataGenerator) {
        super(dataGenerator, LootContextTypes.CHEST);
    }

    @Override
    public void accept(BiConsumer<Identifier, LootTable.Builder> biConsumer) {
        // 在NekoChest中有 80%的机率出现一个Neko池
        biConsumer.accept(NEKO_CHEST, LootTable.builder()
                .pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(0.8F))
                        // 80% 的机率出现一个NekoPotion
                        .with(ItemEntry.builder(ToNekoItems.NEKO_POTION)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(0.8F))))
                        )
        );
    }
}
*/