package org.cneko.toneko.common.mod.advencements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** 首次获得任意变质水（桶/瓶/喷溅/滞留/香水） */
public class SpoiledWaterFirstTrigger extends SimpleCriterionTrigger<SpoiledWaterFirstTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
                .apply(instance, SpoiledWaterFirstTrigger.TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> create() {
            return ToNekoCriteria.SPOILED_WATER_FIRST.createCriterion(new SpoiledWaterFirstTrigger.TriggerInstance(Optional.empty()));
        }
    }
}
