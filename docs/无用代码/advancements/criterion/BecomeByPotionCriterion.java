package org.cneko.toneko.fabric.advancements.criterion;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;


public class BecomeByPotionCriterion extends AbstractCriterion<BecomeByPotionCriterion.Conditions> {

    @Override
    protected Conditions conditionsFromJson(JsonObject json,
                                            Optional<LootContextPredicate> playerPredicate,
                                            AdvancementEntityPredicateDeserializer predicateDeserializer) {
        Conditions conditions = new Conditions();

        return conditions;
    }

    public void trigger(ServerPlayerEntity player) {
        trigger(player, conditions -> {
            return conditions.requirementsMet();
        });
    }

    public static AdvancementCriterion<Conditions> get() {
        return ToNekoCriterion.BECOME_BY_POTION.create(new Conditions());
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return null;
    }


    public static class Conditions implements CriterionConditions {


        boolean requirementsMet() {
            return true;
        }

        @Override
        public void validate(LootContextPredicateValidator validator) {

        }
    }
}
