package org.cneko.toneko.common.mod.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdventurerNeko extends NekoEntity{
    public static final List<String> nekoSkins = new ArrayList<>();
    static {
        nekoSkins.addAll(List.of("grmmy","aquarter"));
    }
    public AdventurerNeko(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }


    @Override
    public void registerGoals() {
        super.registerGoals();
    }

    @Override
    public boolean isFavoriteItem(ItemStack stack) {
        return super.isFavoriteItem(stack) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
    }

    @Override
    public @Nullable AdventurerNeko getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new AdventurerNeko(ToNekoEntities.ADVENTURER_NEKO, level);
    }

    public static AttributeSupplier.Builder createAdventurerNekoAttributes(){
        return createNekoAttributes();
    }
}
