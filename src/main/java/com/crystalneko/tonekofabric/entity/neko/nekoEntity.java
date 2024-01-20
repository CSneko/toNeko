package com.crystalneko.tonekofabric.entity.neko;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class nekoEntity extends AnimalEntity {
    @Nullable
    private net.minecraft.entity.ai.goal.TemptGoal temptGoal;
    private static final Ingredient TAMING_INGREDIENT;

    public nekoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public AnimalEntity createChild(ServerWorld world, PassiveEntity entity) {
        return this;
    }


    @Override
    protected void initGoals() {
        //漫游目标，来自于CatEntity
        this.temptGoal = new TemptGoal(this, 0.6, TAMING_INGREDIENT, true);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5));
        this.goalSelector.add(4, this.temptGoal);
        this.goalSelector.add(9, new AttackGoal(this));
        this.goalSelector.add(11, new WanderAroundFarGoal(this, 0.8, 1.0000001E-5F));
        this.goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
    }



    static {
        TAMING_INGREDIENT = Ingredient.ofItems(new ItemConvertible[]{Items.COD, Items.SALMON});
    }
}
