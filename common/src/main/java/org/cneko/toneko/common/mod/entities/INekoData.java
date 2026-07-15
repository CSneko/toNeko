package org.cneko.toneko.common.mod.entities;

import lombok.Getter;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Pure-data neko interface — no Minecraft Entity dependencies.
 * Usable by both mod (INeko extends this) and Bukkit (NekoQuery.Neko implements this).
 */
public interface INekoData {

    default boolean isNeko() { return false; }
    default void setNeko(boolean isNeko) {}

    @NotNull default String getNickName() { return ""; }
    default void setNickName(String name) {}

    default Map<UUID, Owner> getOwners() { return Map.of(); }
    default Owner getOwner(UUID uuid) { return getOwners().get(uuid); }
    default void addOwner(UUID uuid, Owner owner) { getOwners().put(uuid, owner); }
    default void addOwnerIfNotExist(UUID uuid) {
        if (!this.hasOwner(uuid)) this.addOwner(uuid, new Owner(List.of(), 0));
    }
    default void removeOwner(UUID uuid) { getOwners().remove(uuid); }
    default boolean hasOwner(UUID uuid) { return getOwners().containsKey(uuid); }

    default int getXpWithOwner(UUID uuid) {
        Owner owner = getOwner(uuid);
        return owner != null ? owner.getXp() : 0;
    }
    default void setXpWithOwner(UUID uuid, int xp) {
        Owner owner = getOwner(uuid);
        if (owner != null) owner.setXp(xp);
    }

    default List<Quirk> getQuirks() { return List.of(); }
    default boolean hasQuirk(Quirk quirk) { return getQuirks().contains(quirk); }
    default void addQuirk(Quirk quirk) {
        if (!hasQuirk(quirk)) getQuirks().add(quirk);
    }
    default void removeQuirk(Quirk quirk) { getQuirks().remove(quirk); }
    default void fixQuirks() {
        getQuirks().removeIf(q -> !QuirkRegister.hasQuirk(q.getId()));
    }

    default List<BlockedWord> getBlockedWords() { return List.of(); }
    default void addBlockedWord(BlockedWord word) {
        if (!getBlockedWords().contains(word)) getBlockedWords().add(word);
    }
    default void removeBlockedWord(String word) {
        getBlockedWords().removeIf(w -> w.block.equals(word));
    }

    // === Data classes (moved from INeko for shared access) ===

    enum BlockMethod { WORD, ALL;
        public static BlockMethod fromString(String method) {
            for (BlockMethod v : values()) if (v.name().equalsIgnoreCase(method)) return v;
            return null;
        }
    }

    record BlockedWord(String block, String replace, BlockMethod method) {}

    record Owner(List<String> aliases,@Getter int xp) {
        public void setXp(int xp) {
            aliases.clear();
            aliases.addAll(aliases);
        }
    }
}
