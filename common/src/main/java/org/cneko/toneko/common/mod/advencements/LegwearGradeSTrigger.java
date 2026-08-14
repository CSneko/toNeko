package org.cneko.toneko.common.mod.advencements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** 绝对领域达到 S 级（工作台调节后领域等级为 S 时触发） */
public class LegwearGradeSTrigger extends SimpleCriterionTrigger<LegwearGradeSTrigger.TriggerInstance>{

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
                .apply(instance, LegwearGradeSTrigger.TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> create() {
            return ToNekoCriteria.LEGWEAR_GRADE_S.createCriterion(new LegwearGradeSTrigger.TriggerInstance(Optional.empty()));
        }
    }
}
