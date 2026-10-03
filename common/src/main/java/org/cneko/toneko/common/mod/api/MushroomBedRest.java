package org.cneko.toneko.common.mod.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;
import org.cneko.toneko.common.mod.entities.MushroomRules;
import java.util.Optional;

/** Preserve the bed viewpoint after vanilla completes waking and clears its sleep state. */
public final class MushroomBedRest {
    private MushroomBedRest() {}

    public record WakeSnapshot(BlockPos bed, Vec3 position, float yaw, float pitch) {}

    public static Optional<BlockPos> restingBed(Player player) {
        return player instanceof BedRestingPlayer resting ? resting.toneko$getRestingBed() : Optional.empty();
    }

    public static Optional<BlockPos> bed(Player player) {
        return player.getSleepingPos().or(() -> restingBed(player));
    }

    public static Direction direction(Player player) {
        return bed(player).map(pos -> BedBlock.getBedOrientation(player.level(), pos)).orElse(null);
    }

    public static WakeSnapshot beforeWake(Player player) {
        if (!player.isAlive() || player.isShiftKeyDown() || player.getSleepingPos().isEmpty()) return null;
        boolean accompanied = player.getPassengers().stream().anyMatch(entity -> entity instanceof MushroomGirlEntity mushroom
                && mushroom.getCompanionPose() != MushroomGirlEntity.CARRIED
                && mushroom.getCompanionPose() != MushroomGirlEntity.NONE
                && mushroom.getFamiliarity(player.getUUID()) >= MushroomRules.FRIEND_FAMILIARITY);
        return accompanied ? new WakeSnapshot(player.getSleepingPos().orElseThrow(), player.position(), player.getYRot(), player.getXRot()) : null;
    }

    public static void afterWake(Player player, WakeSnapshot snapshot) {
        if (snapshot == null || !(player instanceof BedRestingPlayer resting)) return;
        // Vanilla has already cleared sleeping position, freed the bed and updated sleep votes.
        // Reapply only position and pose, so being awake here cannot skip the next night.
        resting.toneko$setRestingBed(Optional.of(snapshot.bed()));
        player.setPos(snapshot.position());
        player.setYRot(snapshot.yaw());
        player.setXRot(snapshot.pitch());
        player.setDeltaMovement(Vec3.ZERO);
        player.setPose(Pose.SLEEPING);
        if (player instanceof ServerPlayer serverPlayer) PoseStateSync.pin(serverPlayer, Pose.SLEEPING);
    }

    public static void release(Player player) {
        Optional<BlockPos> bed = restingBed(player);
        if (bed.isEmpty()) return;
        Direction facing = direction(player);
        ((BedRestingPlayer) player).toneko$setRestingBed(Optional.empty());
        player.setPose(Pose.STANDING);
        if (player instanceof ServerPlayer serverPlayer && facing != null && player.isAlive()) {
            Vec3 stand = BedBlock.findStandUpPosition(player.getType(), player.level(), bed.get(), facing, player.getYRot())
                    .orElseGet(() -> Vec3.atBottomCenterOf(bed.get().above()));
            serverPlayer.connection.teleport(stand.x, stand.y, stand.z, player.getYRot(), player.getXRot());
        }
    }

    public static void tick(Player player) {
        Optional<BlockPos> bed = restingBed(player);
        if (bed.isEmpty()) return;
        if (player.level().isClientSide() && (player.isSleeping() || player.getSleepTimer() > 0)) {
            // The rest marker is authoritative: vanilla sleep has ended on the server.
            // Finish the client's wake too, even if its animation/metadata arrives late.
            // This clears the sleep timer/overlay and lets InBedChatScreen close normally.
            player.stopSleepInBed(true, false);
        }
        if (player instanceof ServerPlayer serverPlayer && (!player.isAlive() || player.isInWater()
                || player.isOnFire() || player.isPassenger() || !(player.level().getBlockState(bed.get()).getBlock() instanceof BedBlock))) {
            PoseStateSync.unpin(serverPlayer);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        // Matches LivingEntity#setPosToBed. Keep gravity and movement from drifting the bed camera.
        player.setPos(bed.get().getX() + 0.5, bed.get().getY() + 0.6875, bed.get().getZ() + 0.5);
        player.setOnGround(true);
        player.setPose(Pose.SLEEPING);
    }
}
