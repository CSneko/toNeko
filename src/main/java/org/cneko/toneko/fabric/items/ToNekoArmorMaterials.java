package org.cneko.toneko.fabric.items;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoArmorMaterials {
    public static RegistryEntry<ArmorMaterial> NEKO;

    public static void init(){
        // 如果启用了仅服务器端，则不注册物品
        if (!ConfigUtil.ONLY_SERVER) registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO = register(
                "neko_tail",
                Map.of(
                        ArmorItem.Type.LEGGINGS, 0 // 猫尾巴要啥防御点呀
                ),
                15, // 嗯...还是让你们可以附魔吧
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, // 暂时和皮革的一样就好啦
                () -> Ingredient.ofItems(Items.LEATHER), // 暂时和皮革的一样就好啦
                0.0F, // 猫尾巴可以吸收什么伤害呢
                0.0F, // 猫尾巴还能抵御击退吗?肯定不能啦
                false // 猫尾巴可以染色吗?当然可以啦,但是现在技术还不够呢,求原谅
        );
    }

    /**
     *  注册一个新的盔甲
     * @param id 盔甲id
     * @param defensePoints 防御点
     * @param enchantability 附魔能力
     * @param equipSound 音效
     * @param repairIngredientSupplier 修复原料
     * @param toughness 盔甲会吸收多少伤害
     * @param knockbackResistance 击退抗性
     * @param dyeable 是否可染色
     * @return 盔甲材料
     */
    public static RegistryEntry<ArmorMaterial> register(
            String id, Map<ArmorItem.Type, Integer> defensePoints,
            int enchantability, RegistryEntry<SoundEvent> equipSound,
            Supplier<Ingredient> repairIngredientSupplier,
            float toughness,
            float knockbackResistance,
            boolean dyeable) {
        // Get the supported layers for the armor material
        List<ArmorMaterial.Layer> layers = List.of(
                // 纹理层的 ID、后缀以及该层是否可染色。
                // 我们可以将盔甲材质 ID 作为纹理层 ID 传递。
                // 我们不需要后缀，因此我们将传递一个空字符串。
                // 我们将传递收到的可染色布尔值作为可染色参数。
                new ArmorMaterial.Layer(Identifier.of(MODID, id), "", dyeable)
        );

        ArmorMaterial material = new ArmorMaterial(defensePoints, enchantability, equipSound, repairIngredientSupplier, layers, toughness, knockbackResistance);
        // Register the material within the ArmorMaterials registry.
        material = Registry.register(Registries.ARMOR_MATERIAL, Identifier.of(MODID, id), material);

        // 大多数时候，您会需要材质的 RegistryEntry - 尤其是对于 ArmorItem 构造函数。
        return RegistryEntry.of(material);
    }

}
