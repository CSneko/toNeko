package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.interactives.CrystalNekoInteractivePayload;
import org.jetbrains.annotations.Nullable;

public class CrystalNekoEntity extends NekoEntity{
    public static final String SKIN = "crystal_neko";
    public static final String NAME = "Crystal_Neko";
    public CrystalNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal(NAME));
    }

    @Override
    public @Nullable NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return null;
    }

    @Override
    public boolean canMate(INeko other) {
        return false;
    }

    @Override
    public void openInteractiveMenu(ServerPlayer player) {
        ServerPlayNetworking.send(player,new CrystalNekoInteractivePayload(this.getUUID().toString()));
    }

    @Override
    public String getSkin() {
        return SKIN;
    }
}
