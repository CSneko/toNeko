package org.cneko.toneko.common.mod.advencements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** 集齐所有变质等级的水（fresh/slight/moderate/heavy/foul/overwhelming） */
public class SpoiledWaterCollectorTrigger extends SimpleCriterionTrigger<SpoiledWaterCollectorTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        if (!hasAllGrades(player)) return;
        this.trigger(player, instance -> true);
    }

    private static boolean hasAllGrades(ServerPlayer player) {
        Set<String> grades = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.has(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT)
                    && stack.has(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT)) {
                grades.add(ScentedWaterUtil.grade(ScentedWaterUtil.getSpoilage(stack)));
            }
        }
        return grades.size() >= 6;
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
                .apply(instance, SpoiledWaterCollectorTrigger.TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> create() {
            return ToNekoCriteria.SPOILED_WATER_COLLECTOR.createCriterion(new SpoiledWaterCollectorTrigger.TriggerInstance(Optional.empty()));
        }
    }
}
