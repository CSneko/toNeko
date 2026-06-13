package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public record ToNekoActionPayload(String action, String targetUuid, String value1, String value2, String value3) implements CustomPacketPayload {
    public static final Type<ToNekoActionPayload> ID = new Type<>(toNekoLoc("toneko_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToNekoActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ToNekoActionPayload::action,
            ByteBufCodecs.STRING_UTF8, ToNekoActionPayload::targetUuid,
            ByteBufCodecs.STRING_UTF8, ToNekoActionPayload::value1,
            ByteBufCodecs.STRING_UTF8, ToNekoActionPayload::value2,
            ByteBufCodecs.STRING_UTF8, ToNekoActionPayload::value3,
            ToNekoActionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
