package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record GiftItemPayload(String uuid, int slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GiftItemPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_entity_interactive_gift_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GiftItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,GiftItemPayload::uuid,
            ByteBufCodecs.INT,GiftItemPayload::slot,
            GiftItemPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}