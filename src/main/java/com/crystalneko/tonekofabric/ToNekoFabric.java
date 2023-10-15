package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.libs.base;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.Text;

import java.io.IOException;

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
