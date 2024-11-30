package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record ChatWithNekoPayload(String uuid, String message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChatWithNekoPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "chat_with_neko_entity"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ChatWithNekoPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,ChatWithNekoPayload::uuid,
            ByteBufCodecs.STRING_UTF8,ChatWithNekoPayload::message,
            ChatWithNekoPayload::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
