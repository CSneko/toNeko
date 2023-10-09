package com.crystalneko.tonekofabric.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class command{
    public command(){
        //注册命令
        initCommand();
    }

    /**
     * 注册命令
     */

    public void initCommand(){
        //----------------------------------------------------toneko---------------------------------------------------
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("toneko")
                .executes(context -> {
                    context.getSource().sendMessage(Text.literal("Fabric版本正在制作，请等待发布"));
                    return 1;
                })));
    }


}
