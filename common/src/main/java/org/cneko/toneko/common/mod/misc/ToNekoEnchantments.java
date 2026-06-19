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
    // 猫能爆哈器附魔
    public static final ResourceLocation HISS_POWER_ID = toNekoLoc("hiss_power");
    public static final ResourceKey<Enchantment> HISS_POWER = of(HISS_POWER_ID); // 哈气强化
    public static final ResourceLocation HISS_SPREAD_ID = toNekoLoc("hiss_spread");
    public static final ResourceKey<Enchantment> HISS_SPREAD = of(HISS_SPREAD_ID); // 哈气扩散
    public static final ResourceLocation HISS_EFFICIENCY_ID = toNekoLoc("hiss_efficiency");
    public static final ResourceKey<Enchantment> HISS_EFFICIENCY = of(HISS_EFFICIENCY_ID); // 节能哈气
    public static final ResourceLocation COMBO_EXTEND_ID = toNekoLoc("combo_extend");
    public static final ResourceKey<Enchantment> COMBO_EXTEND = of(COMBO_EXTEND_ID); // 连击延续
    public static final ResourceLocation HISS_ROOT_ID = toNekoLoc("hiss_root");
    public static final ResourceKey<Enchantment> HISS_ROOT = of(HISS_ROOT_ID); // 定身哈气
    public static final ResourceLocation HISS_DEMOLISH_ID = toNekoLoc("hiss_demolish");
    public static final ResourceKey<Enchantment> HISS_DEMOLISH = of(HISS_DEMOLISH_ID); // 破坏哈气

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
