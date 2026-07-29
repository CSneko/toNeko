package org.cneko.toneko.common.mod.advencements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class HissComboTrigger extends SimpleCriterionTrigger<HissComboTrigger.TriggerInstance> {

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int combo) {
        trigger(player, instance -> instance.matches(combo));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, int combo) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.INT.fieldOf("combo").forGetter(TriggerInstance::combo)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(int combo) {
            return combo >= this.combo;
        }

        public static Criterion<TriggerInstance> hasCombo(int combo) {
            return ToNekoCriteria.HISS_COMBO.createCriterion(new TriggerInstance(Optional.empty(), combo));
        }
    }
}
