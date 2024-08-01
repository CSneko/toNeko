package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.EnchantWithLevelsLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.items.ToNekoItems;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;

public class ChestLootTablesProvider extends SimpleFabricLootTableProvider {
    public static final Identifier NEKO_CHEST = Identifier.of(MODID, "chests/neko_loot");
    public ChestLootTablesProvider(FabricDataOutput output) {
        super(output, getWrapperLookup(), LootContextTypes.CHEST);

    }

    @Override
    public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer) {
        // 在NekoChest中有 80%的机率出现一个Neko池
        lootTableBiConsumer.accept(RegistryKey.of(RegistryKeys.LOOT_TABLE, NEKO_CHEST), LootTable.builder()
                .pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(0.8F))
                        .with(ItemEntry.builder(ToNekoItems.NEKO_POTION)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(3F))))
                        .with(ItemEntry.builder(ToNekoItems.NEKO_EARS)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1F))))
                        .with(ItemEntry.builder(ToNekoItems.NEKO_TAIL)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2F))))
                )
        );
    }

    public static CompletableFuture<RegistryWrapper.WrapperLookup> getWrapperLookup() {
        try{
            RegistryWrapper.WrapperLookup wrapperLookup = ToNekoDataGenerator.generator.getRegistries().get();
            // 创建一个已完成的CompletableFuture，其结果是wrapperLookup
            return CompletableFuture.completedFuture(wrapperLookup);
        }catch (Exception e){
            LOGGER.error("Failed to create chest loot tables provider", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
