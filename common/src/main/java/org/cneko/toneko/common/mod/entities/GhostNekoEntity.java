package org.cneko.toneko.common.mod.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFlyingAroundGoal;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GhostNekoEntity extends NekoEntity{
    public static final List<String> nekoSkins = List.of("ninjia");
    public GhostNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.moveControl = new FlyingMoveControl(this,  20, true);
    }

    @Override
    public void randomize() {
        super.randomize();
        EntityUtil.randomizeAttributeValue(this, Attributes.FLYING_SPEED,0.4,0.15,0.3); // 实体的飞行速度为0.15~0.3间
    }

    @Override
    public @Nullable GhostNekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new GhostNekoEntity(ToNekoEntities.GHOST_NEKO,this.level());
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new NekoFlyingAroundGoal(this));
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getMainHandItem().is(Items.LEAD)) {
            return super.hurt(source, amount);
        }
        // 除非命令或魔法，否则不造成伤害
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.MAGIC)){
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public float getFlyingSpeed() {
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public String getDefaultSkin() {
        return "ninjia";
    }

    public static AttributeSupplier.Builder createGhostNekoAttributes(){
        return NekoEntity.createNekoAttributes().add(Attributes.FLYING_SPEED);
    }
}
