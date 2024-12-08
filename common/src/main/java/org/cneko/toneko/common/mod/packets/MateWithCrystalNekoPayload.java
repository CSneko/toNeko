package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record MateWithCrystalNekoPayload(String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MateWithCrystalNekoPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "mate_with_crystal_neko"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MateWithCrystalNekoPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MateWithCrystalNekoPayload::uuid,
            MateWithCrystalNekoPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
