package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record CrystalNekoNyaPayload(@NotNull String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrystalNekoNyaPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "crystal_neko_nya"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalNekoNyaPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CrystalNekoNyaPayload::uuid,
            CrystalNekoNyaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}