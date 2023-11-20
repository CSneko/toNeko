package com.crystalneko.toneko.items

import com.crystalneko.toneko.ToNeko
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe


class stickLevel2 {
    lateinit var plugin:ToNeko
    fun stickLevel2(plugin:ToNeko){
        this.plugin = plugin
        //注册合成表
        register()
    }

    private fun register(){
        // 创建一个具有特定形状的合成配方
        val recipe = ShapedRecipe(NamespacedKey(plugin, "toneko.item.stickLevel2"), getStick.stickLevel2)
        // 设置合成配方的形状
        /*
         牛奶 | 一级棍 | 牛奶
         牛奶 | 末地烛 | 牛奶
         牛奶 | 一级棍 | 牛奶
         */
        recipe.shape("XYX", "XZX", "XYX")
        // 创建 RecipeChoice 对象，将 ItemStack 转换为 RecipeChoice
        val itemChoice = RecipeChoice.ExactChoice(getStick.stick)
        // 设置形状中每个字符对应的原材料
        recipe.setIngredient('X', Material.MILK_BUCKET)
        recipe.setIngredient('Z', Material.END_ROD)
        recipe.setIngredient('Y', itemChoice)

        // 注册合成配方
        plugin.server.addRecipe(recipe)
    }
}