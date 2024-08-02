package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.items.ToNekoItems;

import java.util.function.Consumer;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.fabric.util.TextUtil.translatable;
public class AdvancementsProvider extends FabricAdvancementProvider {
    public static AdvancementEntry NEKO_ATTRACTING;
    public static AdvancementEntry GOT_NEKO_POTION;
    public static AdvancementEntry NEKO_ARMOR;

    protected AdvancementsProvider(FabricDataOutput output) {
        super(output,ToNekoDataGenerator.generator.getRegistries());
    }


    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> consumer) {
        NEKO_ATTRACTING = Advancement.Builder.create()
                .display(
                        ToNekoItems.NEKO_EARS, // 以猫耳朵作为图标
                        translatable("advancements.toneko.root.title"),
                        translatable("advancements.toneko.root.description"),
                        Identifier.of("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.GOAL,
                        true, // 获得时显示在屏幕右上
                        true, // 获得发送到聊天
                        false // 不隐藏进度
                )
                .criterion("neko_attracting", InventoryChangedCriterion.Conditions.items(ToNekoItems.NEKO_COLLECTOR))
                .build(consumer, MODID+"/root");
        GOT_NEKO_POTION = Advancement.Builder.create()
                .display(
                        ToNekoItems.NEKO_POTION, // 以猫娘药水作为图标
                        translatable("advancements.toneko.got_neko_potion.title"),
                        translatable("advancements.toneko.got_neko_potion.description"),
                        Identifier.of("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.CHALLENGE,
                        true, // 获得时显示在屏幕右上
                        true, // 获得发送到聊天
                        false // 不隐藏进度
                )
                .criterion("got_neko_potion", InventoryChangedCriterion.Conditions.items(ToNekoItems.NEKO_POTION))
                .parent(NEKO_ATTRACTING)
                .build(consumer, MODID+"/got_neko_potion");
        NEKO_ARMOR = Advancement.Builder.create()
                .display(
                        ToNekoItems.NEKO_TAIL,
                        translatable("advancements.toneko.neko_armor.title"),
                        translatable("advancements.toneko.neko_armor.description"),
                        Identifier.of("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.CHALLENGE,
                        true, // 获得时显示在屏幕右上
                        true, // 获得发送到聊天
                        false // 不隐藏进度
                )
                .criterion("neko_armor",InventoryChangedCriterion.Conditions.items(ToNekoItems.NEKO_TAIL,ToNekoItems.NEKO_EARS))
                .build(consumer,MODID+"/neko_armor");

    }
}
