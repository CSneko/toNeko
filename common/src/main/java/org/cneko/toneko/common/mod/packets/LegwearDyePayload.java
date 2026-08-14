package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 腿部服饰工作台左右腿染色（C2S）。
 * side: 0=左腿 1=右腿；rgb: -1 表示移除独立染色（回退整体 DYED_COLOR）。
 */
public record LegwearDyePayload(int side, int rgb) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LegwearDyePayload> ID =
            new CustomPacketPayload.Type<>(toNekoLoc("legwear_dye"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LegwearDyePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LegwearDyePayload::side,
            ByteBufCodecs.VAR_INT, LegwearDyePayload::rgb,
            LegwearDyePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
