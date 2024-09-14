package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record NekoBreedPayload (String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoBreedPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_entity_interactive_breed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoBreedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,NekoBreedPayload::uuid,
            NekoBreedPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
