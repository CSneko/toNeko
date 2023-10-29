package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.event.chatEvent;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.api.ModInitializer;
import org.bukkit.entity.Minecart;

public class ToNekoFabric implements ModInitializer {
    private command command;

    /**
     * 运行模组 initializer.
     */
    @Override
    public void onInitialize() {
        //注册命令
        this.command = new command();
    }
}
