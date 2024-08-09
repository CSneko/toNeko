package org.cneko.toneko.neoforge;

import net.neoforged.fml.common.Mod;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.neoforge.fabric.ToNeko;

import static org.cneko.toneko.common.Bootstrap.MODID;


@Mod(MODID)
public final class ToNekoNeoForge {
    public ToNekoNeoForge() {
        Bootstrap.bootstrap(); //通用初始化
        new ToNeko().onInitialize();
    }
}
