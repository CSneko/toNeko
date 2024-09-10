package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record VehicleStopRidePayload(String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VehicleStopRidePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "vehicle_stop_ride"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleStopRidePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,VehicleStopRidePayload::uuid,
            VehicleStopRidePayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
