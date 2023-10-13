package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.libs.base;
import net.fabricmc.api.ModInitializer;

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
        //创建猫娘数据文件
        try {
            base.createFileInDirectory("ctlib/toneko","/nekos.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
