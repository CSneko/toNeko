package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record OpenPlotScreenPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenPlotScreenPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_plot_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenPlotScreenPayload> CODEC = StreamCodec.unit(new OpenPlotScreenPayload());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
