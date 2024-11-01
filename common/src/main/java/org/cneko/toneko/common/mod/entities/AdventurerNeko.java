package org.cneko.toneko.common.mod.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdventurerNeko extends NekoEntity{
    public static final List<String> nekoSkins = new ArrayList<>();
    static {
        nekoSkins.addAll(List.of("grmmy","aquarter"));
    }
    public AdventurerNeko(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public String getSkin() {
        // 你问我为啥要这样干？ 啊我也不知道我为啥要这样干
        String u = this.uuid.toString();
        if (u.startsWith("a") || u.startsWith("b") || u.startsWith("c") || u.startsWith("d")
        || u.startsWith("e") || u.startsWith("f")
        ){
            return "aquarter";
        }
        return "grmmy";
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

    @Override
    public @Nullable AdventurerNeko getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new AdventurerNeko(ToNekoEntities.ADVENTURER_NEKO, level);
    }

    public static AttributeSupplier.Builder createAdventurerNekoAttributes(){
        return createNekoAttributes();
    }
}
