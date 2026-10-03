package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Requests target only the sending player's mushroom companion; actions are server-validated. */
public record MushroomInteractionPayload(String uuid, String action) implements CustomPacketPayload {
    public static final Type<MushroomInteractionPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("toneko", "mushroom_interaction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MushroomInteractionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MushroomInteractionPayload::uuid,
            ByteBufCodecs.STRING_UTF8, MushroomInteractionPayload::action, MushroomInteractionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
