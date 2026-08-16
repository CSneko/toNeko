package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * S2C：广播踩踏状态变化，让附近所有客户端同步播放/停止动画。
 * <p>
 * stomperUuid —— 踩踏者（执行踩的玩家）的 UUID。
 * targetUuid —— 被踩者（躺倒的目标）的 UUID。
 * part —— 踩的部位（face/body）。
 * pose —— 被踩者的躺倒姿态（back/prone）。
 * active —— {@code true} 开始踩（播放循环踩踏动画 + 被踩者躺倒），
 *           {@code false} 结束踩（播放收回动画 + 被踩者起身）。
 */
public record StompAnimPayload(String stomperUuid, String targetUuid, String part, String pose, boolean active) implements CustomPacketPayload {
    public static final Type<StompAnimPayload> ID = new Type<>(toNekoLoc("stomp_anim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StompAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StompAnimPayload::stomperUuid,
            ByteBufCodecs.STRING_UTF8, StompAnimPayload::targetUuid,
            ByteBufCodecs.STRING_UTF8, StompAnimPayload::part,
            ByteBufCodecs.STRING_UTF8, StompAnimPayload::pose,
            ByteBufCodecs.BOOL, StompAnimPayload::active,
            StompAnimPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
