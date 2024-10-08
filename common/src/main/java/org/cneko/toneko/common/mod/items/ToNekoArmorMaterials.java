package org.cneko.toneko.common.mod.items;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoArmorMaterials {
    public static ArmorMaterial NEKO = new NekoArmorMaterial();
    public static void init(){
        // 如果启用了仅服务器端，则不注册物品
        //registerWithOutConfig();
    }

//    /**
//     * 强制注册物品，无论配置文件如何设置
//     */
//    public static void registerWithOutConfig() {
//        NEKO = register(
//                "neko_tail",
//                Map.of(
//                        ArmorItem.Type.LEGGINGS, 0 // 猫尾巴要啥防御点呀
//                ),
//                15, // 嗯...还是让你们可以附魔吧
//                Holder.direct(SoundEvents.CAT_AMBIENT), // 喵喵喵~
//                () -> Ingredient.of(TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("c","wool"))), //wooooooooool
//                0.5F, // 猫尾巴可以吸收什么伤害呢
//                0.5F, // 猫尾巴还能抵御击退吗?肯定不能啦
//                true // 猫尾巴可以染色吗?当然可以啦,但是现在技术还不够呢,求原谅
//        );
//
//
//    }
//
//    /**
//     *  注册一个新的盔甲
//     * @param id 盔甲id
//     * @param defensePoints 防御点
//     * @param enchantability 附魔能力
//     * @param equipSound 音效
//     * @param repairIngredientSupplier 修复原料
//     * @param toughness 盔甲会吸收多少伤害
//     * @param knockbackResistance 击退抗性
//     * @param dyeable 是否可染色
//     * @return 盔甲材料
//     */
//    public static Holder<ArmorMaterial> register(
//            String id, Map<ArmorItem.Type, Integer> defensePoints,
//            int enchantability, Holder<SoundEvent> equipSound,
//            Supplier<Ingredient> repairIngredientSupplier,
//            float toughness,
//            float knockbackResistance,
//            boolean dyeable) {
//        // Get the supported layers for the armor material
//        List<ArmorMaterial.Layer> layers = List.of(
//                // 纹理层的 ID、后缀以及该层是否可染色。
//                // 我们可以将盔甲材质 ID 作为纹理层 ID 传递。
//                // 我们不需要后缀，因此我们将传递一个空字符串。
//                // 我们将传递收到的可染色布尔值作为可染色参数。
//                new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, id), "", dyeable)
//        );
//
//        ArmorMaterial material = new ArmorMaterial(defensePoints, enchantability, equipSound, repairIngredientSupplier, layers, toughness, knockbackResistance);
//        // Register the material within the ArmorMaterials registry.
//        material = Registry.register(BuiltInRegistries.ARMOR_MATERIAL, ResourceLocation.fromNamespaceAndPath(MODID, id), material);
//
//        // 大多数时候，您会需要材质的 RegistryEntry - 尤其是对于 ArmorItem 构造函数。
//        return Holder.direct(material);
//    }

    public static class NekoArmorMaterial implements ArmorMaterial {

        @Override
        public int getDurabilityForType(ArmorItem.@NotNull Type type) {
            return 20;
        }

        @Override
        public int getDefenseForType(ArmorItem.@NotNull Type type) {
            return 2;
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public @NotNull SoundEvent getEquipSound() {
            return SoundEvents.CAT_AMBIENT;
        }

        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return Ingredient.of(TagKey.create(BuiltInRegistries.ITEM.key(), new ResourceLocation("c","wool")));
        }

        @Override
        public String getName() {
            return "neko_armor";
        }

        @Override
        public float getToughness() {
            return 0.5f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    }

}
