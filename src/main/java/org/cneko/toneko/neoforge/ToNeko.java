package org.cneko.toneko.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("toneko")
public class ToNeko {
    public ToNeko(IEventBus bus, ModContainer container){
        /*
        这是给NeoForge用的toNeko
        但是我不太了解NeoForge的机制
        理论上来说，在安装了Forgified Fabric API后可以正常运行部分
        但是奇怪的是竟然缺少了原版的类
        真是令人匪夷所思
        也许是Minecraft还没被加载？
        不过算了吧
        就算支持了估计也不会运行得太好
        所以
        我还是放弃吧¯\_(ツ)_/¯
         */
        new org.cneko.toneko.fabric.ToNeko().onInitialize();
    }
}
