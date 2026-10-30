package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 客户端→服务端：扑跃释放（松开潜行键时发送一次）。
 *
 * @param chargeTicks 蓄力时长（tick）。客户端已按 {@code PounceHandler.MAX_CHARGE_TICKS} 截断，
 *                    服务端仍会再次 clamp，避免被篡改成超长蓄力
 */
public record PouncePayload(int chargeTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PouncePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MODID, "pounce"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PouncePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PouncePayload::chargeTicks,
                    PouncePayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
