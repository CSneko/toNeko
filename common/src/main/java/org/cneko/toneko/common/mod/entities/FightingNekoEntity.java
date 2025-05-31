package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.FightingNekoAttackGoal;
import org.cneko.toneko.common.mod.items.BazookaItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class FightingNekoEntity extends NekoEntity{
    public static final TagKey<Item> NEKO_WEAPON = TagKey.create(Registries.ITEM,toNekoLoc("neko/weapon"));

    public static final List<String> NEKO_SKINS = List.of(
            "ronin"
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
        return super.isLikedItem(stack) || stack.is(NEKO_WEAPON);
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FightingNekoAttackGoal(this));
    }

    public boolean hasWeapon(){
        return this.getInventory().contains(NEKO_WEAPON);
    }




    public static AttributeSupplier.Builder createFightingNekoAttributes() {
        return NekoEntity.createNekoAttributes();
    }
}
