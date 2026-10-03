package org.cneko.toneko.common.mod.entities;

import com.google.common.collect.ImmutableList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.api.BedRestingPlayer;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import sun.misc.Unsafe;
import java.util.Optional;
import java.util.UUID;

/** Apply the wake hooks around vanilla Player#stopSleepInBed, with isolated world fixtures. */
public final class MushroomBedRestRegressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Unsafe fixtures = (Unsafe) field.get(null);
        BedPlayer player = (BedPlayer) fixtures.allocateInstance(BedPlayer.class);
        BedWorld world = (BedWorld) fixtures.allocateInstance(BedWorld.class);
        Companion companion = (Companion) fixtures.allocateInstance(Companion.class);
        var passengers = Entity.class.getDeclaredField("passengers"); passengers.setAccessible(true);
        player.world = world; player.setUUID(UUID.randomUUID());
        companion.trust = 60; companion.mode = MushroomGirlEntity.SITTING_ON_PLAYER;
        passengers.set(player, ImmutableList.of(companion));

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            world.facing = facing;
            player.sleeping = Optional.of(new BlockPos(9, 64, -3));
            player.resting = Optional.empty(); player.pose = Pose.SLEEPING;
            player.location = new Vec3(9.5, 64.6875, -2.5);
            Vec3 original = player.location;
            var snapshot = MushroomBedRest.beforeWake(player);
            check(snapshot != null, "A familiar seated companion preserves the upcoming bed wake");
            // Vanilla still executes, including clearing sleep and its sleeping position.
            player.stopSleepInBed(false, false);
            check(!player.isSleeping() && player.pose == Pose.STANDING, "Vanilla completes normal wake-up before the hold is applied");
            MushroomBedRest.afterWake(player, snapshot);
            check(!player.isSleeping() && player.pose == Pose.SLEEPING, "Resting pose stays awake and cannot count as a sleeper");
            check(player.location.equals(original) && player.resting.equals(Optional.of(snapshot.bed())), "Wake retains bed position and synchronized bed identity");
            check(MushroomBedRest.direction(player) == facing, "Wake preserves each of the four bed orientations");
            check(MushroomGirlEntity.isPlayerLying(player), "Awake bed rest continues to allow the seated companion");
            Vec3 seat = player.getPassengerRidingPosition(companion).subtract(companion.getVehicleAttachmentPoint(player));
            Vec3 expectedSeat = original.add(-facing.getStepX() * 0.9, 0.22, -facing.getStepZ() * 0.9);
            check(seat.distanceToSqr(expectedSeat) < 1.0E-10, "The bed seat stays on the leg side for every bed orientation, including after waking");
            player.location = player.location.add(2, -0.2, 0);
            MushroomBedRest.tick(player);
            check(player.location.equals(original), "Awake bed rest does not drift due to movement or gravity");
            MushroomBedRest.release(player);
            check(player.resting.isEmpty() && player.pose == Pose.STANDING && !player.isSleeping(), "Explicit stand-up clears the rest without restarting sleep");
        }
        player.sleeping = Optional.of(new BlockPos(9, 64, -3)); player.pose = Pose.SLEEPING;
        companion.trust = 59;
        check(MushroomBedRest.beforeWake(player) == null, "A stranger does not hold the player in bed");
        companion.trust = 60; companion.mode = MushroomGirlEntity.CARRIED;
        check(MushroomBedRest.beforeWake(player) == null, "Being carried does not imply a bed seat");
        companion.mode = MushroomGirlEntity.SITTING_ON_PLAYER; player.shift = true;
        check(MushroomBedRest.beforeWake(player) == null, "An explicit Shift stand-up is respected during wake");
        player.shift = false; passengers.set(player, ImmutableList.of());
        check(MushroomBedRest.beforeWake(player) == null, "An ordinary player without a companion wakes normally");
        System.out.println("Mushroom bed rest regression: " + checks + " checks passed.");
    }

    private static final class Companion extends MushroomGirlEntity {
        int trust;
        byte mode;
        private Companion() { super(null, null); }
        @Override public int getFamiliarity(UUID uuid) { return trust; }
        @Override public byte getCompanionPose() { return mode; }
    }
    private static final class BedPlayer extends Player implements BedRestingPlayer {
        BedWorld world;
        Optional<BlockPos> sleeping, resting;
        Pose pose;
        Vec3 location;
        boolean shift;
        private BedPlayer() { super(null, null); }
        @Override public GameType gameMode() { return GameType.SURVIVAL; }
        @Override public Level level() { return world; }
        @Override public boolean isAlive() { return true; }
        @Override public boolean isShiftKeyDown() { return shift; }
        @Override public Optional<BlockPos> getSleepingPos() { return sleeping; }
        @Override public void clearSleepingPos() { sleeping = Optional.empty(); }
        @Override public Pose getPose() { return pose; }
        @Override public void setPose(Pose pose) { this.pose = pose; }
        @Override public Vec3 position() { return location; }
        @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return location.add(0, 1.6, 0); }
        @Override public void setPos(double x, double y, double z) { location = new Vec3(x, y, z); }
        @Override public void setDeltaMovement(Vec3 velocity) { }
        @Override public void setOnGround(boolean ground) { }
        @Override public Optional<BlockPos> toneko$getRestingBed() { return resting; }
        @Override public void toneko$setRestingBed(Optional<BlockPos> bed) { resting = bed; }
    }
    private static final class BedWorld extends ServerLevel {
        Direction facing;
        private BedWorld() { super(null, null, null, null, null, null, false, 0, java.util.List.of(), false); }
        @Override public boolean hasChunkAt(BlockPos pos) { return false; } // Skip native stand-up collision search in the isolated fixture.
        @Override public BlockState getBlockState(BlockPos pos) { return Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing); }
    }
}
