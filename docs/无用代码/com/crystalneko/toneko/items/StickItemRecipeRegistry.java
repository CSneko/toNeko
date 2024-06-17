package com.crystalneko.toneko.items;

import com.crystalneko.toneko.ToNeko;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

public class StickItemRecipeRegistry {

    public static void registerRecipe(){
        // 创建一个具有特定形状的合成配方
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(ToNeko.pluginInstance, "toneko.item.stickLevel2"), StickItemWrapper.wrapStickItemLevel2());
        // 设置合成配方的形状
        /*
         牛奶 | 一级棍 | 牛奶
         牛奶 | 末地烛 | 牛奶
         牛奶 | 一级棍 | 牛奶
         */
        recipe.shape("XYX", "XZX", "XYX");
        // 创建 RecipeChoice 对象，将 ItemStack 转换为 RecipeChoice
        RecipeChoice.ExactChoice itemChoice = new RecipeChoice.ExactChoice(StickItemWrapper.wrapStickItem());
        // 设置形状中每个字符对应的原材料
        recipe.setIngredient('X', Material.MILK_BUCKET);
        recipe.setIngredient('Z', Material.END_ROD);
        recipe.setIngredient('Y', itemChoice);

        // 注册合成配方
        Bukkit.getServer().addRecipe(recipe);
    }

}
