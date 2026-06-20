package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * S2C: Server sends chat history back to client.
 * Each string is formatted as "role:text" where role is "user" or "assistant".
 */
public record ChatHistoryResponsePayload(String nekoUuid, List<String> messages) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChatHistoryResponsePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "chat_history_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatHistoryResponsePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ChatHistoryResponsePayload::nekoUuid,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ChatHistoryResponsePayload::messages,
                    ChatHistoryResponsePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
}
