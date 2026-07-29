package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 客户端→服务端：猫娘潜行切换
 * @param active 是否激活潜行
 */
public record NekoStealthPayload(boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoStealthPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_stealth"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NekoStealthPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, NekoStealthPayload::active,
                    NekoStealthPayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
