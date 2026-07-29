package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 客户端请求切换猫爪多功能工具的范围/模式。
 * action: 0 = 切换范围 (1→3→5→1)
 */
public record NekoMultiToolModePayload(int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoMultiToolModePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_multi_tool_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NekoMultiToolModePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NekoMultiToolModePayload::action,
                    NekoMultiToolModePayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
