package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoEnchantments {

    public static final ResourceLocation REVERSION_ID = toNekoLoc("reversion");
    public static final ResourceKey<Enchantment> REVERSION = of(REVERSION_ID); // 反转
    public static final ResourceLocation ENFORCEMENT_ID = toNekoLoc("enforcement");
    public static final ResourceKey<Enchantment> ENFORCEMENT = of(ENFORCEMENT_ID); // 强制

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
    public static ResourceKey<Enchantment> of(ResourceLocation id) {
        return ResourceKey.create(Registries.ENCHANTMENT, id);
    }
}
