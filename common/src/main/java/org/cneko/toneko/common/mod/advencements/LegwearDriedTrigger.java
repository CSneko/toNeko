package org.cneko.toneko.common.mod.advencements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** 在晾衣架上晾干一件丝袜（湿度从正数降为 0） */
public class LegwearDriedTrigger extends SimpleCriterionTrigger<LegwearDriedTrigger.TriggerInstance> {

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
                .apply(instance, LegwearDriedTrigger.TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> create() {
            return ToNekoCriteria.LEGWEAR_DRIED.createCriterion(new LegwearDriedTrigger.TriggerInstance(Optional.empty()));
        }
    }
}
