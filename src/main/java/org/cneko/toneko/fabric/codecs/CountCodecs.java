package org.cneko.toneko.fabric.codecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CountCodecs {
    public static final Codec<FloatCountCodec> FLOAT_COUNT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("count").forGetter(FloatCountCodec::getCount),
            Codec.FLOAT.fieldOf("max_count").forGetter(FloatCountCodec::getMaxCount)
    ).apply(instance, FloatCountCodec::new));

    public record FloatCountCodec(float count, float maxCount){
        public float getCount() {
            return count;
        }
        public float getMaxCount() {
            return maxCount;
        }
    }
}
