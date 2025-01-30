package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record PluginDetectPayload(String installed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PluginDetectPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "detect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PluginDetectPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PluginDetectPayload::installed,
            PluginDetectPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
