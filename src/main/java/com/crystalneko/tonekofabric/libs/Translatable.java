package com.crystalneko.tonekofabric.libs;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.data.DataProvider;

import java.nio.file.Path;

public class Translatable implements DataGeneratorEntrypoint {

    /**
     * 在此入口点期间向 {@link FabricDataGenerator} 注册 {@link DataProvider}。
     *
     * @param fabricDataGenerator {@link FabricDataGenerator} 实例
     */
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(LangProvider::new);
    }

    //----------------------------------------------Mod自带的语言文件-----------------------------------------------------
    private static class LangProvider extends FabricLanguageProvider {

        private LangProvider(FabricDataOutput dataGenerator) {
            // 默认语言为zh_cn
            super(dataGenerator, "zh_cn");
        }


        @Override
        public void generateTranslations(TranslationBuilder translationBuilder) {
            // 从zh_cn.json文件中获取翻译文本并添加到生成的语言文件中
            try {
                Path zhCnFilePath = dataOutput.getModContainer().findPath("assets/tonekofabric/lang/zh_cn.json").get();
                translationBuilder.add(zhCnFilePath);
            } catch (Exception e) {
                throw new RuntimeException("无法加载zh_cn语言", e);
            }
            // 从en_us.json文件中获取翻译文本并添加到生成的语言文件中
            try {
                Path enUsFilePath = dataOutput.getModContainer().findPath("assets/tonekofabric/lang/en_us.json").get();
                translationBuilder.add(enUsFilePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add en_us language file!", e);
            }
        }
    }
}
