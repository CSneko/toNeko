package org.cneko.toneko.common.mod.client;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry;
import org.cneko.toneko.common.mod.commands.arguments.NekoArgument;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoClient {
    public static void init(){
        NekoScreenRegistry.init();
        ArgumentTypeRegistry.registerArgumentType(
                toNekoLoc("neko"),
                NekoArgument.class, SingletonArgumentInfo.contextFree(NekoArgument::neko));
    }
}
