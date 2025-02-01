package org.cneko.toneko.common.mod.advencements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.Optional;

public class GiftNekoTrigger extends SimpleCriterionTrigger<GiftNekoTrigger.TriggerInstance>{

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
                .apply(instance, GiftNekoTrigger.TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> create() {
            return ToNekoCriteria.GIFT_NEKO.createCriterion(new GiftNekoTrigger.TriggerInstance(Optional.empty()));
        }
    }
}
