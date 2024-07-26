package org.cneko.toneko.fabric.enchanments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEnchantments {

    public static final RegistryKey<Enchantment> REVERSION = of("reversion"); // 反转

    public static void init(){

    }

    /**
     * 注册一个toNeko的Enchantment
     * @param registry 注册器
     * @param key RegistryKey
     * @param builder Enchantment.Builder
     */
    public static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }

    /**
     * 获取一个toNeko的Enchantment的RegistryKey
     * @param id 附魔id
     * @return RegistryKey
     */
    public static RegistryKey<Enchantment> of(String id) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID,id));
    }
}
