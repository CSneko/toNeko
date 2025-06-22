package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.FightingNekoAttackGoal;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class FightingNekoEntity extends NekoEntity{
    public static final TagKey<Item> NEKO_WEAPON = TagKey.create(Registries.ITEM,toNekoLoc("neko/weapon"));
    public static final TagKey<Item> MELEE_WEAPON = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c","tools/melee_weapon"));
    public static final TagKey<Item> RANGED_WEAPON = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c","tools/ranged_weapon"));

    public static final List<String> NEKO_SKINS = List.of(
            "ronin","miruu","muineow","myrrka","peelll"
    );
    public FightingNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public @Nullable NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new FightingNekoEntity(this.getType(), level);
    }

    @Override
    public boolean isLikedItem(ItemStack stack) {
        return super.isLikedItem(stack) || stack.is(NEKO_WEAPON) || stack.is(ToNekoItems.BAZOOKA_AMMO_TAG);
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FightingNekoAttackGoal(this));
    }

    public boolean hasWeapon(){
        return this.getInventory().contains(NEKO_WEAPON);
    }

    private int unhurtTime = 0;

    @Override
    public void tick() {
        super.tick();
        unhurtTime++;
        if (unhurtTime > 1280) {
            // 给予生命回复效果
            this.addEffect(new MobEffectInstance(
                    MobEffects.HEAL,
                    1, // 持续时间为1 tick
                    0 // 强度为0
            ));
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        unhurtTime = 0; // 重置未受伤时间
        return super.hurt(source, amount);
    }

    public static AttributeSupplier.Builder createFightingNekoAttributes() {
        return NekoEntity.createNekoAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.ARMOR,2);
    }
}
