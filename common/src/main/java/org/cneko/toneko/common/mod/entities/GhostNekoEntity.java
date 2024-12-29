package org.cneko.toneko.common.mod.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFlyingAroundGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GhostNekoEntity extends NekoEntity{
    public static final List<String> nekoSkins = List.of("ninjia");
    public GhostNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
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
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected float getFlyingSpeed() {
        return 0.1f;
    }

    @Override
    public String getDefaultSkin() {
        return "ninjia";
    }
}
