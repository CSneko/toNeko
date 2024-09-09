package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record NekoEntityInteractivePayload(String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoEntityInteractivePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_entity_interactive"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoEntityInteractivePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,NekoEntityInteractivePayload::uuid,
            NekoEntityInteractivePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
