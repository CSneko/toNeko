package org.cneko.toneko.common.mod.entities;

/** Dependency-free regression checks; run with :common:mushroomRulesTest. */
public class MushroomRulesTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(MushroomRules.sporeDamage(1) == 3 && MushroomRules.maxHealth(1) == 20,
                "A new mushroom has three-point spores and twenty health");
        check(MushroomRules.regenerationAmount(1) == 0.5f && MushroomRules.REGEN_INTERVAL == 600,
                "Initial natural recovery is half a health point every thirty seconds");
        check(MushroomRules.COMPANION_XP_INTERVAL == 600 && MushroomRules.COMBAT_XP_INTERVAL == 100,
                "Companionship and damaging hits earn experience slowly");
        check(MushroomRules.levelFromXp(-1) == 1 && MushroomRules.levelFromXp(99) == 1
                && MushroomRules.levelFromXp(100) == 2, "The first upgrade needs one hundred experience");
        for (int level = 1; level < MushroomRules.MAX_LEVEL; level++) {
            int threshold = MushroomRules.xpAtLevel(level + 1);
            check(threshold - MushroomRules.xpAtLevel(level) == MushroomRules.xpToNextLevel(level),
                    "Experience totals agree with the increasing cost of each upgrade");
            check(MushroomRules.levelFromXp(threshold - 1) == level
                    && MushroomRules.levelFromXp(threshold) == level + 1, "Each upgrade occurs at its exact threshold");
            check(MushroomRules.maxHealth(level + 1) > MushroomRules.maxHealth(level)
                    && MushroomRules.sporeDamage(level + 1) > MushroomRules.sporeDamage(level)
                    && MushroomRules.regenerationAmount(level + 1) > MushroomRules.regenerationAmount(level),
                    "Every upgrade improves health, damage and recovery");
        }
        check(MushroomRules.addExperience(Integer.MAX_VALUE, Integer.MAX_VALUE) == MushroomRules.xpAtLevel(30),
                "Experience cannot overflow or exceed the final upgrade");
        check(MushroomRules.addExperience(10, -1) == 10 && MushroomRules.addExperience(-10, 0) == 0,
                "Invalid experience gains cannot erase progress or create negative experience");
        check(MushroomRules.levelFromXp(Integer.MAX_VALUE) == 30 && MushroomRules.xpToNextLevel(30) == 0,
                "The final level is capped without requiring another upgrade");
        check(MushroomRules.maxHealth(0) == 20 && MushroomRules.maxHealth(31) == 78,
                "Out-of-range level inputs retain bounded stats");
        check(MushroomRules.lowHealth(6, 20) && !MushroomRules.lowHealth(7, 20), "Refuge threshold is 30 percent health");
        check(!MushroomRules.lowHealth(0, 20), "Dead mushrooms cannot seek refuge");
        check(!MushroomRules.feelsNeglected(20, 6000) && MushroomRules.feelsNeglected(21, 6000), "Neglect requires affection strictly above 20");
        check(!MushroomRules.feelsNeglected(100, 5999), "Neglect waits for five minutes nearby");
        int gaze = 0;
        for (int i = 0; i < 200; i++) gaze = MushroomRules.gazeTicks(gaze, true, true);
        check(gaze == 200, "Ten seconds alone is not yet more than ten seconds");
        check(MushroomRules.gazeTicks(gaze, true, true) == 201, "The next uninterrupted gaze tick blushes");
        check(MushroomRules.gazeTicks(gaze, true, false) == 0, "Looking away resets continuous gaze");
        check(MushroomRules.gazeTicks(gaze, false, true) == 0, "Sleeping or standing cannot accumulate blush gaze");
        for (int day = 0; day < 5; day++) {
            long base = day * 24000L;
            check(MushroomRules.isDay(base), "Dawn must be daytime");
            check(MushroomRules.isDay(base + 12999), "Day before dusk");
            check(!MushroomRules.isDay(base + 13000), "Dusk must wake her");
            check(!MushroomRules.isDay(base + 22999), "Last night tick");
            check(MushroomRules.isDay(base + 23000), "Dawn must restore rest");
        }
        check(MushroomRules.shouldRest(6000, 0, 0, false), "Uninterrupted day rest");
        check(!MushroomRules.shouldRest(6000, 1, 0, false), "Final awake tick");
        check(!MushroomRules.shouldRest(6000, 0, 1, false), "Coffee prevents sleep through final tick");
        check(!MushroomRules.shouldRest(6000, 0, 0, true), "Danger interrupts rest");
        check(!MushroomRules.shouldRest(18000, 0, 0, false), "Night is active without coffee");
        check(MushroomRules.WAKE_TICKS == 2400, "Two minutes of waking");
        check(MushroomRules.COFFEE_TICKS == 6000, "Five minutes of coffee");
        check(MushroomRules.addFamiliarity(99, 15) == 100, "Trust capped at 100");
        check(MushroomRules.addFamiliarity(5, -15) == 0, "Harm cannot cause negative trust");
        check(MushroomRules.relationship(99, 0, 15).equals(new MushroomRules.Relationship(100, 0)),
                "Reaching full familiarity unlocks affection without granting it early");
        check(MushroomRules.relationship(100, 0, 4).affection() == 1, "Later ordinary interactions grant one affection");
        check(MushroomRules.relationship(100, 0, 12).affection() == 4, "Food grants four affection");
        check(MushroomRules.relationship(100, 0, 15).affection() == 5, "Coffee grants five affection");
        check(MushroomRules.relationship(100, 99, 15).affection() == 100, "Affection is bounded");
        check(MushroomRules.relationship(100, 30, -15).equals(new MushroomRules.Relationship(100, 15)),
                "Harm first lowers affection");
        check(MushroomRules.relationship(100, 5, -15).equals(new MushroomRules.Relationship(90, 0)),
                "Remaining harm lowers familiarity after affection is exhausted");
        check(MushroomRules.relationship(50, 99, 0).affection() == 0, "Affection cannot precede full familiarity");
        check(MushroomRules.relationship(100, 100, Integer.MIN_VALUE).equals(new MushroomRules.Relationship(0, 0)),
                "Extreme damage cannot overflow the relationship bounds");
        for (int score = 20; score < 100; score++) {
            check(MushroomRules.lingerDuration(score + 1) >= MushroomRules.lingerDuration(score), "Higher affection keeps her nearby longer");
            check(MushroomRules.lingerInterval(score + 1) <= MushroomRules.lingerInterval(score), "Higher affection makes visits more frequent");
        }
        check(MushroomRules.affectionStage(49).equals("fond") && MushroomRules.affectionStage(50).equals("close")
                && MushroomRules.affectionStage(80).equals("attached"), "Affection stages respect their thresholds");
        check(MushroomRules.PET_FAMILIARITY < MushroomRules.FRIEND_FAMILIARITY
                && MushroomRules.FRIEND_FAMILIARITY < MushroomRules.HUG_FAMILIARITY, "Intimacy unlocks in stages");
        check(MushroomRules.canProduceChild(true, true, true, 3, 0, 0), "Eligible spore birth");
        check(!MushroomRules.canProduceChild(false, true, true, 1, 0, 0), "Babies cannot reproduce");
        check(!MushroomRules.canProduceChild(true, false, true, 1, 0, 0), "Dry places cannot produce children");
        check(!MushroomRules.canProduceChild(true, true, false, 1, 0, 0), "Bright places cannot produce children");
        check(!MushroomRules.canProduceChild(true, true, true, 4, 0, 0), "Local population is bounded");
        check(!MushroomRules.canProduceChild(true, true, true, 1, 1, 0), "Birth cooldown prevents retries");
        check(!MushroomRules.canProduceChild(true, true, true, 1, 0, 0.001), "Probability cutoff is exclusive");
        check(MushroomRules.canProduceChild(true, true, true, 1, 0, 0.000999), "Rare birth below cutoff");
        int births = 0;
        for (int i = 0; i < 10000; i++) {
            if (MushroomRules.canProduceChild(true, true, true, 1, 0, i / 10000.0)) births++;
        }
        check(births == 10, "Exactly 0.1 percent of eligible release samples produce a child");
        check(MushroomRules.SPORE_INTERVAL == 600 && MushroomRules.BIRTH_INTERVAL == 1200,
                "Mushrooms try twice per minute; rare births only once");
        check(MushroomRules.SPORE_POSITION_ATTEMPTS > 1, "Spores search multiple ground candidates");
        for (int trust = 0; trust <= 100; trust++) {
            check(MushroomRules.canJoinPlayer(trust, true, false, true, false) == (trust >= 60), "Carry trust boundary");
            check(MushroomRules.canJoinPlayer(trust, false, true, true, false) == (trust >= 80), "Companion trust boundary");
            check(!MushroomRules.canJoinPlayer(trust, false, false, true, false), "Sitting requires a lying player");
            check(!MushroomRules.canJoinPlayer(trust, true, true, false, false), "Unsafe players cannot carry");
            check(!MushroomRules.canJoinPlayer(trust, false, true, true, true), "Never stack companions on an occupied player");
            check(MushroomRules.canJoinPlayer(trust, false, true, true, true, false) == (trust >= 60), "Bed companions unlock for familiar friends");
        }
        System.out.println("Mushroom rules: " + checks + " checks passed.");
    }
}
