package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record PlayerLeadByPlayerPayload(String holder,String target) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerLeadByPlayerPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "player_lead_by_player"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLeadByPlayerPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlayerLeadByPlayerPayload::holder,
            ByteBufCodecs.STRING_UTF8, PlayerLeadByPlayerPayload::target,
            PlayerLeadByPlayerPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}