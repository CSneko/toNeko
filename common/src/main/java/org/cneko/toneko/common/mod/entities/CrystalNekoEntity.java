package org.cneko.toneko.common.mod.entities;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.packets.interactives.CrystalNekoInteractivePayload;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CrystalNekoEntity extends NekoEntity{
    public static final String SKIN = "crystal_neko";
    public static final String NAME = "Crystal_Neko";
    public static final UUID CRYSTAL_NEKO_UUID = UUID.fromString("13309540-fcc1-4d54-884b-55433f1f431f");
    private int nyaCount = 0;
    public CrystalNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal(NAME));
    }

    @Override
    public NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new CrystalNekoEntity(ToNekoEntities.CRYSTAL_NEKO, level);
    }

    @Override
    public boolean canMate(INeko other) {
        return  other.getEntity().getUUID().equals(CRYSTAL_NEKO_UUID) || other.getEntity().getName().getString().contains(NAME);
    }

    @Override
    public void openInteractiveMenu(ServerPlayer player) {
        ServerPlayNetworking.send(player,new CrystalNekoInteractivePayload(this.getUUID().toString()));
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

    @Override
    public NekoQuery.Neko getNeko() {
        return NekoQuery.getNeko(CRYSTAL_NEKO_UUID);
    }
}
