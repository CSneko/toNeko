package org.cneko.toneko.common.mod.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoFlyingAroundGoal;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GhostNekoEntity extends NekoEntity{
    public static final List<String> nekoSkins = List.of("ninjia");
    public GhostNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
//        this.noPhysics = true;
    }

    @Override
    public @Nullable NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new GhostNekoEntity(ToNekoEntities.GHOST_NEKO,this.level());
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(50,new NekoFlyingAroundGoal(this));
    }
}
