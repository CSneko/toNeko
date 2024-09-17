package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record CrystalNekoInteractivePayload(String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrystalNekoInteractivePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "crystal_neko_entity_interactive"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalNekoInteractivePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CrystalNekoInteractivePayload::uuid,
            CrystalNekoInteractivePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}