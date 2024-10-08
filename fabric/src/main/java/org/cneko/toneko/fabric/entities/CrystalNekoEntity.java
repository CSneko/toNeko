package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.Nullable;

public class CrystalNekoEntity extends NekoEntity{
    public static final String SKIN = "crystal_neko";
    public static final String NAME = "Crystal_Neko";
    private int nyaCount = 0;
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
//        ServerPlayNetworking.send(player,new CrystalNekoInteractivePayload(this.getUUID().toString()));
    }

    @Override
    public String getSkin() {
        return SKIN;
    }

    public void nya(Player player){
        if (nyaCount < 6) {
            int r = random.nextInt(6);
            player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.nya." + r));
            nyaCount++;
        }else {
            int r = random.nextInt(5);
            player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.nya_tried."+ r));
        }
    }

    public static boolean checkCrystalNekoSpawnRules(EntityType<? extends net.minecraft.world.entity.Mob> entityType, LevelAccessor levelAccessor, MobSpawnType reason, BlockPos pos, RandomSource randomSource) {
        return true;
    }
}
