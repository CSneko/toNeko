package com.crystalneko.tonekofabric.items;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

import static com.crystalneko.tonekofabric.libs.base.translatable;


public class stick {
    public static ItemStack stack;
    public stick(){
        //注册物品
        register();
    }
    public static void register(){
        ItemStack stack1 = new ItemStack(Items.STICK);
        //设置nbt ["neko","true"]
        NbtCompound nbt = new NbtCompound();
        NbtString nbtValue = NbtString.of("true");
        nbt.put("neko",nbtValue);
        // 创建描述文本对象
        Text description = translatable("item.stick-lore");
        nbt.putString("display",Text.Serializer.toJson(description));
        stack1.setNbt(nbt);
        //设置名称
        stack1.setCustomName(translatable("item.stick"));
        // 添加附魔特效(耐久1)
        stack1.addEnchantment(Enchantments.UNBREAKING, 1);
        stack = stack1;
    }
    public static ItemStack get(){
        return stack;
    }
}
