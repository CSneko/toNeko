package com.crystalneko.tonekofabric.event;

import io.netty.channel.EventLoop;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.thread.ThreadExecutor;

public class chatEvent {
    public chatEvent(){
        noDisplayMessage();
    }

    //------------------------------------------------------不显示某条消息------------------------------------------------
    public void noDisplayMessage() {
        ClientPlayNetworking.registerReceiver(new Identifier("minecraft:chat"), (client, handler, buf, responseSender) -> {
            final String message = buf.readString();
            client.execute(() -> {
                //判断消息是否含有特殊字符
                if (message.contains("§b>>§f")) {
                    client.inGameHud.getChatHud().addMessage(Text.of(message));
                }
            });
        });
    }
}

