package org.cneko.toneko.common.mod.advencements;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NekoLevelTrigger extends SimpleCriterionTrigger<NekoLevelTrigger.TriggerInstance> {

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, triggerInstance -> triggerInstance.matches(player.getNekoLevel()));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, double level) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.DOUBLE.fieldOf("level").forGetter(TriggerInstance::level)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(double level) {
            return level >= this.level;
        }

        public static Criterion<TriggerInstance> hasLevel(double level) {
            return ToNekoCriteria.NEKO_LV100.createCriterion(new TriggerInstance(Optional.empty(), level));
        }
    }
}