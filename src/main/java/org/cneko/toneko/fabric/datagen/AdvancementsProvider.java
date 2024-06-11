package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.items.ToNekoItems;

import java.util.function.Consumer;

import static org.cneko.toneko.fabric.util.TextUtil.translatable;
import static org.cneko.toneko.common.Bootstrap.MODID;
public class AdvancementsProvider extends FabricAdvancementProvider {
    static AdvancementEntry GOT_NEKO_POTION;
    static AdvancementEntry BECOME_NEKO;


    protected AdvancementsProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<AdvancementEntry> consumer) {
        GOT_NEKO_POTION = Advancement.Builder.create()
                .display(
                        ToNekoItems.NEKO_POTION, // 以猫娘药水作为图标
                        translatable("advancements.toneko.root.title"),
                        translatable("advancements.toneko.root.description"),
                        new Identifier("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.GOAL,
                        true, // 获得时显示在屏幕右上
                        true, // 获得发送到聊天
                        false // 不隐藏进度
                )
                .criterion("got_neko_potion", InventoryChangedCriterion.Conditions.items(ToNekoItems.NEKO_POTION))
                .build(consumer, MODID+"/root");
        /*BECOME_NEKO = Advancement.Builder.create()
                .display(
                        ToNekoItems.NEKO_POTION, // 以猫娘药水作为图标
                        translatable("advancements.toneko.become_neko.title"),
                        translatable("advancements.toneko.become_neko.description"),
                        new Identifier("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.CHALLENGE,
                        true, // 获得时显示在屏幕右上
                        true, // 获得发送到聊天
                        false // 不隐藏进度
                )
                .criterion("become_neko", InventoryChangedCriterion.)
                .parent(GOT_NEKO_POTION)
                .build(consumer, MODID+"/become_neko");*/

    }



}
