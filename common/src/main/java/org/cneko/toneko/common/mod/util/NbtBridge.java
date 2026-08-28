package org.cneko.toneko.common.mod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 26.x：实体存档改为 ValueInput/ValueOutput。
 * 为减少迁移量，把项目自有的 NBT 块作为单个 {@link CompoundTag} 编解码，
 * 既有 saveNekoNBTData/loadNekoNBTData 逻辑保持不变。
 */
public final class NbtBridge {
    public static java.util.UUID readUuid(net.minecraft.nbt.CompoundTag tag, String key) {
        return tag.read(key, net.minecraft.core.UUIDUtil.CODEC).orElseGet(() -> new java.util.UUID(0L, 0L));
    }

    public static void putUuid(net.minecraft.nbt.CompoundTag tag, String key, java.util.UUID value) {
        tag.store(key, net.minecraft.core.UUIDUtil.CODEC, value);
    }

    /** 26.x：ItemStack.save()/parse() 变为 Codec，这里统一封装。 */
    public static net.minecraft.nbt.Tag encodeStack(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.item.ItemStack stack) {
        return stack.isEmpty()
                ? new net.minecraft.nbt.CompoundTag()
                : net.minecraft.world.item.ItemStack.CODEC
                        .encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                        .getOrThrow();
    }

    public static net.minecraft.world.item.ItemStack decodeStack(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        return net.minecraft.world.item.ItemStack.CODEC
                .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result().orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    public static final String DATA_KEY = "TonekoData";

    private NbtBridge() {}

    public static void store(CompoundTag data, ValueOutput out) {
        if (data == null || data.isEmpty()) return;
        out.store(DATA_KEY, CompoundTag.CODEC, data);
    }

    /** 从 ValueInput 读回自有 NBT 数据块；无数据时返回空标签。 */
    public static CompoundTag read(ValueInput in) {
        return in.read(DATA_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
