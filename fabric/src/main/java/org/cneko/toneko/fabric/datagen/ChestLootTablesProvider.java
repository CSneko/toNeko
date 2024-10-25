package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.cneko.toneko.common.mod.items.ToNekoItems;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ChestLootTablesProvider extends SimpleFabricLootTableProvider {
    public static final ResourceLocation NEKO_CHEST = ResourceLocation.fromNamespaceAndPath(MODID, "chests/neko_loot");
    public ChestLootTablesProvider(FabricDataOutput output) {
        super(output, getWrapperLookup(), LootContextParamSets.CHEST);

    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> lootTableBiConsumer) {
        // 在NekoChest中有 80%的机率出现一个Neko池
        lootTableBiConsumer.accept(ResourceKey.create(Registries.LOOT_TABLE, NEKO_CHEST), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(0.8F))
                        .add(LootItem.lootTableItem(ToNekoItems.NEKO_POTION)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3F))))
                        .add(LootItem.lootTableItem(ToNekoItems.NEKO_EARS)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1F))))
                        .add(LootItem.lootTableItem(ToNekoItems.NEKO_TAIL)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2F))))
                )
        );
    }

    public static CompletableFuture<HolderLookup.Provider> getWrapperLookup() {
        try{
            HolderLookup.Provider wrapperLookup = ToNekoDataGenerator.generator.getRegistries().get();
            // 创建一个已完成的CompletableFuture，其结果是wrapperLookup
            return CompletableFuture.completedFuture(wrapperLookup);
        }catch (Exception e){
            LOGGER.error("Failed to create chest loot tables provider", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
