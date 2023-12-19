package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.event.*;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ToNekoFabric implements ModInitializer {
    public static Boolean started = false;
    private command command;

    /**
     * 运行模组 initializer.
     */
    @Override
    public void onInitialize() {
        new base();
        //注册命令
        this.command = new command();

        //注册监听事件
        event();

        //注册物品
        new stick();

        //注册权限组
        new lp();
    }
    //简易的监听事件
    private void event(){
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            new playerAttack();
            new playerJoin();
            new playerLeave();
        });
    }
}
