package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record OpenNekoInfoScreenPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenNekoInfoScreenPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_neko_info_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNekoInfoScreenPayload> CODEC = StreamCodec.unit(new OpenNekoInfoScreenPayload());
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
