package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import net.fabricmc.api.ModInitializer;

public class ToNekoFabric implements ModInitializer {
    private command command;

    /**
     * 运行模组 initializer.
     */
    @Override
    public void onInitialize() {
        this.command = new command();
        System.out.println("我来证明mod被加载了");

    }
}
