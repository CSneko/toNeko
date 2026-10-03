package org.cneko.toneko.common.mod.entities;

import com.google.common.collect.ImmutableList;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import org.cneko.toneko.common.mod.entities.ai.NekoBrain;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;
import org.cneko.toneko.common.mod.entities.ai.goal.ZombieMushroomTargetGoal;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.JsonConfiguration;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.Vec3;
import sun.misc.Unsafe;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;

/** Native riding calls on server fixtures after Fabric applies the actual production mixin. */
public final class MushroomCompanionRegressionTest {
    private static int checks;
    private static Unsafe fixtures;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void field(Object instance, Class<?> owner, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name); field.setAccessible(true); field.set(instance, value);
    }
    private static MushroomProbe fixture(WorldProbe world, PlayerProbe player) throws Exception {
        var mushroom = (MushroomProbe) fixtures.allocateInstance(MushroomProbe.class);
        field(mushroom, Entity.class, "level", world);
        field(mushroom, Entity.class, "type", EntityType.PIG); // Only vanilla registries are bootstrapped in fixtures.
        field(mushroom, Entity.class, "passengers", ImmutableList.of());
        field(mushroom, Entity.class, "random", RandomSource.create(1));
        field(mushroom, Entity.class, "position", new Vec3(4.5, 64, 0.5));
        mushroom.setBoundingBox(new AABB(4,64,0,5,66,1));
        field(mushroom, Entity.class, "onGround", true);
        var builder = new SynchedEntityData.Builder(mushroom);
        // Entity's constructor defines these before calling the virtual defineSynchedData method.
        for (var entry : Map.<String, Object>of("DATA_SHARED_FLAGS_ID", (byte)0, "DATA_AIR_SUPPLY_ID", 300,
                "DATA_CUSTOM_NAME", Optional.empty(), "DATA_CUSTOM_NAME_VISIBLE", false, "DATA_SILENT", false,
                "DATA_NO_GRAVITY", false, "DATA_POSE", Pose.STANDING, "DATA_TICKS_FROZEN", 0).entrySet()) {
            var data = Entity.class.getDeclaredField(entry.getKey()); data.setAccessible(true);
            @SuppressWarnings("unchecked") var accessor = (EntityDataAccessor<Object>) data.get(null);
            builder.define(accessor, entry.getValue());
        }
        mushroom.defineSynchedData(builder);
        field(mushroom, Entity.class, "entityData", builder.build());
        mushroom.attributes = new AttributeMap(LivingEntity.createLivingAttributes().build());
        mushroom.brain = (BrainProbe) fixtures.allocateInstance(BrainProbe.class);
        mushroom.navigation = (NavigationProbe) fixtures.allocateInstance(NavigationProbe.class);
        mushroom.navigation.owner = mushroom; mushroom.navigation.reachable = true;
        mushroom.look = new LookControl(mushroom);
        mushroom.trust = 60;
        mushroom.health = 20;
        mushroom.visible = true;
        field(mushroom, MushroomGirlEntity.class, "familiarity", new java.util.HashMap<UUID, Integer>());
        field(mushroom, MushroomGirlEntity.class, "affection", new java.util.HashMap<UUID, Integer>());
        field(mushroom, MushroomGirlEntity.class, "nextInteraction", new java.util.HashMap<UUID, Long>());
        field(mushroom, MushroomGirlEntity.class, "unattendedTicks", new java.util.HashMap<UUID, Integer>());
        field(mushroom, MushroomGirlEntity.class, "observedThreats", new java.util.HashMap<>());
        field(mushroom, NekoEntity.class, "owners", new java.util.HashMap<UUID, INeko.Owner>());
        return mushroom;
    }
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        fixtures = (Unsafe) field.get(null);
        var world = (WorldProbe) fixtures.allocateInstance(WorldProbe.class);
        world.blocks = new java.util.HashMap<>();
        var player = (PlayerProbe) fixtures.allocateInstance(PlayerProbe.class);
        field(player, Entity.class, "level", world);
        field(player, Entity.class, "type", EntityType.PLAYER);
        field(player, Entity.class, "passengers", ImmutableList.of());
        field(player, Entity.class, "position", new Vec3(0.5, 64.6875, 0.5));
        player.setUUID(UUID.randomUUID()); player.pose = Pose.SLEEPING;
        player.bed = Optional.of(new BlockPos(0, 64, 0));
        world.facing = Direction.SOUTH; world.players = List.of(player);
        var mushroom = fixture(world, player);
        check(!EntityType.PLAYER.canSerialize(), "The actual player vehicle is not saveable and reproduces the vanilla restriction");
        check(!mushroom.startRiding(player, true, false), "Ordinary unrequested mounts retain the vanilla restriction");
        for (byte mode : new byte[]{MushroomGirlEntity.CARRIED, MushroomGirlEntity.SITTING_ON_PLAYER, MushroomGirlEntity.LYING_ON_PLAYER}) {
            mushroom.mode(mode);
            check(mushroom.startRiding(player, true, false), "Each companion pose can use the real server-side player riding path");
            check(mushroom.getVehicle() == player && player.getPassengers().contains(mushroom), "Native riding establishes both ends of the relationship");
            check(mushroom.shouldBeSaved(), "A player-borne companion remains eligible for the world's entity save");
            var out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            check(mushroom.save(out) && out.buildResult().getBooleanOr("fixture_saved", false), "World entity saving includes the companion independently of player data");
            // Clear native links between cases without invoking world-bound dismount events.
            field(mushroom, Entity.class, "vehicle", null); field(player, Entity.class, "passengers", ImmutableList.of());
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            world.facing = facing;
            for (boolean drowsy : new boolean[]{false, true}) {
                mushroom = fixture(world, player); mushroom.drowsy = drowsy;
                var goal = mushroom.new ComfortPlayerGoal();
                check(goal.canUse(), "A sleeping familiar player is selected before reaching the bed");
                goal.start();
                check(mushroom.brain.destination != null, "Comfort approaches a reachable side of the sleeping player's bed");
                check(mushroom.navigation.candidates.stream().noneMatch(p -> p.equals(player.bed.orElseThrow())),
                        "Bed approach targets safe sides rather than the obstructed bed head");
                check(mushroom.brain.priority == BehaviorPriority.COMPANION && mushroom.brain.speed >= 1.1,
                        "Bedtime approach takes priority over routine movement and has a prompt walking speed");
                field(mushroom, Entity.class, "position", new Vec3(1.5, 64, 0.5));
                goal.tick();
                check(mushroom.getVehicle() == player && mushroom.getCompanionPose() == MushroomGirlEntity.SITTING_ON_PLAYER,
                        "The actual goal joins the bed player and always sits, including when drowsy");
                check(mushroom.synced == player, "Automatic joining synchronizes the carrier's own client");
                check(Math.abs(mushroom.getYRot() - facing.toYRot()) < 1e-4, "The automatic bed seat faces the player's head");
                check(mushroom.getXRot() > 0, "Gaze is computed after positioning the rider and points down to the player");
                check(!goal.canContinueToUse(), "The approach goal finishes after mounting");
                goal.stop();
                field(mushroom, Entity.class, "vehicle", null); field(player, Entity.class, "passengers", ImmutableList.of());
            }
        }
        mushroom = fixture(world, player); mushroom.trust = 59;
        check(!mushroom.new ComfortPlayerGoal().canUse(), "Automatic bed companionship still requires familiarity 60");
        mushroom.trust = 60; player.pose = Pose.STANDING; player.bed = Optional.empty();
        check(!mushroom.new ComfortPlayerGoal().canUse(), "An awake standing player does not trigger automatic companionship");
        player.pose = Pose.SLEEPING;
        check(!mushroom.new ComfortPlayerGoal().canUse(), "Ordinary ground lying still requires familiarity 80");
        mushroom.trust = 80;
        check(mushroom.new ComfortPlayerGoal().canUse(), "A close friend lying on the ground triggers companionship");
        player.bed = Optional.of(new BlockPos(0, 64, 0));
        // At dusk, vanilla permits sleep before the mushroom girl's night activity begins.
        mushroom = fixture(world, player); mushroom.rest(true);
        field(mushroom, Entity.class, "position", new Vec3(7.5, 64, 0.5));
        var duskGoal = mushroom.new ComfortPlayerGoal();
        check(duskGoal.canUse(), "Bedtime within eight blocks can interrupt her remaining daytime rest");
        duskGoal.start();
        check(!mushroom.isResting() && duskGoal.canContinueToUse(), "Starting bedtime companionship wakes her before walking");
        check(duskGoal.requiresUpdateEveryTick(), "Approach runs every tick while the sleeping player is available");
        // Exercise vanilla goal arbitration, including interrupting an already-running routine.
        var selector = new GoalSelector();
        var routine = new RoutineGoal();
        mushroom = fixture(world, player);
        var comfort = mushroom.new ComfortPlayerGoal();
        selector.addGoal(2, routine); selector.addGoal(0, comfort);
        player.pose = Pose.STANDING; player.bed = Optional.empty();
        selector.tick();
        check(routine.started, "A routine movement goal is already running before bedtime");
        player.pose = Pose.SLEEPING; player.bed = Optional.of(new BlockPos(0, 64, 0));
        selector.tick();
        check(routine.stopped && mushroom.brain.destination != null,
                "The native goal selector interrupts routine movement immediately for bedtime");
        // Bedtime wins over a closer player resting on the ground.
        var groundPlayer = (PlayerProbe) fixtures.allocateInstance(PlayerProbe.class);
        field(groundPlayer, Entity.class, "level", world);
        field(groundPlayer, Entity.class, "position", new Vec3(4.5, 64, 1.5));
        field(groundPlayer, Entity.class, "passengers", ImmutableList.of());
        groundPlayer.setUUID(UUID.randomUUID()); groundPlayer.pose = Pose.SLEEPING; groundPlayer.bed = Optional.empty();
        world.players = List.of(groundPlayer, player);
        mushroom = fixture(world, player); mushroom.trust = 80;
        comfort = mushroom.new ComfortPlayerGoal();
        check(comfort.canUse(), "Multiple resting players offer valid companionship targets");
        comfort.start();
        check(mushroom.brain.destination != null && mushroom.brain.approach == null,
                "A sleeping bed player takes precedence over nearer ground rest");
        world.players = List.of(player);
        // Also exercise the shared brain's full arbitration, independently of the goal selector.
        var brain = new NekoBrain(mushroom); brain.enableFullArbitration();
        brain.submitMove(groundPlayer, 1, BehaviorPriority.HIGH, routine); brain.tick();
        brain.submitMove(player, 1.1, BehaviorPriority.COMPANION, comfort); brain.tick();
        check(brain.isMovingTowards(player), "Bedtime movement interrupts HIGH even before its minimum running time elapses");
        for (BehaviorPriority competing : BehaviorPriority.values()) {
            if (competing == BehaviorPriority.COMPANION) continue;
            check(BehaviorPriority.isHigherThan(BehaviorPriority.COMPANION, competing),
                    "Bedtime priority is higher than " + competing);
            brain.reset();
            brain.submitMove(groundPlayer, 1, competing, routine); brain.tick();
            brain.submitMove(player, 1.1, BehaviorPriority.COMPANION, comfort); brain.tick();
            check(brain.isMovingTowards(player), "Bedtime immediately interrupts an active " + competing + " intent");
            brain.submitMove(groundPlayer, 1, competing, routine); brain.tick();
            check(brain.isMovingTowards(player), competing + " cannot take navigation back from bedtime companionship");
            brain.stopMoving(routine);
            check(brain.isMovingTowards(player), "An unrelated stop request cannot cancel the active bedtime approach");
            brain.stopMoving(comfort);
            check(!brain.isMovingTowards(player), "The companion goal can still end its own highest-priority approach");
        }
        field(mushroom, Entity.class, "position", new Vec3(1.5, 64, 0.5));
        comfort.tick();
        mushroom.stopRiding();
        var cooldown = MushroomGirlEntity.class.getDeclaredField("companionCooldown"); cooldown.setAccessible(true);
        check(cooldown.getInt(mushroom) == MushroomRules.COMPANION_COOLDOWN,
                "Asking her to get down still prevents an immediate automatic return");
        check(!comfort.canContinueToUse(), "A previously active approach also respects the manual dismount cooldown");
        cooldown.setInt(mushroom, 0);
        comfort = mushroom.new ComfortPlayerGoal(); check(comfort.canUse(), "A new invitation is eligible once the manual cooldown ends");
        comfort.start();
        player.pose = Pose.STANDING; player.bed = Optional.empty();
        var tickCompanion = MushroomGirlEntity.class.getDeclaredMethod("tickCompanion"); tickCompanion.setAccessible(true);
        tickCompanion.invoke(mushroom);
        check(!mushroom.isPassenger() && cooldown.getInt(mushroom) == 0,
                "Natural standing ends the visit without suppressing the next bedtime");
        field(mushroom, Mob.class, "goalSelector", new GoalSelector());
        mushroom.registerGoals();
        var available = mushroom.getGoalSelector().getAvailableGoals();
        check(available.stream().anyMatch(g -> g.getGoal() instanceof MushroomGirlEntity.ComfortPlayerGoal && g.getPriority() == 2),
                "Comfort follows flotation and timid defense in the actual registration");
        check(available.stream().anyMatch(g -> g.getGoal() instanceof MushroomGirlEntity.TimidDefenseGoal && g.getPriority() == 1),
                "Timid defense interrupts companionship before routine goals");

        player.pose = Pose.SLEEPING; player.bed = Optional.of(new BlockPos(0,64,0));
        field(player, Entity.class, "passengers", ImmutableList.of());
        mushroom = fixture(world, player); mushroom.visible = false; mushroom.rest(true);
        comfort = mushroom.new ComfortPlayerGoal();
        check(comfort.canUse(), "A sleeping friend can be selected around a corner without initial line of sight");
        comfort.start();
        check(!mushroom.isResting() && comfort.canContinueToUse() && mushroom.brain.destination != null,
                "The mushroom wakes and routes to the bed without requiring continuous sight");
        field(mushroom, Entity.class, "position", new Vec3(2.5,64,0.5));
        mushroom.brain.destination = null;
        comfort.tick();
        check(mushroom.getVehicle() == null && mushroom.brain.destination != null,
                "A close failed join continues navigating instead of freezing beside the bed");
        mushroom.visible = true;
        comfort.tick();
        check(mushroom.getVehicle() == player, "A reachable bed side more than 1.8 blocks from the head can actually mount");
        mushroom.stopRiding();
        check(!mushroom.new ComfortPlayerGoal().canUse(), "Manual dismissal is respected during the same bed rest");
        var tickCooldown = MushroomGirlEntity.class.getDeclaredMethod("tickCompanionCooldown"); tickCooldown.setAccessible(true);
        player.pose = Pose.STANDING; player.bed = Optional.empty(); tickCooldown.invoke(mushroom);
        player.pose = Pose.SLEEPING; player.bed = Optional.of(new BlockPos(0,64,0));
        check(mushroom.new ComfortPlayerGoal().canUse(), "The next bedtime does not inherit the previous visit's manual cooldown");

        mushroom = fixture(world, player); mushroom.navigation.reachable = false;
        comfort = mushroom.new ComfortPlayerGoal(); check(comfort.canUse(), "Bed search can begin before reachability is known");
        comfort.start();
        check(!comfort.canContinueToUse() && !comfort.canUse(), "An unreachable bed yields control and waits before retrying");
        mushroom.tickCount = 100; mushroom.navigation.reachable = true;
        check(comfort.canUse(), "An unreachable bed is retried after its short path cooldown");
        // Native block ray casts exclude bed sides that are behind a solid wall.
        world.blocks.put(new BlockPos(1,64,0), Blocks.STONE.defaultBlockState());
        world.blocks.put(new BlockPos(1,65,0), Blocks.STONE.defaultBlockState());
        comfort.start();
        check(!mushroom.navigation.candidates.contains(new BlockPos(2,64,0)),
                "Native collision rays never choose a seat approach through a wall");
        world.blocks.clear();

        // Use production relationship bookkeeping, data synchronization and state persistence.
        mushroom = fixture(world, player); mushroom.actualRelationships = true;
        var state = new CompoundTag(); var scores = new CompoundTag(); scores.putInt(player.getUUID().toString(),99);
        state.put("Familiarity",scores); mushroom.readMushroomState(state);
        world.time = 1000; mushroom.recordInteraction(player,4);
        check(mushroom.getFamiliarity(player.getUUID()) == 100 && mushroom.getAffection(player.getUUID()) == 0,
                "Existing familiarity progresses into the new stage without granting early affection");
        mushroom.recordInteraction(player,15);
        check(mushroom.getAffection(player.getUUID()) == 0, "Interaction cooldown also limits affection farming");
        world.time += 100; mushroom.recordInteraction(player,15);
        check(mushroom.getAffection(player.getUUID()) == 5, "A later coffee interaction raises the actual stored affection");
        world.time += 100; mushroom.recordInteraction(groundPlayer,4);
        check(mushroom.getFamiliarity(groundPlayer.getUUID()) == 4 && mushroom.getAffection(groundPlayer.getUUID()) == 0,
                "A second player has independent familiarity and locked affection");
        world.client = true;
        check(mushroom.getFamiliarity(player.getUUID()) == 100 && mushroom.getAffection(player.getUUID()) == 5,
                "The actual synchronized score strings expose both stages to the client");
        world.client = false;
        var stateOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        stateOutput.store("MushroomState",CompoundTag.CODEC,mushroom.saveMushroomState());
        var decoded = TagValueInput.create(ProblemReporter.DISCARDING,RegistryAccess.EMPTY,stateOutput.buildResult())
                .read("MushroomState",CompoundTag.CODEC).orElseThrow();
        var loaded = fixture(world,player); loaded.actualRelationships = true; loaded.readMushroomState(decoded);
        check(loaded.getAffection(player.getUUID()) == 5 && loaded.getFamiliarity(groundPlayer.getUUID()) == 4,
                "Native save codecs retain affection and each player's familiarity on reload");
        var legacy = decoded.copy(); legacy.remove("Affection"); loaded.readMushroomState(legacy);
        check(loaded.getFamiliarity(player.getUUID()) == 100 && loaded.getAffection(player.getUUID()) == 0,
                "Old saves retain full familiarity and begin affection at zero");
        mushroom.recordInteraction(player,-15);
        check(mushroom.getAffection(player.getUUID()) == 0 && mushroom.getFamiliarity(player.getUUID()) == 90,
                "Harm is never blocked by positive interaction cooldown and consumes affection before familiarity");

        // High-affection visiting is independent of explicit follow and lower priority than bedtime.
        player.pose = Pose.STANDING; player.bed = Optional.empty();
        scores.putInt(player.getUUID().toString(),100);
        var affectionate = new CompoundTag(); affectionate.putInt(player.getUUID().toString(),80);
        state.put("Affection",affectionate); loaded.readMushroomState(state);
        var linger = loaded.new LingerWithFriendGoal();
        check(linger.canUse(), "High affection starts an unsolicited visit without a follow command");
        linger.start();
        check(loaded.brain.approach == player && loaded.brain.priority == BehaviorPriority.NORMAL,
                "A visit approaches the liked player using ordinary navigation priority");
        loaded.visible = false;
        check(linger.canContinueToUse(), "A visit persists through brief loss of sight instead of leaving immediately");
        loaded.visible = true;
        field(loaded,Entity.class,"position",player.position().add(2,0,0)); linger.tick();
        check(loaded.brain.approach == null && linger.canContinueToUse(), "A visit waits near the player without crowding their body");
        loaded.tickCount = MushroomRules.lingerDuration(80);
        check(!linger.canContinueToUse(), "A visit ends after its affection-dependent duration");
        linger.stop(); check(!loaded.new LingerWithFriendGoal().canUse(), "Visits pause between episodes rather than permanently following");
        field(loaded,MushroomGirlEntity.class,"lingerCooldown",0); loaded.tickCount = 0;
        loaded.followOwner(player,32,1);
        check(!loaded.new LingerWithFriendGoal().canUse(), "Explicit following takes precedence over spontaneous visiting");
        field(loaded,Mob.class,"goalSelector",new GoalSelector()); loaded.registerGoals();
        var followGoal = loaded.getGoalSelector().getAvailableGoals().stream().filter(g -> g.getPriority() == 4)
                .findFirst().orElseThrow().getGoal();
        check(followGoal.canUse(), "An affectionate explicit follower keeps its movement goal while already close");
        followGoal.tick();
        check(loaded.brain.approach == null,
                "An affectionate follower waits nearby instead of yielding to random wandering");
        loaded.stopFollowing();
        check(!followGoal.canContinueToUse(), "Stopping following releases the affectionate wait too");
        check(!loaded.new LingerWithFriendGoal().canUse(), "Stopping following also pauses unsolicited visits");
        state.putBoolean("Staying",true); loaded.readMushroomState(state);
        check(!loaded.new LingerWithFriendGoal().canUse(), "An explicit stay command also prevents unsolicited visits");
        state.putBoolean("Staying",false); loaded.readMushroomState(state);
        field(loaded,Entity.class,"position",new Vec3(4.5,64,0.5));
        selector = new GoalSelector();
        selector.addGoal(4,loaded.new LingerWithFriendGoal()); selector.addGoal(0,loaded.new ComfortPlayerGoal());
        selector.tick(); check(loaded.brain.approach == player, "The goal selector begins a normal affection visit");
        player.pose = Pose.SLEEPING; player.bed = Optional.of(new BlockPos(0,64,0));
        selector.tick();
        check(loaded.brain.destination != null && loaded.brain.priority == BehaviorPriority.COMPANION,
                "Bedtime immediately interrupts an ongoing affection visit despite the visit's stop cooldown");
        field(loaded,MushroomGirlEntity.class,"lingerCooldown",0); loaded.rest(true);
        check(!loaded.new LingerWithFriendGoal().canUse(), "Affection does not erase daytime mushroom physiology");
        checkTimidReactions(world, player);
        checkProjectileAim();
        checkGrowth(world, player);
        checkTemporaryBehavior(world, player);
        System.out.println("Mushroom companion regression: " + checks + " checks passed with production riding, projectile and zombie targeting mixins applied.");
    }

    private static void checkTimidReactions(WorldProbe world, PlayerProbe player) throws Exception {
        player.pose = Pose.STANDING; player.bed = Optional.empty();
        field(player, Entity.class, "passengers", ImmutableList.of());
        var mushroom = fixture(world, player);
        var monster = (MonsterProbe) fixtures.allocateInstance(MonsterProbe.class);
        monster.living = true; monster.setUUID(UUID.randomUUID());
        field(monster, Entity.class, "position", new Vec3(6.5,64,0.5));
        monster.setBoundingBox(new AABB(6,64,0,7,66,1));
        world.monsters = List.of(monster); mushroom.tickCount = 5;
        mushroom.tickThreats();
        var defense = mushroom.new TimidDefenseGoal();
        check(defense.canUse(), "A nearby visible monster triggers defense before harming her");
        defense.start(); defense.tick();
        check(mushroom.isCowering() && !mushroom.canMove(), "Healthy threatened mushrooms stop and cower");
        check(mushroom.defendedAgainst == monster, "Cowering directs defensive spores at the observed threat");
        check(!mushroom.new ComfortPlayerGoal().canUse(), "Danger prevents new bed visits");

        mushroom.health = 5;
        var corner = new BlockPos(2,64,-2);
        for (int y = 64; y <= 65; y++) {
            world.blocks.put(new BlockPos(3,y,-2), Blocks.STONE.defaultBlockState());
            world.blocks.put(new BlockPos(2,y,-1), Blocks.STONE.defaultBlockState());
        }
        check(mushroom.cornerWalls(corner) == 4, "Two perpendicular walls receive the corner preference");
        defense.tick();
        check(mushroom.brain.destination != null && mushroom.brain.priority == BehaviorPriority.CRITICAL,
                "Low health seeks reachable cover even during defense");
        check(mushroom.brain.destination.equals(Vec3.atBottomCenterOf(corner)), "Refuge chooses the screened corner over exposed ground");
        field(mushroom, Entity.class, "position", mushroom.brain.destination);
        defense.tick();
        check(mushroom.isHiding() && !mushroom.canMove(), "At the refuge she crouches and trembles without drifting");
        monster.living = false;
        monster.damage = new DamageSource(Holder.direct(new DamageType("fixture",0)), player);
        mushroom.tickCount = 10; mushroom.tickThreats();
        check((int)get(mushroom,MushroomGirlEntity.class,"admirationTicks") == MushroomRules.ADMIRATION_TICKS,
                "The player who defeats the tracked threat earns admiration");
        defense.stop(); mushroom.health = 20; mushroom.tickEmotions();
        check(mushroom.getEmotion() == MushroomGirlEntity.ADMIRING, "Admiration becomes visible after immediate danger ends");
        mushroom.tickThreats();
        check((int)get(mushroom,MushroomGirlEntity.class,"admirationTicks") < MushroomRules.ADMIRATION_TICKS,
                "The same dead threat cannot repeatedly reward admiration");

        var neglected = fixture(world,player); neglected.actualRelationships = true;
        var state = new CompoundTag(); var scores = new CompoundTag(); var affection = new CompoundTag(); var unattended = new CompoundTag();
        scores.putInt(player.getUUID().toString(),100); affection.putInt(player.getUUID().toString(),21);
        unattended.putInt(player.getUUID().toString(),MushroomRules.NEGLECT_TICKS - 1);
        state.put("Familiarity",scores); state.put("Affection",affection); state.put("Unattended",unattended);
        neglected.readMushroomState(state); neglected.tickEmotions();
        check(neglected.getEmotion() == MushroomGirlEntity.DISPLEASED, "Five minutes nearby with affection above 20 shows displeasure");
        neglected.recordInteraction(player,4);
        check(neglected.getEmotion() == MushroomGirlEntity.NEUTRAL, "An actual interaction immediately clears neglect");
        field(neglected,MushroomGirlEntity.class,"nextInteraction",new java.util.HashMap<UUID,Long>());
        field(neglected,MushroomGirlEntity.class,"unattendedTicks",new java.util.HashMap<>(Map.of(player.getUUID(),6000)));
        world.players = List.of(); neglected.tickEmotions();
        check(neglected.getEmotion() == MushroomGirlEntity.NEUTRAL, "An absent player does not cause a displeased expression");
        var saved = neglected.saveMushroomState(); var restored = fixture(world,player); restored.actualRelationships = true;
        restored.readMushroomState(saved); world.players = List.of(player); restored.tickEmotions();
        check(restored.getEmotion() == MushroomGirlEntity.DISPLEASED, "Neglect timing survives saving and loading");

        var visitor = fixture(world,player); visitor.mode(MushroomGirlEntity.SITTING_ON_PLAYER);
        field(visitor,Entity.class,"vehicle",player); field(visitor,MushroomGirlEntity.class,"bedSleepVisit",true);
        player.bed = Optional.empty(); player.restingBed = Optional.of(new BlockPos(0,64,0)); player.pose = Pose.SLEEPING; visitor.looking = true;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            world.facing = facing;
            field(visitor,Entity.class,"position",player.position().add(-facing.getStepX()*0.9,0.22,-facing.getStepZ()*0.9));
            player.setYRot(facing.toYRot()+45); // Mouse yaw differs from the existing fixed bed camera.
            player.setXRot(-27);
            check(visitor.actualGaze(player), "Awake gaze matches the mattress camera for every bed orientation");
            player.setXRot(30);
            check(!visitor.actualGaze(player), "Looking down at the mattress does not count as watching her face");
        }
        world.facing = Direction.SOUTH;
        player.setXRot(0);
        for(int i=0;i<200;i++) visitor.tickEmotions();
        check(visitor.getEmotion() != MushroomGirlEntity.BLUSHING, "The awake bed visit waits more than ten seconds of continuous gaze");
        int steamBefore = world.steamPuffs;
        visitor.tickEmotions(); check(visitor.getEmotion() == MushroomGirlEntity.BLUSHING, "Continued gaze after waking makes her blush");
        check(world.steamPuffs > steamBefore, "Blushing emits the sparse purple steam particles");
        visitor.looking = false; visitor.tickEmotions();
        check((int)get(visitor,MushroomGirlEntity.class,"gazeTicks") == 0, "Looking away clears the gaze counter");
        for(int i=0;i<60;i++) visitor.tickEmotions();
        check(visitor.getEmotion() != MushroomGirlEntity.BLUSHING, "Blushing fades after the gaze stops");
        visitor.looking = true; player.bed = player.restingBed; player.restingBed = Optional.empty(); player.pose = Pose.SLEEPING;
        for(int i=0;i<220;i++) visitor.tickEmotions();
        check(visitor.getEmotion() != MushroomGirlEntity.BLUSHING, "Sleeping players cannot trigger awake gaze blushing");
        world.monsters = List.of(); world.blocks.clear(); player.pose = Pose.STANDING; player.bed = Optional.empty();
    }

    private static Object get(Object object,Class<?> owner,String name) throws Exception {
        var field=owner.getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }

    private static void checkProjectileAim() throws Exception {
        var projectile = (ProjectileProbe) fixtures.allocateInstance(ProjectileProbe.class);
        var shooter = (ShooterProbe) fixtures.allocateInstance(ShooterProbe.class);
        var previous = ToNekoEffects.HALLUCINATION;
        // A vanilla registered holder substitutes for the custom registry entry in this isolated bootstrap.
        ToNekoEffects.HALLUCINATION = MobEffects.NAUSEA.value();
        projectile.owner = shooter;
        projectile.shoot(0,0,1,1.5f,1);
        check(projectile.inaccuracy == 1 && projectile.speed == 1.5f, "Normal projectile aim and speed remain unchanged");
        shooter.effect = new MobEffectInstance(MobEffects.NAUSEA,200,0);
        projectile.shoot(0,0,1,1.5f,1);
        check(projectile.inaccuracy == 9 && projectile.speed == 1.5f, "The production projectile mixin widens affected mobs' aim without changing speed");
        var player = (PlayerProbe) fixtures.allocateInstance(PlayerProbe.class);
        player.effect = shooter.effect; projectile.owner = player;
        projectile.shoot(0,0,1,1.5f,1);
        check(projectile.inaccuracy == 9 && projectile.speed == 1.5f, "Affected players use the same real projectile dispersion");
        projectile.owner = shooter;
        shooter.effect = new MobEffectInstance(MobEffects.NAUSEA,200,1);
        projectile.shoot(0,0,1,1.5f,1);
        check(projectile.inaccuracy == 17, "Stronger hallucination increases real projectile dispersion");
        projectile.owner = null; projectile.shoot(0,0,1,1.5f,1);
        check(projectile.inaccuracy == 1, "Ownerless projectiles retain their normal accuracy");
        var victim = (MonsterProbe) fixtures.allocateInstance(MonsterProbe.class); victim.setUUID(UUID.randomUUID());
        var friend = (MonsterProbe) fixtures.allocateInstance(MonsterProbe.class); friend.setUUID(UUID.randomUUID());
        field(projectile,MushroomSporeProjectile.class,"intendedVictim",victim.getUUID());
        check(projectile.canHit(victim) && !projectile.canHit(friend), "Traveling defensive spores pass through bystanders and hit only the intended threat");
        ToNekoEffects.HALLUCINATION = previous;
    }
    private static void advanceGrowth(MushroomProbe mushroom, int ticks) {
        for (int i = 0; i < ticks; i++) mushroom.tickGrowth();
    }

    private static void checkGrowth(WorldProbe world, PlayerProbe player) throws Exception {
        world.players = List.of(); world.dayTime = 6000;
        var mushroom = fixture(world, player);
        check(mushroom.getMushroomLevel() == 1 && mushroom.getMaxHealth() == 20
                && mushroom.getSporeDamage() == 3, "New mushrooms use the requested base health and damage");
        check(mushroom.getPassiveHealAmount() == 0, "The inherited unconditional healing cannot bypass sunlight rules");
        mushroom.health = 10;
        field(mushroom, MushroomGirlEntity.class, "shaded", true);
        advanceGrowth(mushroom, 599);
        check(mushroom.health == 10, "Shaded healing waits the entire thirty-second interval");
        advanceGrowth(mushroom, 1);
        check(mushroom.health == 10.5f, "Daytime shade restores half a health point at level one");
        field(mushroom, MushroomGirlEntity.class, "shaded", false);
        advanceGrowth(mushroom, 1200);
        check(mushroom.health == 10.5f, "Exposed daylight does not restore health");
        world.dayTime = 18000;
        advanceGrowth(mushroom, 600);
        check(mushroom.health == 11, "Nighttime permits natural recovery without shade");
        mushroom.burning = true;
        advanceGrowth(mushroom, 600);
        check(mushroom.health == 11, "Burning interrupts natural recovery");
        mushroom.burning = false;
        field(mushroom, MushroomGirlEntity.class, "regenerationDelay", 200);
        advanceGrowth(mushroom, 798);
        check(mushroom.health == 11, "The post-damage delay postpones a complete healing interval");
        advanceGrowth(mushroom, 1);
        check(mushroom.health == 11.5f, "Recovery resumes after the damage delay and healing interval");

        mushroom.addGrowthExperience(99);
        check(mushroom.getMushroomLevel() == 1 && mushroom.getLevelExperience() == 99,
                "Progress below the upgrade threshold retains the first level");
        mushroom.addGrowthExperience(1);
        check(mushroom.getMushroomLevel() == 2 && mushroom.getNekoLevel() == 2 && mushroom.getMaxHealth() == 22
                && mushroom.getLevelExperience() == 0, "Reaching the threshold updates both synchronized level and maximum health");
        check(mushroom.health == 11.5f && Math.abs(mushroom.getSporeDamage() - 3.2f) < 0.001f,
                "Leveling raises damage without instantly refilling health");
        mushroom.updateNekoLevelModifiers(); mushroom.updateNekoLevelModifiers();
        check(mushroom.getMaxHealth() == 22, "Repeated level updates never stack maximum-health bonuses");
        advanceGrowth(mushroom, 600);
        check(Math.abs(mushroom.health - 12.1f) < 0.001f, "A higher level restores more health in the same interval");
        mushroom.health = 21.9f; advanceGrowth(mushroom, 600);
        check(mushroom.health == 22, "Natural recovery never exceeds maximum health");

        var companion = fixture(world, player);
        world.players = List.of(player);
        advanceGrowth(companion, 599);
        check(companion.getGrowthExperience() == 0, "Companionship requires a full thirty seconds");
        advanceGrowth(companion, 1);
        check(companion.getGrowthExperience() == 1, "A visible familiar player nearby slowly grants experience");
        companion.trust = 59; advanceGrowth(companion, 600);
        check(companion.getGrowthExperience() == 1, "Strangers do not grant passive companionship experience");
        companion.trust = 60; companion.visible = false; advanceGrowth(companion, 600);
        check(companion.getGrowthExperience() == 1, "A player behind a wall does not count as companionship");
        companion.visible = true; companion.setPos(20,64,0); advanceGrowth(companion, 600);
        check(companion.getGrowthExperience() == 1, "Distant players do not grant companionship experience");
        companion.setPos(4.5,64,0.5);
        world.players = List.of(player, player); advanceGrowth(companion, 600);
        check(companion.getGrowthExperience() == 2, "Additional nearby players do not multiply the experience rate");
        world.players = List.of(); advanceGrowth(companion, 600);
        check(companion.getGrowthExperience() == 2, "Unaccompanied or unloaded time does not grant passive experience");

        var fighter = fixture(world, player);
        var victim = (MonsterProbe) fixtures.allocateInstance(MonsterProbe.class);
        victim.setUUID(UUID.randomUUID()); victim.acceptsDamage = true;
        var projectile = (ProjectileProbe) fixtures.allocateInstance(ProjectileProbe.class);
        field(projectile, Entity.class, "level", world);
        field(projectile, MushroomSporeProjectile.class, "intendedVictim", victim.getUUID());
        field(projectile, MushroomSporeProjectile.class, "damage", fighter.getSporeDamage());
        projectile.owner = fighter;
        projectile.sources = (DamageSourcesProbe) fixtures.allocateInstance(DamageSourcesProbe.class);
        projectile.impact(victim);
        check(victim.receivedDamage == 3 && fighter.getGrowthExperience() == 1,
                "The real spore impact deals three health points and grants combat experience");
        projectile.impact(victim);
        check(fighter.getGrowthExperience() == 1, "Rapid damaging hits respect the combat experience cooldown");
        advanceGrowth(fighter, 100); victim.acceptsDamage = false;
        projectile.impact(victim);
        check(fighter.getGrowthExperience() == 1, "Blocked or invulnerable hits do not grant experience");
        victim.acceptsDamage = true; projectile.impact(victim);
        check(fighter.getGrowthExperience() == 2, "A later successful hostile hit grants the next experience point");
        fighter.addGrowthExperience(98);
        field(projectile, MushroomSporeProjectile.class, "damage", fighter.getSporeDamage());
        projectile.impact(victim);
        check(Math.abs(victim.receivedDamage - 3.2f) < 0.001f, "The real impact uses the shooter's increased spore damage");
        float damageBefore = victim.receivedDamage;
        field(projectile, MushroomSporeProjectile.class, "intendedVictim", UUID.randomUUID());
        projectile.impact(victim);
        check(victim.receivedDamage == damageBefore && fighter.getGrowthExperience() == 100,
                "An unintended victim neither takes damage nor grants experience");

        field(fighter, MushroomGirlEntity.class, "regenerationTicks", 345);
        field(fighter, MushroomGirlEntity.class, "regenerationDelay", 120);
        field(fighter, MushroomGirlEntity.class, "companionshipTicks", 234);
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.store("MushroomState", CompoundTag.CODEC, fighter.saveMushroomState());
        var saved = TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, output.buildResult())
                .read("MushroomState", CompoundTag.CODEC).orElseThrow();
        var loaded = fixture(world, player); loaded.readMushroomState(saved);
        check(loaded.getGrowthExperience() == 100 && loaded.getMushroomLevel() == 2 && loaded.getMaxHealth() == 22,
                "The native storage codec restores experience, level and maximum health");
        check((int)get(loaded, MushroomGirlEntity.class, "regenerationTicks") == 345
                && (int)get(loaded, MushroomGirlEntity.class, "regenerationDelay") == 120
                && (int)get(loaded, MushroomGirlEntity.class, "companionshipTicks") == 234,
                "A save preserves partial recovery, damage delay and companionship progress");
        loaded.readMushroomState(new CompoundTag());
        check(loaded.getMushroomLevel() == 1 && loaded.getMaxHealth() == 20, "Old saves initialize with the first growth level");
        saved.putInt("GrowthExperience", Integer.MAX_VALUE); loaded.readMushroomState(saved);
        check(loaded.getMushroomLevel() == 30 && loaded.getMaxHealth() == 78,
                "Oversized saved experience is bounded at the final level");
        saved.putInt("GrowthExperience", -100); loaded.readMushroomState(saved);
        check(loaded.getGrowthExperience() == 0 && loaded.getMushroomLevel() == 1,
                "Invalid negative experience cannot break level loading");
        world.players = List.of(player); world.dayTime = 0;
    }

    private static void checkTemporaryBehavior(WorldProbe world, PlayerProbe player) throws Exception {
        var previousConfig = ConfigUtil.CONFIG;
        try {
            ConfigUtil.CONFIG = new JsonConfiguration("{}");
            var mushroom = fixture(world, player);
            Component original = Component.translatable("name.toneko.mushroom_girl.yelu_zi");
            Component fixed = Component.translatable("name.toneko.mushroom_girl.ziye_bai");
            mushroom.setCustomName(original);
            mushroom.setNickName("nickname");
            mushroom.updateConfiguredName();
            check(fixed.equals(mushroom.getCustomName()) && mushroom.getNickName().isEmpty(),
                    "Default name lock synchronizes the actual name and suppresses nickname overrides");
            mushroom.updateConfiguredName();
            var out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            out.store("MushroomState", CompoundTag.CODEC, mushroom.saveMushroomState());
            var saved = TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, out.buildResult())
                    .read("MushroomState", CompoundTag.CODEC).orElseThrow();
            var loaded = fixture(world, player);
            loaded.setCustomName(fixed); loaded.readMushroomState(saved);
            ConfigUtil.CONFIG.set("mushroom_girl.fixed_name", false);
            loaded.updateConfiguredName(); mushroom.updateConfiguredName();
            check(original.equals(loaded.getCustomName()) && original.equals(mushroom.getCustomName()),
                    "Disabling the name lock restores original names after repeated updates and a native save round trip");
            check(mushroom.getNickName().equals("nickname"), "Disabling the lock also restores the original nickname");
            var oldSave = fixture(world, player); oldSave.setCustomName(original);
            ConfigUtil.CONFIG.set("mushroom_girl.fixed_name", true); oldSave.readMushroomState(new CompoundTag());
            ConfigUtil.CONFIG.set("mushroom_girl.fixed_name", false); oldSave.updateConfiguredName();
            check(original.equals(oldSave.getCustomName()), "Old saves without name backups keep their original names");
            check(!Messaging.canSpeak(mushroom), "Mushroom speech defaults to muted");
            // These calls must return before formatting or touching a network connection.
            Messaging.sendNekoChat(null, mushroom, "muted reply");
            Messaging.sendNekoChatInRange(null, mushroom, "muted broadcast", 64);
            Messaging.modifyAndSendMessage(mushroom, "muted interaction", null);
            Messaging.modifyAndSendMessageToAll(mushroom, "muted interaction broadcast");
            var neko = (CrystalNekoEntity) fixtures.allocateInstance(CrystalNekoEntity.class);
            check(Messaging.canSpeak(neko), "The mushroom mute switch does not silence other nekos");
            ConfigUtil.CONFIG.set("mushroom_girl.messages.enable", true);
            check(Messaging.canSpeak(mushroom), "The same entity can speak again when the switch is enabled");

            var zombie = (TargetingZombieProbe) fixtures.allocateInstance(TargetingZombieProbe.class);
            field(zombie, Entity.class, "level", world);
            field(zombie, Entity.class, "type", EntityType.ZOMBIE);
            field(zombie, Entity.class, "position", new Vec3(0.5, 64, 0.5));
            field(zombie, Entity.class, "random", RandomSource.create(1));
            field(zombie, Mob.class, "goalSelector", new GoalSelector());
            field(zombie, Mob.class, "targetSelector", new GoalSelector());
            field(zombie, Mob.class, "navigation", fixtures.allocateInstance(NavigationProbe.class));
            zombie.setBoundingBox(new AABB(0, 64, 0, 1, 66, 1));
            zombie.attributes = new AttributeMap(Zombie.createAttributes().build());
            zombie.sensing = new Sensing(zombie); zombie.visible = true;
            zombie.installNativeGoals();
            var installed = zombie.targets().getAvailableGoals().stream()
                    .filter(g -> g.getGoal() instanceof ZombieMushroomTargetGoal).toList();
            check(installed.size() == 1 && installed.getFirst().getPriority() == 1,
                    "The production mixin adds exactly one mushroom target goal at priority 1");
            check(zombie.targets().getAvailableGoals().stream()
                    .filter(g -> g.getGoal() instanceof NearestAttackableTargetGoal && !(g.getGoal() instanceof ZombieMushroomTargetGoal))
                    .allMatch(g -> g.getPriority() > installed.getFirst().getPriority()),
                    "Mushroom targeting precedes vanilla player and village targets");
            var goal = (ZombieMushroomTargetGoal) installed.getFirst().getGoal();
            world.mushrooms = List.of(mushroom);
            check(goal.canUse(), "A visible mushroom in vanilla follow range is found by the actual native targeting code");
            zombie.visible = false; zombie.sensing.tick();
            check(!goal.canUse(), "Mushroom targeting does not see through walls");
            zombie.visible = true; zombie.sensing.tick();
            zombie.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(2);
            check(!goal.canUse(), "Reduced vanilla follow range also limits mushroom targeting");
            zombie.attributes.getInstance(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(35);
            world.mushrooms = List.of();
            var selector = new GoalSelector();
            selector.addGoal(2, new Goal() {
                { setFlags(java.util.EnumSet.of(Flag.TARGET)); }
                @Override public boolean canUse() { return true; }
                @Override public void start() { zombie.setTarget(player); }
            });
            selector.addGoal(1, goal); selector.tick();
            check(zombie.getTarget() == player, "Vanilla-priority targets remain usable when no mushroom is available");
            world.mushrooms = List.of(mushroom); zombie.sensing.tick(); selector.tick();
            check(zombie.getTarget() == mushroom, "The native goal selector switches from a player target to a nearby mushroom");
            ConfigUtil.CONFIG.set("mushroom_girl.zombie_targeting", false);
            check(!goal.canUse() && !goal.canContinueToUse(), "Disabling targeting prevents new starts and stops an existing pursuit");
            selector.tick();
            check(zombie.getTarget() == player, "Disabling targeting releases the target flag to vanilla-priority goals");
        } finally {
            ConfigUtil.CONFIG = previousConfig;
            world.mushrooms = List.of();
        }
    }

    private static final class TargetingZombieProbe extends Zombie {
        AttributeMap attributes;
        Sensing sensing;
        LivingEntity target;
        boolean visible;
        private TargetingZombieProbe() { super(null, null); }
        void installNativeGoals() { super.registerGoals(); }
        GoalSelector targets() { return targetSelector; }
        @Override public AttributeMap getAttributes() { return attributes; }
        @Override public Sensing getSensing() { return sensing; }
        @Override public LivingEntity getTarget() { return target; }
        @Override public void setTarget(LivingEntity value) { target = value; }
        @Override public boolean hasLineOfSight(Entity entity) { return visible; }
        @Override public boolean canAttack(LivingEntity entity) { return entity.isAlive(); }
        @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return null; }
        @Override public double getEyeY() { return getY() + 1.6; }
    }

    private static final class MushroomProbe extends MushroomGirlEntity {
        int trust;
        boolean drowsy;
        boolean visible, actualRelationships, burning;
        AttributeMap attributes;
        BrainProbe brain;
        NavigationProbe navigation;
        LookControl look;
        ServerPlayer synced;
        float health;
        LivingEntity defendedAgainst;
        boolean looking;
        private MushroomProbe() { super(null, null); }
        @SuppressWarnings("unchecked") void mode(byte mode) throws Exception {
            var field = MushroomGirlEntity.class.getDeclaredField("COMPANION"); field.setAccessible(true);
            getEntityData().set((EntityDataAccessor<Byte>) field.get(null), mode);
        }
        @SuppressWarnings("unchecked") void rest(boolean rest) throws Exception {
            var field = MushroomGirlEntity.class.getDeclaredField("RESTING"); field.setAccessible(true);
            getEntityData().set((EntityDataAccessor<Boolean>) field.get(null), rest);
        }
        @Override public double getEyeY() { return getY() + 0.735; }
        @Override public int getFamiliarity(UUID player) { return actualRelationships ? super.getFamiliarity(player) : trust; }
        @Override public boolean isDrowsy() { return drowsy; }
        @Override public boolean isAlive() { return true; }
        @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return null; }
        @Override public float getHealth() { return health; }
        @Override public void setHealth(float value) { health = Math.clamp(value, 0, getMaxHealth()); }
        @Override public boolean isLookingAtFace(net.minecraft.world.entity.player.Player player) { return looking; }
        boolean actualGaze(net.minecraft.world.entity.player.Player player) { return super.isLookingAtFace(player); }
        @Override void shootDefensiveSpore(ServerLevel server, LivingEntity target) { defendedAgainst = target; }
        @Override public void stopTriggeredAnim(String controller, String animation) { }
        @Override public boolean isInWater() { return false; }
        @Override public boolean isOnFire() { return burning; }
        @Override public net.minecraft.world.item.ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public boolean hasLineOfSight(Entity entity) { return visible; }
        @Override public AttributeMap getAttributes() { return attributes; }
        @Override public NekoBrain getNekoBrain() { return brain; }
        @Override public NavigationProbe getNavigation() { return navigation; }
        @Override public LookControl getLookControl() { return look; }
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { }
        @Override public void refreshDimensions() { }
        @Override public BlockPos blockPosition() { return BlockPos.containing(position()); }
        @Override public void setPose(Pose pose) { }
        @Override public void setPos(double x, double y, double z) {
            try { field(this, Entity.class, "position", new Vec3(x, y, z)); }
            catch (Exception e) { throw new AssertionError(e); }
        }
        @Override public void dismountTo(double x, double y, double z) { setPos(x, y, z); }
        @Override protected void syncCompanionTo(ServerPlayer player) { synced = player; }
        @Override public void saveWithoutId(ValueOutput out) { out.putBoolean("fixture_saved", true); }
    }
    private static final class PlayerProbe extends ServerPlayer implements org.cneko.toneko.common.mod.api.BedRestingPlayer {
        Pose pose;
        Optional<BlockPos> bed;
        Optional<BlockPos> restingBed;
        MobEffectInstance effect;
        private PlayerProbe() { super(null, null, null, null); }
        @Override public boolean isAlive() { return true; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isInWater() { return false; }
        @Override public boolean isOnFire() { return false; }
        @Override public Pose getPose() { return pose; }
        @Override public boolean isDescending() { return false; }
        @Override public MobEffectInstance getEffect(Holder<MobEffect> holder) { return effect; }
        @Override public net.minecraft.world.item.ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public BlockPos blockPosition() { return BlockPos.containing(position()); }
        @Override public double getEyeY() { return getY() + 0.2; }
        @Override public Optional<BlockPos> getSleepingPos() { return bed; }
        @Override public Optional<BlockPos> toneko$getRestingBed() { return restingBed == null ? Optional.empty() : restingBed; }
        @Override public void toneko$setRestingBed(Optional<BlockPos> bed) { restingBed = bed; }
        @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return position().add(0, 1.6, 0); }
        @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return position().add(1, 0, 0); }
        @Override public void sendSystemMessage(Component message) { }
    }
    private static final class WorldProbe extends ServerLevel {
        Direction facing;
        List<ServerPlayer> players;
        Map<BlockPos,BlockState> blocks;
        long time, dayTime;
        boolean client;
        List<MonsterProbe> monsters = List.of();
        List<MushroomProbe> mushrooms = List.of();
        int steamPuffs;
        private WorldProbe() { super(null, null, null, null, null, null, false, 0, java.util.List.of(), false); }
        @Override public boolean isClientSide() { return client; }
        @Override public long getGameTime() { return time; }
        @Override public long getOverworldClockTime() { return dayTime; }
        @Override public List<ServerPlayer> players() { return players; }
        @Override public boolean isLoaded(BlockPos pos) { return true; }
        @Override public boolean canSeeSky(BlockPos pos) { return false; }
        @Override public int getMaxLocalRawBrightness(BlockPos pos) { return 0; }
        @Override public boolean noCollision(Entity entity, AABB bounds) { return true; }
        @Override public <T extends net.minecraft.core.particles.ParticleOptions> int sendParticles(T particle,double x,double y,double z,int count,
                double dx,double dy,double dz,double speed) {
            if (particle == MushroomSporeProjectile.PURPLE) steamPuffs++;
            return 0;
        }
        @Override public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type,AABB bounds,java.util.function.Predicate<? super T> predicate) {
            return java.util.stream.Stream.concat(monsters == null ? java.util.stream.Stream.empty() : monsters.stream(),
                            mushrooms == null ? java.util.stream.Stream.empty() : mushrooms.stream()).filter(type::isInstance).map(type::cast)
                    .filter(e -> bounds.contains(e.position())).filter(predicate).toList();
        }
        @Override public BlockState getBlockState(BlockPos pos) {
            if (blocks.containsKey(pos)) return blocks.get(pos);
            BlockPos head = new BlockPos(0,64,0);
            if (pos.equals(head) || pos.equals(head.relative(facing.getOpposite())))
                return Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,facing)
                        .setValue(BedBlock.PART,pos.equals(head) ? BedPart.HEAD : BedPart.FOOT);
            return (pos.getY() < 64 ? Blocks.DIRT : Blocks.AIR).defaultBlockState();
        }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) { }
    }
    private static final class MonsterProbe extends Zombie {
        boolean living;
        boolean acceptsDamage;
        float receivedDamage;
        DamageSource damage;
        private MonsterProbe() { super(null,null); }
        @Override public boolean isAlive() { return living; }
        @Override public boolean canBeHitByProjectile() { return true; }
        @Override public DamageSource getLastDamageSource() { return damage; }
        @Override public double getEyeY() { return getY()+1.6; }
        @Override public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
            receivedDamage = amount; return acceptsDamage;
        }
    }
    private static final class ShooterProbe extends Zombie {
        MobEffectInstance effect;
        private ShooterProbe() { super(null,null); }
        @Override public MobEffectInstance getEffect(Holder<MobEffect> holder) { return effect; }
    }
    private static final class ProjectileProbe extends MushroomSporeProjectile {
        Entity owner;
        DamageSources sources;
        float inaccuracy,speed;
        private ProjectileProbe() { super(null,null); }
        @Override public Entity getOwner() { return owner; }
        @Override public DamageSources damageSources() { return sources; }
        @Override public Vec3 getMovementToShoot(double x,double y,double z,float speed,float inaccuracy) {
            this.inaccuracy=inaccuracy; this.speed=speed; return new Vec3(0,0,speed);
        }
        boolean canHit(Entity entity) { return canHitEntity(entity); }
        void impact(LivingEntity victim) { onHitEntity(new net.minecraft.world.phys.EntityHitResult(victim)); }
    }
    private static final class DamageSourcesProbe extends DamageSources {
        private DamageSourcesProbe() { super(RegistryAccess.EMPTY); }
        @Override public DamageSource thrown(Entity projectile, Entity shooter) {
            return new DamageSource(Holder.direct(new DamageType("fixture", 0)), projectile, shooter);
        }
    }
    private static final class BrainProbe extends NekoBrain {
        LivingEntity approach;
        Vec3 destination;
        BehaviorPriority priority;
        double speed;
        private BrainProbe() { super(null); }
        @Override public void submitMove(LivingEntity target, double speed, BehaviorPriority priority, Object source) {
            destination = null;
            approach = target; this.speed = speed; this.priority = priority;
        }
        @Override public void submitMove(Vec3 target, double speed, BehaviorPriority priority, Object source) {
            approach = null; destination = target; this.speed = speed; this.priority = priority;
        }
        @Override public void stopMoving(Object source) { approach = null; destination = null; }
        @Override public void reset() { super.reset(); approach = null; destination = null; }
    }
    private static final class NavigationProbe extends GroundPathNavigation {
        MushroomProbe owner;
        boolean reachable;
        java.util.Set<BlockPos> candidates;
        private NavigationProbe() { super(null, null); }
        @Override public void stop() { }
        @Override public void setCanFloat(boolean enabled) { }
        @Override public boolean moveTo(Entity entity, double speed) { return true; }
        @Override public Path createPath(BlockPos target,int accuracy) { return createPath(java.util.Set.of(target),accuracy); }
        @Override public Path createPath(java.util.Set<BlockPos> targets,int accuracy) {
            candidates = java.util.Set.copyOf(targets);
            BlockPos chosen = targets.stream().min(java.util.Comparator.comparingDouble(p -> p.distToCenterSqr(owner.position()))).orElseThrow();
            return new Path(List.of(new Node(chosen.getX(),chosen.getY(),chosen.getZ())), chosen, reachable);
        }
        @Override public boolean isDone() { return false; }
    }
    private static final class RoutineGoal extends Goal {
        boolean started, stopped;
        RoutineGoal() { setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return true; }
        @Override public void start() { started = true; }
        @Override public void stop() { stopped = true; }
    }
}
