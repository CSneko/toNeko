package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * C2S：玩家按下/松开踩踏键，请求开始或结束「玩味的踩」。
 * <p>
 * targetUuid —— 被踩目标的 UUID（仅 {@code active == true} 时有意义）。
 * part —— 踩的部位：{@code face}（脸/头）或 {@code body}（胸口/身体）。
 * pose —— 被踩者的躺倒姿态：{@code back}（仰面朝上）或 {@code prone}（趴着）。
 * active —— {@code true} 表示按键按下（开始踩），{@code false} 表示松开（取消踩）。
 */
public record StompActionPayload(String targetUuid, String part, String pose, boolean active) implements CustomPacketPayload {
    public static final Type<StompActionPayload> ID = new Type<>(toNekoLoc("stomp_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StompActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StompActionPayload::targetUuid,
            ByteBufCodecs.STRING_UTF8, StompActionPayload::part,
            ByteBufCodecs.STRING_UTF8, StompActionPayload::pose,
            ByteBufCodecs.BOOL, StompActionPayload::active,
            StompActionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
