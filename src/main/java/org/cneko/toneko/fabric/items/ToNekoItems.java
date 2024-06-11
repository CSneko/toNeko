package org.cneko.toneko.fabric.items;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.cneko.toneko.common.util.ConfigUtil;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoItems {
    public static NekoPotion NEKO_POTION = new NekoPotion();
    public static void register() {
        // 如果启用了仅服务器端，则不注册物品
        if (!ConfigUtil.ONLY_SERVER) registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        Registry.register(Registries.ITEM, new Identifier(MODID,NekoPotion.ID), NEKO_POTION);
    }
}
