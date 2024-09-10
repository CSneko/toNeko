package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record RideEntityPayload(String uuid,String vehicleUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RideEntityPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_entity_interactive_ride_entity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RideEntityPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,RideEntityPayload::uuid,
            ByteBufCodecs.STRING_UTF8,RideEntityPayload::vehicleUuid,
            RideEntityPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
