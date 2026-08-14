package org.cneko.toneko.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.advencements.*;
import org.cneko.toneko.common.mod.items.ToNekoItems;

import java.util.List;
import java.util.function.Consumer;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
import static org.cneko.toneko.fabric.items.ToNekoItems.NEKO_COLLECTOR;

public class AdvancementsProvider extends FabricAdvancementProvider {

    public static AdvancementHolder NEKO_ATTRACTING;
    public static AdvancementHolder GOT_NEKO_POTION;
    public static AdvancementHolder NEKO_ARMOR;
    public static AdvancementHolder CATNIP;
    public static AdvancementHolder FIRST_GIFT;
    public static AdvancementHolder NEKO_LV100;

    // 入门/能力线
    public static AdvancementHolder BECOME_NEKO;
    public static AdvancementHolder CLIMB_WALL;

    // 社交/繁殖线
    public static AdvancementHolder TAME_NEKO;
    public static AdvancementHolder BREED_NEKO;

    // 特殊
    public static AdvancementHolder NINE_LIVES;

    // 战斗线
    public static AdvancementHolder HISS_FIRST_USE;
    public static AdvancementHolder HISS_COMBO_5;
    public static AdvancementHolder HISS_COMBO_15;
    public static AdvancementHolder EASTER_EGG;

    // 等级里程
    public static AdvancementHolder NEKO_LV10;
    public static AdvancementHolder NEKO_LV50;

    // 丝袜线
    public static AdvancementHolder LEGWEAR_COLLECT;
    public static AdvancementHolder LEGWEAR_FIRST_DYE;
    public static AdvancementHolder LEGWEAR_GRADE_S;

    private static final ResourceLocation BG = ResourceLocation.parse("textures/gui/advancements/backgrounds/adventure.png");

