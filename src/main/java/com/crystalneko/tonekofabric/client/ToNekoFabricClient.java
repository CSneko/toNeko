package com.crystalneko.tonekofabric.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.api.ClientModInitializer;
import com.crystalneko.tonekofabric.event.chatEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import static net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerReceiver;

public class ToNekoFabricClient implements ClientModInitializer {
    private chatEvent chatEvent;
        private boolean isInitialized = false;
    private boolean hasRegistered = false;

    @Override
    public void onInitializeClient() {
        // 注册 "ClientPlayConnectionEvents"
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            this.chatEvent = new chatEvent();
        });
    }


}
