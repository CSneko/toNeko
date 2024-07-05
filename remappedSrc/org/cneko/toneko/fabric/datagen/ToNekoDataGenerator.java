package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class ToNekoDataGenerator implements DataGeneratorEntrypoint {
    public static FabricDataGenerator generator;
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator g) {
        FabricDataGenerator.Pack pack = g.createPack();
        generator = g;
        // 进度生成
        pack.addProvider(AdvancementsProvider::new);
        // 战利品生成
        pack.addProvider(ChestLootTablesProvider::new);
    }
}