    protected AdvancementsProvider(FabricDataOutput output) {
        super(output, ToNekoDataGenerator.generator.getRegistries());
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
        // =========================================================
        //  🌸 ROOT：猫娘的诱惑
        // =========================================================
        NEKO_ATTRACTING = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_EARS,
                        translatable("advancements.toneko.root.title"),
                        translatable("advancements.toneko.root.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("neko_attracting", InventoryChangeTrigger.TriggerInstance.hasItems(NEKO_COLLECTOR))
                .save(consumer, MODID + "/root");

        // =========================================================
        //  🧪 获得猫娘药水
        // =========================================================
        GOT_NEKO_POTION = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_POTION,
                        translatable("advancements.toneko.got_neko_potion.title"),
                        translatable("advancements.toneko.got_neko_potion.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("got_neko_potion", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.NEKO_POTION))
                .parent(NEKO_ATTRACTING)
                .save(consumer, MODID + "/got_neko_potion");

        // =========================================================
        //  ✨ 变身！猫娘！ —— [新]
        // =========================================================
        BECOME_NEKO = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_POTION,
                        translatable("advancements.toneko.become_neko.title"),
                        translatable("advancements.toneko.become_neko.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("become_neko", NekoBecomeTrigger.TriggerInstance.create())
                .parent(GOT_NEKO_POTION)
                .save(consumer, MODID + "/become_neko");

        // =========================================================
        //  🌿 猫薄荷初体验
        // =========================================================
        CATNIP = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.CATNIP,
                        translatable("advancements.toneko.catnip.title"),
                        translatable("advancements.toneko.catnip.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("catnip", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.CATNIP))
                .parent(GOT_NEKO_POTION)
                .save(consumer, MODID + "/catnip");

        // =========================================================
        //  👗 猫娘换装
        // =========================================================
        NEKO_ARMOR = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_EARS,
                        translatable("advancements.toneko.neko_armor.title"),
                        translatable("advancements.toneko.neko_armor.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("neko_armor", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.NEKO_TAIL, ToNekoItems.NEKO_EARS))
                .parent(NEKO_ATTRACTING)
                .save(consumer, MODID + "/neko_armor");

        // =========================================================
        //  🧦 丝袜收藏家 —— [新]（4 款全收集，AND 语义）
        // =========================================================
        LEGWEAR_COLLECT = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.LEGWEAR_PANTYHOSE_40D,
                        translatable("advancements.toneko.legwear_collect.title"),
                        translatable("advancements.toneko.legwear_collect.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("pantyhose_40d", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.LEGWEAR_PANTYHOSE_40D))
                .addCriterion("pantyhose_20d", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.LEGWEAR_PANTYHOSE_20D))
                .addCriterion("pantyhose_5d", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.LEGWEAR_PANTYHOSE_5D))
                .addCriterion("over_knee", InventoryChangeTrigger.TriggerInstance.hasItems(ToNekoItems.LEGWEAR_OVER_KNEE))
                .requirements(AdvancementRequirements.allOf(List.of("pantyhose_40d", "pantyhose_20d", "pantyhose_5d", "over_knee")))
                .parent(NEKO_ARMOR)
                .save(consumer, MODID + "/legwear_collect");

        // =========================================================
        //  🎨 初染 —— [新]
        // =========================================================
        LEGWEAR_FIRST_DYE = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.LEGWEAR_PANTYHOSE_5D,
                        translatable("advancements.toneko.legwear_first_dye.title"),
                        translatable("advancements.toneko.legwear_first_dye.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("legwear_first_dye", FirstLegwearDyeTrigger.TriggerInstance.create())
                .parent(LEGWEAR_COLLECT)
                .save(consumer, MODID + "/legwear_first_dye");

        // =========================================================
        //  👑 S 级领域 —— [新] CHALLENGE
        // =========================================================
        LEGWEAR_GRADE_S = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.LEGWEAR_OVER_KNEE,
                        translatable("advancements.toneko.legwear_grade_s.title"),
                        translatable("advancements.toneko.legwear_grade_s.description"),
                        BG,
                        AdvancementType.CHALLENGE,
                        true, true, false
                )
                .addCriterion("legwear_grade_s", LegwearGradeSTrigger.TriggerInstance.create())
                .parent(LEGWEAR_FIRST_DYE)
                .save(consumer, MODID + "/legwear_grade_s");

        // =========================================================
        //  🧗 猫爪攀墙 —— [新]
        // =========================================================
        CLIMB_WALL = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_MULTI_TOOL,
                        translatable("advancements.toneko.climb_wall.title"),
                        translatable("advancements.toneko.climb_wall.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("climb_wall", NekoClimbTrigger.TriggerInstance.create())
                .parent(BECOME_NEKO)
                .save(consumer, MODID + "/climb_wall");

        // =========================================================
        //  🎁 第一次送礼
        // =========================================================
        FIRST_GIFT = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.CATNIP_SANDWICH,
                        translatable("advancements.toneko.first_gift.title"),
                        translatable("advancements.toneko.first_gift.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("first_gift", GiftNekoTrigger.TriggerInstance.create())
                .parent(CATNIP)
                .save(consumer, MODID + "/first_gift");

        // =========================================================
        //  📝 契约成立 —— [新]
        // =========================================================
        TAME_NEKO = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.CONTRACT,
                        translatable("advancements.toneko.tame_neko.title"),
                        translatable("advancements.toneko.tame_neko.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("tame_neko", TameNekoTrigger.TriggerInstance.create())
                .parent(FIRST_GIFT)
                .save(consumer, MODID + "/tame_neko");

        // =========================================================
        //  🐣 新生命 —— [新]
        // =========================================================
        BREED_NEKO = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.DEAGE_TREAT,
                        translatable("advancements.toneko.breed_neko.title"),
                        translatable("advancements.toneko.breed_neko.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("breed_neko", NekoBreedTrigger.TriggerInstance.create())
                .parent(TAME_NEKO)
                .save(consumer, MODID + "/breed_neko");

        // =========================================================
        //  🐱 九命猫 —— [新]
        // =========================================================
        NINE_LIVES = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NINE_LIVES_CHARM,
                        translatable("advancements.toneko.nine_lives.title"),
                        translatable("advancements.toneko.nine_lives.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("nine_lives", NineLivesTrigger.TriggerInstance.create())
                .parent(BECOME_NEKO)
                .save(consumer, MODID + "/nine_lives");

        // =========================================================
        //  💥 哈气初心者 —— [新]
        // =========================================================
        HISS_FIRST_USE = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_ENERGY_BURST,
                        translatable("advancements.toneko.hiss_first_use.title"),
                        translatable("advancements.toneko.hiss_first_use.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("hiss_first_use", HissFirstUseTrigger.TriggerInstance.create())
                .parent(BECOME_NEKO)
                .save(consumer, MODID + "/hiss_first_use");

        // =========================================================
        //  🔥 连击大师（5连击） —— [新]
        // =========================================================
        HISS_COMBO_5 = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_ENERGY_BURST,
                        translatable("advancements.toneko.hiss_combo_5.title"),
                        translatable("advancements.toneko.hiss_combo_5.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("hiss_combo_5", HissComboTrigger.TriggerInstance.hasCombo(5))
                .parent(HISS_FIRST_USE)
                .save(consumer, MODID + "/hiss_combo_5");

        // =========================================================
        //  💀 哈气暴走（15连击） —— [新] CHALLENGE
        // =========================================================
        HISS_COMBO_15 = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.EVIL_NEKO_ENERGY_BURST,
                        translatable("advancements.toneko.hiss_combo_15.title"),
                        translatable("advancements.toneko.hiss_combo_15.description"),
                        BG,
                        AdvancementType.CHALLENGE,
                        true, true, false
                )
                .addCriterion("hiss_combo_15", HissComboTrigger.TriggerInstance.hasCombo(15))
                .parent(HISS_COMBO_5)
                .save(consumer, MODID + "/hiss_combo_15");

        // =========================================================
        //  🐱👤 圆头耄耋降临！ —— [新] CHALLENGE + HIDDEN
        // =========================================================
        EASTER_EGG = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_DIAMOND,
                        translatable("advancements.toneko.easter_egg.title"),
                        translatable("advancements.toneko.easter_egg.description"),
                        BG,
                        AdvancementType.CHALLENGE,
                        true, true, true  // <-- hidden
                )
                .addCriterion("easter_egg", EasterEggTrigger.TriggerInstance.create())
                .parent(HISS_FIRST_USE)
                .save(consumer, MODID + "/easter_egg");

        // =========================================================
        //  ⭐ 小有成就（Lv10） —— [新]
        // =========================================================
        NEKO_LV10 = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_INGOT,
                        translatable("advancements.toneko.neko_lv10.title"),
                        translatable("advancements.toneko.neko_lv10.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("neko_lv10", NekoLevelTrigger.TriggerInstance.hasLevel(10))
                .parent(BECOME_NEKO)
                .save(consumer, MODID + "/neko_lv10");

        // =========================================================
        //  ⭐⭐ 猫娘大师（Lv50） —— [新]
        // =========================================================
        NEKO_LV50 = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_DIAMOND,
                        translatable("advancements.toneko.neko_lv50.title"),
                        translatable("advancements.toneko.neko_lv50.description"),
                        BG,
                        AdvancementType.GOAL,
                        true, true, false
                )
                .addCriterion("neko_lv50", NekoLevelTrigger.TriggerInstance.hasLevel(50))
                .parent(NEKO_LV10)
                .save(consumer, MODID + "/neko_lv50");

        // =========================================================
        //  ⭐⭐⭐ 百级猫仙（Lv100） —— [父节点改为 Lv50] CHALLENGE
        // =========================================================
        NEKO_LV100 = Advancement.Builder.advancement()
                .display(
                        ToNekoItems.NEKO_TAIL,
                        translatable("advancements.toneko.neko_lv100.title"),
                        translatable("advancements.toneko.neko_lv100.description"),
                        BG,
                        AdvancementType.CHALLENGE,
                        true, true, false
                )
                .addCriterion("neko_lv100", NekoLevelTrigger.TriggerInstance.hasLevel(100))
                .parent(NEKO_LV50)
                .save(consumer, MODID + "/neko_lv100");
    }
}
