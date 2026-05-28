package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoAttackGoal;
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
    public void randomize() {
        super.randomize();
        // 随机近战武器（铁~钻石品质）
        Item[] meleeWeapons = {Items.IRON_SWORD, Items.DIAMOND_SWORD};
        Item weapon = meleeWeapons[this.random.nextInt(meleeWeapons.length)];
        if (this.getInventory().items.stream().noneMatch(s -> s.is(weapon))) {
            this.getInventory().add(new ItemStack(weapon));
        }
        // 概率获得Bazooka及弹药
        if (this.random.nextFloat() < 0.4f) {
            if (this.getInventory().items.stream().noneMatch(s -> s.is(ToNekoItems.BAZOOKA))) {
                this.getInventory().add(new ItemStack(ToNekoItems.BAZOOKA));
            }
            // 随机弹药类型（爆炸弹或闪电弹），放入一组
            Item ammo = this.random.nextBoolean() ? ToNekoItems.EXPLOSIVE_BOMB : ToNekoItems.LIGHTNING_BOMB;
            if (this.getInventory().items.stream().noneMatch(s -> s.is(ammo))) {
                this.getInventory().add(new ItemStack(ammo, 64));
            }
        }
    }

    private NekoAttackGoal attackGoal;

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.attackGoal = new NekoAttackGoal(this);
        this.goalSelector.addGoal(2, this.attackGoal);
    }

    private int unhurtTime = 0;
    private int loliLightningCooldown = 0;

    @Override
    public void tick() {
        super.tick();
        unhurtTime++;
        if (unhurtTime > 1280) {
            // 给予生命回复效果
            this.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
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

    @Override
    protected void setHatredTarget(LivingEntity target, int duration) {
        super.setHatredTarget(target, duration);
        // 直接将仇恨目标传入NekoAttackGoal，让goal系统接管战斗逻辑
        if (attackGoal != null) {
            attackGoal.setTarget(target);
        }
    }

    @Override
    protected void clearHatred() {
        LivingEntity wasTarget = this.hatredTarget;
        super.clearHatred();
        // 仅当goal追踪的正是仇恨目标时才清除，避免干扰goal自主狩猎的怪物目标
        if (attackGoal != null && wasTarget != null && wasTarget.equals(attackGoal.getTarget())) {
            attackGoal.setTarget(null);
        }
    }

    @Override
    protected void tickHatred() {
        if (this.hatredTarget == null) return;
        if (!this.hatredTarget.isAlive()) {
            clearHatred();
            return;
        }
        this.hatredCooldown--;
        // 战斗由NekoAttackGoal完全接管（通过setHatredTarget已传入目标）

        // 萝莉形态：周期性对仇恨玩家释放闪电攻击
        if (this.isBaby() && this.hatredTarget instanceof Player && !this.level().isClientSide) {
            if (loliLightningCooldown > 0) {
                loliLightningCooldown--;
            } else {
                loliLightningCooldown = 100; // 5秒冷却
                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, this.level());
                lightning.setPos(this.hatredTarget.getX(), this.hatredTarget.getY(), this.hatredTarget.getZ());
                lightning.setVisualOnly(true);
                this.level().addFreshEntity(lightning);
                this.hatredTarget.hurt(this.damageSources().lightningBolt(), 5.0f);
            }
        }

        trySendHatredMessage();
    }

    public static AttributeSupplier.Builder createFightingNekoAttributes() {
        return NekoEntity.createNekoAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.ARMOR,2);
    }
}
