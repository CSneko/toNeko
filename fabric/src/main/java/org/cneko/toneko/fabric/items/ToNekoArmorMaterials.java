package org.cneko.toneko.fabric.items;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 26.x 迁移说明：{@code ArmorMaterial} 变为不可注册的 record
 * (durability, defense, enchantmentValue, equipSound, toughness, knockbackResistance, repairable, asset)，
 * 相关注册表（ARMOR_MATERIAL 等）已删除。
 * 这里改为直接构造材质实例；装备渲染资源键使用自定义 id，客户端需要 assets/toneko/equipment/*.json（由 GeckoLib/原版资源提供）。
 */
public class ToNekoArmorMaterials {
    public static ArmorMaterial NEKO;
    public static ArmorMaterial LEGWEAR;
    public static void init(){
        // 如果启用了仅服务器端，则不注册物品
        registerWithOutConfig();
    }

    /**
     * 强制构建材质，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        ResourceKey<EquipmentAsset> nekoAsset = equipmentAsset("neko_tail");
        ResourceKey<EquipmentAsset> legwearAsset = equipmentAsset("legwear");

        NEKO = new ArmorMaterial(
                15, // durability
                Map.of(ArmorType.CHESTPLATE, 0), // 猫尾巴要啥防御点呀
                15, // enchantability：嗯...还是让你们可以附魔吧
                SoundEvents.CAT_AMBIENT_BABY, // 喵喵喵~
                0.5F, // toughness：猫尾巴可以吸收什么伤害呢
                0.5F, // knockbackResistance：猫尾巴还能抵御击退吗?肯定不能啦
                woolTag(), // 修复材料：羊毛
                nekoAsset
        );

        LEGWEAR = new ArmorMaterial(
                15, // durability
                Map.of(ArmorType.LEGGINGS, 1), // 丝袜：象征性的 1 点防御
                15, // enchantability：可以附魔
                SoundEvents.ARMOR_EQUIP_LEATHER, // 皮革摩擦声比较贴切
                0F, // 丝袜当然吸收不了伤害啦
                0F, // 也抵御不了击退
                woolTag(), // 修复材料：羊毛
                legwearAsset
        );
    }

    /** 羊毛标签，用于铁砧修复。 */
    private static TagKey<net.minecraft.world.item.Item> woolTag() {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wool"));
    }

    private static ResourceKey<EquipmentAsset> equipmentAsset(String id) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, id));
    }
}
