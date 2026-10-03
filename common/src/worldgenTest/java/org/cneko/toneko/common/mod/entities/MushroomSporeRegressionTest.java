package org.cneko.toneko.common.mod.entities;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import sun.misc.Unsafe;

import java.util.*;

/** Runs the production spore method with native blocks and a small, deterministic world fixture. */
public final class MushroomSporeRegressionTest {
    private static Unsafe fixtures;
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void field(Object object, Class<?> owner, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
    private static int cooldown(SporeProbe mother) throws Exception {
        var field = MushroomGirlEntity.class.getDeclaredField("birthCooldown");
        field.setAccessible(true);
        return field.getInt(mother);
    }
    private static SporeProbe fixture(double birthRoll, double mushroomRoll) throws Exception {
        SporeProbe mother = (SporeProbe) fixtures.allocateInstance(SporeProbe.class);
        WorldProbe world = (WorldProbe) fixtures.allocateInstance(WorldProbe.class);
        world.blocks = new HashMap<>(); world.children = new ArrayList<>();
        world.rules = new GameRules(FeatureFlagSet.of());
        world.brightness = 8; world.population = 1; world.clearCollision = true;
        world.blocks.put(new BlockPos(0, 63, 0), Blocks.DIRT.defaultBlockState());
        mother.world = world;
        world.mother = mother;
        mother.tickCount = 1200;
        mother.rolls = new ControlledRandom(birthRoll, mushroomRoll);
        field(mother, Entity.class, "random", mother.rolls);
        field(mother, Entity.class, "position", new Vec3(0.5, 64, 0.5));
        mother.setBoundingBox(new AABB(0, 64, 0, 1, 66, 1));
        field(mother, MushroomGirlEntity.class, "moist", true);
        field(mother, MushroomGirlEntity.class, "shaded", true);
        return mother;
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var unsafe = Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        fixtures = (Unsafe) unsafe.get(null);

        SporeProbe mother = fixture(0.5, 0);
        mother.release();
        check(mother.world.planted == 1, "Native spore method places a real mushroom block");
        check(mother.rolls.ints >= 4, "A bad first ground candidate does not prevent another position being tried");
        check(mother.rolls.doubles == 2, "Multiple placement candidates do not multiply probability rolls");
        check(mother.world.growthParticles == 1, "Successful growth has visible particles at the plant");
        check(mother.world.children.isEmpty(), "Ordinary mushroom roll does not create a child");

        mother = fixture(0.5, 0);
        for (int i = 1; i <= 8; i++) mother.world.blocks.put(new BlockPos(i % 4 - 2, 64, i / 4 + 1), Blocks.BROWN_MUSHROOM.defaultBlockState());
        mother.release();
        check(mother.world.planted == 0, "Existing mushrooms cap further planting");

        mother = fixture(0, 0);
        mother.world.rules.set(GameRules.MOB_GRIEFING, false, null);
        mother.release();
        check(mother.world.planted == 0 && mother.world.children.isEmpty(), "Mob griefing disables both ecological changes");

        mother = fixture(0, 0.5);
        mother.world.brightness = 13;
        mother.release();
        check(mother.world.planted == 0, "Ordinary mushrooms still obey native light requirements");
        check(mother.world.children.size() == 1 && mother.world.children.getFirst().baby,
                "A shaded safe floor can produce a juvenile independently of ordinary mushroom survival");
        check(cooldown(mother) == MushroomRules.BIRTH_COOLDOWN, "Successful birth applies maternal cooldown");
        check(cooldown(mother.world.children.getFirst()) == MushroomRules.BIRTH_COOLDOWN, "Juvenile has its own cooldown");
        check(mother.world.growthParticles == 1, "Successful birth produces visible feedback");
        mother.release();
        check(mother.world.children.size() == 1, "Birth cooldown prevents immediate duplicate offspring");

        mother = fixture(0, 0.5); mother.world.population = 4; mother.release();
        check(mother.world.children.isEmpty(), "Native method respects the local population limit");
        mother = fixture(0, 0.5); mother.world.clearCollision = false; mother.release();
        check(mother.world.children.isEmpty() && cooldown(mother) == 0, "Blocked birth placement neither spawns nor consumes cooldown");
        mother = fixture(0, 0.5); mother.baby = true; mother.release();
        check(mother.world.children.isEmpty(), "Juveniles do not produce offspring");
        mother = fixture(0, 0); mother.resting = true; mother.release();
        check(mother.world.planted == 0 && mother.world.children.isEmpty(), "Daytime rest suppresses ecological changes");
        mother = fixture(0, 0); mother.mounted = true; mother.release();
        check(mother.world.planted == 0 && mother.world.children.isEmpty(), "Mounted companions do not plant on the player");
        System.out.println("Mushroom spore regression: " + checks + " checks passed.");
    }

    private static final class ControlledRandom extends LegacyRandomSource {
        int ints, doubles;
        final double birth, mushroom;
        ControlledRandom(double birth, double mushroom) { super(1); this.birth = birth; this.mushroom = mushroom; }
        @Override public int nextInt(int bound) { return ints++ < 2 ? 0 : Math.min(3, bound - 1); }
        @Override public double nextDouble() { return doubles++ == 0 ? birth : mushroom; }
        @Override public boolean nextBoolean() { return false; }
    }

    private static final class SporeProbe extends MushroomGirlEntity {
        WorldProbe world;
        ControlledRandom rolls;
        boolean baby, resting, mounted;
        private SporeProbe() { super(null, null); }
        void release() { spawnAmbientParticles(); }
        @Override public Level level() { return world; }
        @Override public BlockPos blockPosition() { return new BlockPos(0, 64, 0); }
        @Override public boolean isResting() { return resting; }
        @Override public boolean isPassenger() { return mounted; }
        @Override public boolean isNekoBaby() { return baby; }
        @Override public void setNekoBaby(boolean baby) { this.baby = baby; }
        @Override public void moveTo(double x, double y, double z, float yaw, float pitch) { }
        @Override public void playExpressAnim(String animation) { }
        @Override protected MushroomGirlEntity createSporeChild(ServerLevel level) {
            try { return (SporeProbe) fixtures.allocateInstance(SporeProbe.class); }
            catch (InstantiationException e) { throw new AssertionError(e); }
        }
    }

    private static final class WorldProbe extends ServerLevel {
        Map<BlockPos, BlockState> blocks;
        List<SporeProbe> children;
        GameRules rules;
        SporeProbe mother;
        int brightness, population, planted, growthParticles;
        boolean clearCollision;
        private WorldProbe() { super(null, null, null, null, null, null, false, 0, List.of(), false); }
        @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
        @Override public boolean isLoaded(BlockPos pos) { return true; }
        @Override public boolean isRainingAt(BlockPos pos) { return true; }
        @Override public boolean canSeeSky(BlockPos pos) { return false; }
        @Override public int getMaxLocalRawBrightness(BlockPos pos) { return brightness; }
        @Override public int getRawBrightness(BlockPos pos, int skyDarken) { return brightness; }
        @Override public GameRules getGameRules() { return rules; }
        @Override public List<ServerPlayer> players() { return List.of(); }
        @Override public boolean noCollision(Entity child) { return clearCollision; }
        @Override public boolean addFreshEntity(Entity child) { children.add((SporeProbe) child); return true; }
        @Override public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB box) { return Collections.nCopies(population, type.cast(mother)); }
        @Override public boolean setBlockAndUpdate(BlockPos pos, BlockState block) { blocks.put(pos.immutable(), block); planted++; return true; }
        @Override public <T extends ParticleOptions> int sendParticles(T particle, double x, double y, double z, int count,
                                                                       double dx, double dy, double dz, double speed) {
            if (particle == ParticleTypes.HAPPY_VILLAGER) growthParticles++;
            return 0;
        }
    }
}
