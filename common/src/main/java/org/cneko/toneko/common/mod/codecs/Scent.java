package org.cneko.toneko.common.mod.codecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 丝袜气味数据：intensity（0~100 强度）+ wearer（最近穿着者显示名，空串=无）。
 * 作为 {@code legwear_scent} DataComponent 的载体，持久化与网络同步两套 codec 成对提供。
 */
public record Scent(int intensity, String wearer) {
    public static final Scent EMPTY = new Scent(0, "");

    public static final Codec<Scent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("intensity").forGetter(Scent::intensity),
            Codec.STRING.fieldOf("wearer").forGetter(Scent::wearer)
    ).apply(instance, Scent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Scent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scent::intensity,
            ByteBufCodecs.STRING_UTF8, Scent::wearer,
            Scent::new
    );

    public Scent withIntensity(int newIntensity) {
        return new Scent(newIntensity, wearer);
    }

    public Scent withWearer(String newWearer) {
        return new Scent(intensity, newWearer);
    }
}
