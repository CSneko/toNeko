package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEnchantments {

    public static final ResourceKey<Enchantment> REVERSION = of("reversion"); // 反转

    public static void init(){

    }

    /**
     * 注册一个toNeko的Enchantment
     * @param registry 注册器
     * @param key RegistryKey
     * @param builder Enchantment.Builder
     */
    public static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }

    /**
     * 获取一个toNeko的Enchantment的RegistryKey
     * @param id 附魔id
     * @return RegistryKey
     */
    public static ResourceKey<Enchantment> of(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID,id));
    }
}
