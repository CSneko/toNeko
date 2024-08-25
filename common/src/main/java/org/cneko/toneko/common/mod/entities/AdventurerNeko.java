package org.cneko.toneko.common.mod.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AdventurerNeko extends NekoEntity{
    public static final List<String> nekoSkins = new ArrayList<>();
    static {
        nekoSkins.addAll(List.of("grmmy"));
    }
    public AdventurerNeko(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }


    public static AttributeSupplier.Builder createAdventurerNekoAttributes(){
        return createNekoAttributes();
    }
}
