package org.cneko.toneko.common.mod.genetics.api;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 遗传因子（等位基因），支持 Modifier 的自动叠加与清理，以及 AI 的动态装卸。
 */
public class Allele {
    @Getter
    private final ResourceLocation id;
    @Getter
    private final int dominance; // 显性权重

    // 自定义数据回调 (用于修改萌属性、NBT等)
    private final BiConsumer<LivingEntity, CompoundTag> onExpress;
    private final BiConsumer<LivingEntity, CompoundTag> onRemove;

    // 属性修饰符模板 (参数：LocusID -> 生成唯一的Modifier)
    private final List<ModifierTemplate> modifierTemplates = new ArrayList<>();

    // AI 注入模板
    private final List<Function<Mob, PrioritizedGoal>> aiTemplates = new ArrayList<>();

    public Allele(ResourceLocation id, int dominance,
                  BiConsumer<LivingEntity, CompoundTag> onExpress,
                  BiConsumer<LivingEntity, CompoundTag> onRemove) {
        this.id = id;
        this.dominance = dominance;
        this.onExpress = onExpress != null ? onExpress : (e, tag) -> {};
        this.onRemove = onRemove != null ? onRemove : (e, tag) -> {};
    }

    /**
     * 声明一个属性修饰符，多个相同的基因如果处于不同的基因座，修饰符会自动叠加！
     */
    public Allele addAttributeModifier(Holder<Attribute> attribute, String modifierNameSuffix, double amount, AttributeModifier.Operation operation) {
        this.modifierTemplates.add(new ModifierTemplate(attribute, modifierNameSuffix, amount, operation));
        return this;
    }

    /**
     * 声明一个由基因注入的 AI Goal
     */
    public Allele addAIGoal(int priority, Function<Mob, Goal> goalFactory) {
        this.aiTemplates.add(mob -> new PrioritizedGoal(priority, goalFactory.apply(mob)));
        return this;
    }

    // --- 引擎调用的生命周期方法 ---

    public void apply(LivingEntity entity, Locus locus, CompoundTag geneticData, List<Goal> trackedGoals) {
        // 1. 执行自定义逻辑 (如添加 MoeTags)
        this.onExpress.accept(entity, geneticData);

        // 2. 注入并叠加 Attribute Modifier
        for (ModifierTemplate template : modifierTemplates) {
            AttributeInstance instance = entity.getAttribute(template.attribute);
            if (instance != null) {
                ResourceLocation modifierId = getDynamicModifierId(locus.id(), template.suffix);
                // 确保先清除旧的，防止因为数值更新导致的崩溃
                instance.removeModifier(modifierId);
                instance.addPermanentModifier(new AttributeModifier(modifierId, template.amount, template.operation));
            }
        }

        // 3. 动态添加 AI (如果实体是 Mob)
        if (entity instanceof Mob mob) {
            for (Function<Mob, PrioritizedGoal> aiFactory : aiTemplates) {
                PrioritizedGoal pGoal = aiFactory.apply(mob);
                mob.goalSelector.addGoal(pGoal.priority, pGoal.goal);
                trackedGoals.add(pGoal.goal); // 追踪，以便后续清理
            }
        }
    }

    public void remove(LivingEntity entity, Locus locus, CompoundTag geneticData) {
        // 1. 移除自定义数据
        this.onRemove.accept(entity, geneticData);

        // 2. 移除 Modifier
        for (ModifierTemplate template : modifierTemplates) {
            AttributeInstance instance = entity.getAttribute(template.attribute);
            if (instance != null) {
                ResourceLocation modifierId = getDynamicModifierId(locus.id(), template.suffix);
                instance.removeModifier(modifierId);
            }
        }
    }

    // 生成唯一的 Modifier ID，格式例如： toneko:genetic_locusid_modifiername
    private ResourceLocation getDynamicModifierId(ResourceLocation locusId, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "genetic_" + locusId.getPath() + "_" + suffix);
    }

    private record ModifierTemplate(Holder<Attribute> attribute, String suffix, double amount, AttributeModifier.Operation operation) {}
    public record PrioritizedGoal(int priority, Goal goal) {}
}