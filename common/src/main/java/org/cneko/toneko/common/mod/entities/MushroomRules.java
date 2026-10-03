package org.cneko.toneko.common.mod.entities;

/** Shared, deterministic rules for mushroom physiology and social progression. */
public final class MushroomRules {
    public static final int WAKE_TICKS = 2 * 60 * 20;
    public static final int COFFEE_TICKS = 5 * 60 * 20;
    public static final int PET_FAMILIARITY = 40;
    public static final int FRIEND_FAMILIARITY = 60;
    public static final int HUG_FAMILIARITY = 80;
    public static final int MAX_FAMILIARITY = 100;
    public static final int LINGER_AFFECTION = 20;
    public static final int CLOSE_AFFECTION = 50;
    public static final int ATTACHED_AFFECTION = 80;
    public static final int INTERACTION_COOLDOWN = 100;
    public static final int SPORE_INTERVAL = 600;
    public static final int BIRTH_INTERVAL = 1200;
    public static final double MUSHROOM_CHANCE = 0.35;
    public static final int SPORE_POSITION_ATTEMPTS = 16;
    public static final int COMPANION_COOLDOWN = 1200;
    public static final int BIRTH_COOLDOWN = 24000;
    public static final int LOCAL_POPULATION_LIMIT = 4;
    public static final double BIRTH_CHANCE = 0.001;
    public static final int DEFENSE_INTERVAL = 40;
    public static final int ADMIRATION_TICKS = 160;
    public static final int NEGLECT_TICKS = 5 * 60 * 20;
    public static final int BLUSH_GAZE_TICKS = 10 * 20;
    public static final int BLUSH_LINGER_TICKS = 60;
    public static final double THREAT_RANGE = 10;
    public static final int MAX_LEVEL = 30;
    public static final int REGEN_INTERVAL = 30 * 20;
    public static final int REGEN_DAMAGE_DELAY = 10 * 20;
    public static final int COMPANION_XP_INTERVAL = 30 * 20;
    public static final int COMBAT_XP_INTERVAL = 5 * 20;
    public static final float BASE_SPORE_DAMAGE = 3f;
    public static final float BASE_MAX_HEALTH = 20f;

    public static int xpToNextLevel(int level) {
        level = Math.clamp(level, 1, MAX_LEVEL);
        return level == MAX_LEVEL ? 0 : 100 + (level - 1) * 40;
    }

    public static int xpAtLevel(int level) {
        int completed = Math.clamp(level, 1, MAX_LEVEL) - 1;
        return completed * 100 + 20 * completed * (completed - 1);
    }

    public static int levelFromXp(int experience) {
        int level = 1;
        while (level < MAX_LEVEL && experience >= xpAtLevel(level + 1)) level++;
        return level;
    }

    public static int addExperience(int current, int amount) {
        return (int)Math.clamp((long)current + Math.max(0, amount), 0, xpAtLevel(MAX_LEVEL));
    }

    public static float maxHealth(int level) {
        return BASE_MAX_HEALTH + (Math.clamp(level, 1, MAX_LEVEL) - 1) * 2f;
    }

    public static float sporeDamage(int level) {
        return BASE_SPORE_DAMAGE + (Math.clamp(level, 1, MAX_LEVEL) - 1) * 0.2f;
    }

    public static float regenerationAmount(int level) {
        return 0.5f + (Math.clamp(level, 1, MAX_LEVEL) - 1) * 0.1f;
    }

    public static boolean lowHealth(float health, float maximum) {
        return health > 0 && maximum > 0 && health <= maximum * 0.3f;
    }

    public static boolean feelsNeglected(int affection, int nearbyTicks) {
        return affection > 20 && nearbyTicks >= NEGLECT_TICKS;
    }

    public static int gazeTicks(int previous, boolean awakeBedVisit, boolean looking) {
        return awakeBedVisit && looking ? Math.min(BLUSH_GAZE_TICKS + 1, previous + 1) : 0;
    }

    private MushroomRules() {}

    public static boolean isDay(long time) {
        long dayTime = Math.floorMod(time, 24000);
        return dayTime < 13000 || dayTime >= 23000;
    }

    public static boolean shouldRest(long time, int wakeTicks, int coffeeTicks, boolean danger) {
        return isDay(time) && wakeTicks <= 0 && coffeeTicks <= 0 && !danger;
    }

    public static int addFamiliarity(int current, int amount) {
        return (int)Math.clamp((long)current + amount, 0, MAX_FAMILIARITY);
    }

    public record Relationship(int familiarity, int affection) {}

    public static Relationship relationship(int familiarity, int affection, int amount) {
        familiarity = addFamiliarity(familiarity, 0);
        affection = familiarity == MAX_FAMILIARITY ? Math.clamp(affection, 0, 100) : 0;
        if (amount > 0) {
            // Reaching full familiarity opens the stage; only later interactions earn affection.
            if (familiarity < MAX_FAMILIARITY) return new Relationship(addFamiliarity(familiarity, amount), 0);
            return new Relationship(familiarity, addFamiliarity(affection, Math.max(1, amount / 3)));
        }
        if (amount < 0) {
            long harm = -(long)amount;
            return new Relationship(addFamiliarity(familiarity, (int)-Math.min(MAX_FAMILIARITY,
                    Math.max(0, harm - affection))), (int)Math.max(0, affection - harm));
        }
        return new Relationship(familiarity, affection);
    }

    public static String affectionStage(int affection) {
        return affection >= ATTACHED_AFFECTION ? "attached" : affection >= CLOSE_AFFECTION ? "close" : "fond";
    }

    public static int lingerDuration(int affection) {
        return 20 * (10 + Math.clamp(affection, 0, 100) / 2);
    }

    public static int lingerInterval(int affection) {
        return 20 * (60 - Math.clamp(affection, 0, 100) / 2);
    }

    public static boolean canProduceChild(boolean adult, boolean moist, boolean shaded,
                                          int nearby, int cooldown, double roll) {
        return adult && moist && shaded && nearby < LOCAL_POPULATION_LIMIT
                && cooldown <= 0 && roll >= 0 && roll < BIRTH_CHANCE;
    }

    public static boolean canJoinPlayer(int familiarity, boolean carrying, boolean playerLying,
                                        boolean safe, boolean occupied) {
        return canJoinPlayer(familiarity, carrying, playerLying, false, safe, occupied);
    }

    public static int companionFamiliarity(boolean carrying, boolean inBed) {
        return carrying || inBed ? FRIEND_FAMILIARITY : HUG_FAMILIARITY;
    }

    public static boolean canJoinPlayer(int familiarity, boolean carrying, boolean playerLying,
                                        boolean inBed, boolean safe, boolean occupied) {
        return safe && !occupied && familiarity >= companionFamiliarity(carrying, inBed)
                && (carrying || playerLying);
    }
}
