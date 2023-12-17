package com.crystalneko.toneko.items;

import com.crystalneko.toneko.ToNeko;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class getStick {
    private ToNeko plugin;
    public static ItemStack stick;
    public static ItemStack stickLevel2;
    public getStick(ToNeko plugin){
        this.plugin = plugin;
        stick = registerStick();
        stickLevel2 = registerStickLevel2();
    }
    public void getStick(Player player){
        //添加到玩家背包
        player.getInventory().addItem(stick);
    }
    public void getStick2(Player player){
        //添加到玩家背包
        player.getInventory().addItem(stickLevel2);
    }
    private ItemStack registerStick(){
        // 创建一个NamespacedKey，用于唯一标识NBT标签
        NamespacedKey key = new NamespacedKey(plugin, "neko");
        //创建一个新的木棍对象
        ItemStack itemStack = new ItemStack(Material.STICK);
        //获取物品的ItemMeta对象
        ItemMeta itemMeta = itemStack.getItemMeta();
        //设置物品的展示名称
        itemMeta.setDisplayName(ToNeko.getMessage("item.stick"));
        // 原始的String类型的lore
        String loreString = ToNeko.getMessage("item.stick-lore");
        // 将String类型的lore转换为Component类型
        Component loreComponent = Component.text(loreString);
        // 创建一个List<Component>并加入转换后的lore
        List<Component> lore = new ArrayList<>();
        lore.add(loreComponent);
        itemMeta.lore(lore);
        //为物品添加附魔特效
        itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        //设置物品的自定义材质
        itemMeta.setCustomModelData(10000);
        //设置物品的NBT标签
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "true");
        //将修改后的ItemMeta对象应用到物品上
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    private ItemStack registerStickLevel2(){
        // 创建一个NamespacedKey，用于唯一标识NBT标签
        NamespacedKey key = new NamespacedKey(plugin, "nekolevel");
        //创建一个新的对象
        ItemStack itemStack = new ItemStack(Material.END_ROD);
        //获取物品的ItemMeta对象
        ItemMeta itemMeta = itemStack.getItemMeta();
        //设置物品的展示名称
        itemMeta.setDisplayName(ToNeko.getMessage("item.stick-level-2"));
        // 原始的String类型的lore
        String loreString = ToNeko.getMessage("item.stick-level-2-lore");
        // 将String类型的lore转换为Component类型
        Component loreComponent = Component.text(loreString);
        // 创建一个List<Component>并加入转换后的lore
        List<Component> lore = new ArrayList<>();
        lore.add(loreComponent);
        itemMeta.lore(lore);
        //为物品添加附魔特效
        itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
        itemMeta.addEnchant(Enchantment.LOYALTY, 1, true);
        //设置物品的自定义材质
        itemMeta.setCustomModelData(10000);
        //设置物品的NBT标签
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 2);
        //将修改后的ItemMeta对象应用到物品上
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
