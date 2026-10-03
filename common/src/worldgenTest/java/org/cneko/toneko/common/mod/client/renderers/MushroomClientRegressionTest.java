package org.cneko.toneko.common.mod.client.renderers;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.BoneSnapshots;
import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.collect.ImmutableList;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.TickRateManager;
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
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import sun.misc.Unsafe;
import java.util.Map;
import java.util.Optional;

/** Actual client camera/mixin and Gecko bone transforms, without a GPU or a game world. */
public final class MushroomClientRegressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void field(Object instance, Class<?> owner, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name); field.setAccessible(true); field.set(instance, value);
    }
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        var fixtures = (Unsafe)unsafeField.get(null);
        // Resolving the listener also verifies the production wake injection's method and field targets.
        Class.forName(ClientPacketListener.class.getName());
        var world = (BedWorld)fixtures.allocateInstance(BedWorld.class);
        var player = (BedPlayer)fixtures.allocateInstance(BedPlayer.class);
        player.world = world; player.resting = Optional.of(new BlockPos(0, 64, 0));
        player.sleeping = player.resting; player.pose = Pose.SLEEPING;
        field(player, Entity.class, "passengers", ImmutableList.of());
        player.setPos(0.5, 64.6875, 0.5);
        field(player, Player.class, "sleepCounter", 90);
        world.facing = Direction.SOUTH;
        MushroomBedRest.tick(player);
        check(!player.isSleeping(), "A rest marker completes client wake even if the wake animation is delayed");
        check(player.getSleepTimer() == 0, "Client wake clears the sleeping screen overlay timer");
        check(player.getPose() == Pose.SLEEPING && player.resting.isPresent(), "Waking retains only the requested bed pose and identity");
        field(player, Player.class, "sleepCounter", 100);
        MushroomBedRest.tick(player);
        check(player.getSleepTimer() == 0, "The usual wake fade also clears without hiding the awake HUD");

        var minecraft = (Minecraft)fixtures.allocateInstance(Minecraft.class);
        var options = (OptionsProbe)fixtures.allocateInstance(OptionsProbe.class);
        options.distance = (OptionInstance<Double>)fixtures.allocateInstance(OptionInstance.class);
        field(options.distance, OptionInstance.class, "value", 1d);
        field(minecraft, Minecraft.class, "options", options);
        var camera = new Camera();
        field(camera, Camera.class, "minecraft", minecraft);
        field(camera, Camera.class, "entity", player);
        field(camera, Camera.class, "eyeHeight", 0.2f);
        field(camera, Camera.class, "eyeHeightOld", 0.2f);
        var align = Camera.class.getDeclaredMethod("alignWithEntity", float.class); align.setAccessible(true);
        var prepare = Camera.class.getDeclaredMethod("prepareCullFrustum", Matrix4fc.class, Matrix4f.class, Vec3.class);
        prepare.setAccessible(true);
        var projection = new Matrix4f().perspective((float)Math.toRadians(70), 16f / 9, 0.05f, 128);
        var mushroom = (Companion)fixtures.allocateInstance(Companion.class);
        mushroom.companionPose = MushroomGirlEntity.SITTING_ON_PLAYER;
        field(mushroom, Entity.class, "vehicle", player);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            world.facing = facing;
            for (float mouseYaw : new float[]{-120, -30, 0, 73, 180, 295}) {
              for (float pitch : new float[]{-75, -30, 0, 35, 75}) {
                player.setYRot(mouseYaw); player.setXRot(pitch);
                player.xo = player.getX(); player.yo = player.getY(); player.zo = player.getZ();
                // These are the native Camera.update stages, in their real order, excluding
                // GPU-dependent projection setup. The production mixin must run IN align.
                align.invoke(camera, 1f);
                check(Math.abs(camera.yRot() - (facing.toYRot() - 180)) < 1e-5 && camera.xRot() == pitch,
                        "Bed view follows up/down aim before culling, for every bed and mouse direction");
                check(camera.position().distanceTo(player.position().add(0, 0.5, 0)) < 1e-6,
                        "Looking up/down never moves the bed camera into the mattress or the player's body");
                prepare.invoke(camera, camera.getViewRotationMatrix(new Matrix4f()), projection, camera.position());
                var forward = new Vector3f(0, 0, -1).rotate(camera.rotation()).mul(4);
                Vec3 ahead = camera.position().add(forward.x, forward.y, forward.z);
                Vec3 behind = camera.position().add(-forward.x, -forward.y, -forward.z);
                check(camera.getCullFrustum().pointInFrustum(ahead.x, ahead.y, ahead.z),
                        "The scene in front of the actual bed view remains visible after mouse movement");
                check(!camera.getCullFrustum().pointInFrustum(behind.x, behind.y, behind.z),
                        "Culling never retains the old mouse-facing scene behind the bed view");
                var state = new TonekoLivingEntityGeoState(); state.bodyRot = mouseYaw;
                MushroomGirlRenderPose.alignCompanion(mushroom, state);
                check(state.bodyRot == facing.toYRot() && state.yRot == 0
                                && state.getGeckolibData(DataTickets.ENTITY_BODY_YAW) == facing.toYRot(),
                        "The living Gecko render state faces the player's head rather than inheriting mouse yaw");
              }
            }
        }
        var cameraState = new CameraRenderState();
        camera.extractRenderState(cameraState, 1f);
        check(!cameraState.entityRenderState.isSleeping, "The actual awake camera render state no longer suppresses hands as a sleeper");
        check(!minecraft.options.hideGui, "Bed rest preserves the player's HUD visibility setting");

        // Exercise the real LevelRenderer first-person entity filter, including its
        // production wrapper, without allocating GPU render buffers or drawing a frame.
        var renderWorld = (RenderWorld)fixtures.allocateInstance(RenderWorld.class);
        renderWorld.entities = java.util.List.of(player); renderWorld.ticks = new TickRateManager();
        field(minecraft, Minecraft.class, "level", renderWorld);
        var renderer = (LevelRendererProbe)fixtures.allocateInstance(LevelRendererProbe.class);
        field(renderer, LevelRenderer.class, "minecraft", minecraft);
        field(renderer, LevelRenderer.class, "level", renderWorld);
        field(renderer, LevelRenderer.class, "entityRenderDispatcher", fixtures.allocateInstance(DispatcherProbe.class));
        var extract = LevelRenderer.class.getDeclaredMethod("extractVisibleEntities", Camera.class,
                Frustum.class, DeltaTracker.class, LevelRenderState.class); extract.setAccessible(true);
        var renderState = new LevelRenderState();
        extract.invoke(renderer, camera, camera.getCullFrustum(), DeltaTracker.ONE, renderState);
        check(renderState.entityRenderStates.size() == 1, "An awake player resting in bed still renders their own body");
        player.sleeping = player.resting;
        check(mushroom.isWinkingAtSleepingPlayer(), "Sitting on a sleeping player selects the wink expression");
        mushroom.companionPose = MushroomGirlEntity.CARRIED;
        check(!mushroom.isWinkingAtSleepingPlayer(), "Carried companions never select the bed wink");
        mushroom.companionPose = MushroomGirlEntity.LYING_ON_PLAYER;
        check(!mushroom.isWinkingAtSleepingPlayer(), "Lying companions never select the seated wink");
        mushroom.companionPose = MushroomGirlEntity.SITTING_ON_PLAYER;
        align.invoke(camera, 1f);
        check(camera.xRot() == player.getViewXRot(1f), "Native sleeping bed view also supports up/down aim");
        renderState.entityRenderStates.clear();
        extract.invoke(renderer, camera, camera.getCullFrustum(), DeltaTracker.ONE, renderState);
        check(renderState.entityRenderStates.size() == 1, "Native sleep retains its original own-body rendering");
        MushroomBedRest.tick(player);
        check(!mushroom.isWinkingAtSleepingPlayer(), "Waking ends the seated wink even while bed rest continues");
        check(!player.isSleeping(), "Showing the resting body never restores actual sleeping state");

        var head = BoneSnapshot.create(null);
        var left = BoneSnapshot.create(null); var right = BoneSnapshot.create(null);
        Map<String, BoneSnapshot> bones = Map.of("head", head, "iris_left", left, "iris_right", right);
        BoneSnapshots snapshots = name -> Optional.ofNullable(bones.get(name));
        MushroomGirlRenderPose.applyLookDirection(snapshots, 0, 35, true);
        var stack = new PoseStack(); head.rotate(stack);
        var gaze = stack.last().pose().transformDirection(new Vector3f(0, 0, -1));
        check(gaze.y < -0.5f, "The actual Gecko head transformation looks down, not up");
        check(left.getTranslateY() < 0 && right.getTranslateY() < 0, "Both irises follow the downward gaze");
        MushroomBedRest.release(player);
        align.invoke(camera, 1f);
        check(camera.yRot() == player.getViewYRot(1f) && camera.xRot() == player.getViewXRot(1f),
                "Explicit standing restores normal camera aim");
        renderState.entityRenderStates.clear();
        extract.invoke(renderer, camera, camera.getCullFrustum(), DeltaTracker.ONE, renderState);
        check(renderState.entityRenderStates.isEmpty(), "Explicit standing restores normal first-person own-body filtering");
        player.pose = Pose.SLEEPING;
        extract.invoke(renderer, camera, camera.getCullFrustum(), DeltaTracker.ONE, renderState);
        check(renderState.entityRenderStates.isEmpty(), "A sleeping pose alone never bypasses the first-person body filter");
        System.out.println("Mushroom client regression: " + checks + " checks passed with production client mixins applied.");
    }

    private static final class OptionsProbe extends Options {
        OptionInstance<Double> distance;
        private OptionsProbe() { super(null, null); }
        @Override public CameraType getCameraType() { return CameraType.FIRST_PERSON; }
        @Override public int getEffectiveRenderDistance() { return 8; }
        @Override public OptionInstance<Double> entityDistanceScaling() { return distance; }
    }
    private static final class Companion extends MushroomGirlEntity {
        byte companionPose;
        private Companion() { super(null, null); }
        @Override public byte getCompanionPose() { return companionPose; }
    }
    private static final class LevelRendererProbe extends LevelRenderer {
        private LevelRendererProbe() { super(null, null, null, null, null, null); }
        @Override protected boolean shouldShowEntityOutlines() { return false; }
        @Override public boolean isSectionCompiledAndVisible(BlockPos pos) { return true; }
    }
    private static final class DispatcherProbe extends EntityRenderDispatcher {
        private DispatcherProbe() { super(null, null, null, null, null, null, null, null, null, null, null); }
        @Override public <E extends Entity> boolean shouldRender(E entity, Frustum frustum, double x, double y, double z) { return true; }
        @Override public <E extends Entity> EntityRenderState extractEntity(E entity, float partialTick) { return new EntityRenderState(); }
    }
    private static final class RenderWorld extends ClientLevel {
        Iterable<Entity> entities; TickRateManager ticks;
        private RenderWorld() { super(null, null, null, null, 0, 0, null, false, 0, 0); }
        @Override public Iterable<Entity> entitiesForRendering() { return entities; }
        @Override public TickRateManager tickRateManager() { return ticks; }
        @Override public boolean isOutsideBuildHeight(int y) { return false; }
    }
    private static final class BedPlayer extends Player implements BedRestingPlayer {
        BedWorld world;
        Optional<BlockPos> sleeping, resting;
        Pose pose;
        private BedPlayer() { super(null, null); }
        @Override public GameType gameMode() { return GameType.SURVIVAL; }
        @Override public Level level() { return world; }
        @Override public float getHealth() { return 20; }
        @Override public boolean hasEffect(Holder<MobEffect> effect) { return false; }
        @Override public Pose getPose() { return pose; }
        @Override public void setPose(Pose pose) { this.pose = pose; }
        @Override public void clearSleepingPos() { sleeping = Optional.empty(); }
        @Override public Optional<BlockPos> getSleepingPos() { return sleeping; }
        @Override public BlockPos blockPosition() { return BlockPos.containing(position()); }
        @Override public void setPos(double x, double y, double z) {
            try { field(this, Entity.class, "position", new Vec3(x, y, z)); }
            catch (Exception e) { throw new AssertionError(e); }
        }
        @Override public void setDeltaMovement(Vec3 velocity) { }
        @Override public void setOnGround(boolean ground) { }
        @Override public Optional<BlockPos> toneko$getRestingBed() { return resting; }
        @Override public void toneko$setRestingBed(Optional<BlockPos> bed) { resting = bed; }
    }
    private static final class BedWorld extends ServerLevel {
        Direction facing;
        private BedWorld() { super(null, null, null, null, null, null, false, 0, java.util.List.of(), false); }
        @Override public boolean isClientSide() { return true; }
        @Override public boolean hasChunkAt(BlockPos pos) { return false; }
        @Override public BlockState getBlockState(BlockPos pos) { return Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing); }
    }
}
