package org.cneko.toneko.fabric.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdventurerNeko extends NekoEntity{
    public static final List<String> nekoSkins = new ArrayList<>();
    static {
        nekoSkins.addAll(List.of("grmmy"));
    }
    public AdventurerNeko(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Set<Item> getFavoriteItems() {
        Set<Item> i = super.getFavoriteItems();
        i.add(Items.DIAMOND_SWORD);
        i.add(Items.DIAMOND_CHESTPLATE);
        i.add(Items.DIAMOND_HELMET);
        i.add(Items.DIAMOND_LEGGINGS);
        i.add(Items.DIAMOND_BOOTS);
        i.add(Items.NETHERITE_SWORD);
        i.add(Items.NETHERITE_CHESTPLATE);
        i.add(Items.NETHERITE_HELMET);
        i.add(Items.NETHERITE_LEGGINGS);
        i.add(Items.NETHERITE_BOOTS);
        return i;
    }

    public static AttributeSupplier.Builder createAdventurerNekoAttributes(){
        return createNekoAttributes();
    }
}
