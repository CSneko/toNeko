package org.cneko.toneko.common.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cneko.toneko.common.Bootstrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * UUID-keyed neko player data store.
 * Persists to JSON files under ctlib/toneko/neko_data/.
 */
public class NekoQuery {
    private static final Map<UUID, Neko> NEKOS = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = Path.of("ctlib/toneko/neko_data");

    public static void init() {
        try { Files.createDirectories(DATA_PATH); } catch (IOException ignored) {}
        loadAll();
    }

    // === Static accessors ===

    public static @NotNull Neko getNeko(UUID uuid) {
        return NEKOS.computeIfAbsent(uuid, Neko::new);
    }

    public static boolean isNeko(UUID uuid) {
        Neko n = NEKOS.get(uuid);
        return n != null && n.isNeko();
    }

    public static void setNeko(UUID uuid, boolean isNeko) {
        Neko n = NEKOS.computeIfAbsent(uuid, Neko::new);
        n.setNeko(isNeko);
        n.save();
    }

    public static double getLevel(UUID uuid) {
        Neko n = NEKOS.get(uuid);
        return n != null ? n.getLevel() : 0;
    }

    // === Neko class ===

    public static class Neko {
        private UUID uuid;
        private boolean isNeko;
        private String nickName = "";
        private double level;
        private float energy;
        private float maxEnergy = 1000f;
        private int age = 0; // 0=adult, negative=baby (matches INeko.getMaxAge=24000)
        private final Map<UUID, Double> xpMap = new LinkedHashMap<>();
        private final Set<UUID> owners = new LinkedHashSet<>();
        private final Map<UUID, List<String>> aliases = new LinkedHashMap<>();
        private final List<String> blockedWords = new ArrayList<>();
        private final List<String> quirkList = new ArrayList<>();

        public Neko(UUID uuid) { this.uuid = uuid; }

        public UUID getUuid() { return uuid; }
        public boolean isNeko() { return isNeko; }
        public void setNeko(boolean v) { isNeko = v; }
        public String getNickName() { return nickName; }
        public void setNickName(String s) { nickName = s; }
        public double getLevel() { return level; }
        public void addLevel(double v) { level += v; }
        public void setLevel(double v) { level = v; }
        public float getNekoEnergy() { return energy; }
        public void setNekoEnergy(float v) { energy = v < 0 ? 0 : Math.min(v, maxEnergy); }
        public float getMaxNekoEnergy() { return maxEnergy; }
        public void setMaxNekoEnergy(float v) { maxEnergy = v < 100 ? 100 : v; }
        public int getNekoAge() { return age; }
        public void setNekoAge(int v) { age = v; }
        public boolean isNekoBaby() { return age < 0; }
        public void setNekoBaby(boolean baby) {
            if (baby) age = -24000; else if (age < 0) age = 0;
        }
        public int getMaxAge() { return 24000; }
        /** Advance age by 1 tick. Called every 20 ticks by EnergyManager. Returns true while growing up. */
        public void tickAge() {
            if (age < 0) age = Math.min(age + 20, 0); // advance 1 second worth
        }

        public double getXp(UUID owner) { return xpMap.getOrDefault(owner, 0.0); }
        public void setXp(UUID owner, double xp) { xpMap.put(owner, xp); }
        public Map<UUID, Double> getXpMap() { return xpMap; }

        public boolean hasOwner(UUID owner) { return owners.contains(owner); }
        public void addOwner(UUID owner) { owners.add(owner); }
        public void removeOwner(UUID owner) { owners.remove(owner); }
        public Set<UUID> getOwners() { return owners; }

        public void addAlias(UUID owner, String alias) {
            aliases.computeIfAbsent(owner, k -> new ArrayList<>()).add(alias);
        }
        public void removeAlias(UUID owner, String alias) {
            List<String> list = aliases.get(owner);
            if (list != null) list.remove(alias);
        }
        public Map<UUID, List<String>> getAliases() { return aliases; }

        public List<String> getBlockedWords() { return blockedWords; }
        public void addBlock(String word, String replacement, String method) {
            blockedWords.add(word + "|" + replacement + "|" + method);
        }

        public List<String> getQuirks() { return quirkList; }
        public void fixQuirks() {
            // Remove any quirks that don't exist in the registry
            quirkList.removeIf(q -> org.cneko.toneko.common.mod.quirks.QuirkRegister.getById(q) == null);
        }

        public void save() {
            try {
                Path file = DATA_PATH.resolve(uuid.toString() + ".json");
                Files.writeString(file, GSON.toJson(this));
            } catch (IOException e) {
                Bootstrap.LOGGER.warn("Failed to save neko data for {}", uuid);
            }
        }
    }

    // === NekoData static helpers ===

    public static class NekoData {
        public static void saveAll() {
            NEKOS.values().forEach(Neko::save);
        }

        public static void saveAllAsync(Runnable callback) {
            CompletableFuture.runAsync(() -> {
                saveAll();
                callback.run();
            });
        }

        public static void removeAll() {
            NEKOS.clear();
            try {
                Files.list(DATA_PATH).forEach(f -> {
                    try { Files.delete(f); } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }

        public static int getNekoCount() {
            return (int) NEKOS.values().stream().filter(Neko::isNeko).count();
        }

        public static void asyncGetAllNekoCount(Consumer<Integer> callback) {
            CompletableFuture.runAsync(() -> callback.accept(getNekoCount()));
        }

        public static void saveAndRemoveNeko(UUID uuid) {
            Neko n = NEKOS.remove(uuid);
            if (n != null) n.save();
        }
    }

    // === Persistence ===

    private static void loadAll() {
        try {
            Files.list(DATA_PATH).filter(f -> f.toString().endsWith(".json")).forEach(f -> {
                try {
                    String json = Files.readString(f);
                    Neko n = GSON.fromJson(json, Neko.class);
                    if (n != null && n.uuid != null) NEKOS.put(n.uuid, n);
                } catch (Exception ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
