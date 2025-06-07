package org.cneko.toneko.common.mod.entities;

import lombok.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface INeko {

    default LivingEntity getEntity(){
        throw new RuntimeException("You should implements in your entity");
    }
    default boolean isPlayer(){
        return this.getEntity() instanceof Player;
    }

    default boolean allowMateIfNotNeko(){
        return false;
    }

    default boolean isNeko(){
        return false;
    }
    default void setNeko(boolean isNeko){
    }

    default int getNekoAbility(){
        return (int)(this.getNekoLevel() + this.getEntity().getAttributeValue(ToNekoAttributes.NEKO_DEGREE));
    }

    default float getNekoLevel(){
        return 0;
    }
    default void setNekoLevel(float level){
    }

    default float getMaxNekoEnergy(){
        return (float) getEntity().getAttributeValue(ToNekoAttributes.MAX_NEKO_ENERGY);
    }
    default float getNekoEnergy(){
        return 0;
    }
    default void setNekoEnergy(float energy){
    }

    default Owner getOwner(UUID uuid){
        return getOwners().get(uuid);
    }
    default void addOwner(UUID uuid,Owner owner){
        getOwners().put(uuid, owner);
    }
    default void addOwnerIfNotExist(UUID uuid){
        if (!this.hasOwner(uuid)){
            this.addOwner(uuid, new Owner(List.of(), 0));
        }
    }
    default void removeOwner(UUID uuid){
        this.getOwners().remove(uuid);
    }
    default Map<UUID,Owner> getOwners(){
        return Map.of();
    }
    default boolean hasOwner(UUID uuid){
        return this.getOwners().containsKey(uuid);
    }



    default int getXpWithOwner(UUID uuid){
        return getOwner(uuid).getXp();
    }
    default void setXpWithOwner(UUID uuid, int xp){
         this.getOwner(uuid).setXp(xp);
    }

    default List<BlockedWord> getBlockedWords(){
        return List.of();
    }
    default void addBlockedWord(BlockedWord word){
        if (!this.getBlockedWords().contains(word)){
            this.getBlockedWords().add(word);
        }
    }
    default void removeBlockedWord(String word){
        this.getBlockedWords().removeIf(blockedWord -> blockedWord.block.equals(word));
    }

    @NotNull
    default String getNickName(){
        return "";
    }
    default void setNickName(String name){
    }

    default List<Quirk> getQuirks(){
        return List.of();
    }
    default boolean hasQuirk(Quirk quirk){
        return this.getQuirks().contains(quirk);
    }
    default void addQuirk(Quirk quirk){
        if (!this.hasQuirk(quirk)){
            this.getQuirks().add(quirk);
        }
    }
    default void removeQuirk(Quirk quirk){
        this.getQuirks().remove(quirk);
    }
    default void fixQuirks(){
        // 修复quirks
        this.getQuirks().removeIf(quirk -> QuirkRegister.hasQuirk(quirk.getId()));
    }


    default void saveNekoNBTData(@NotNull CompoundTag nbt){
        nbt.putBoolean("IsNeko", this.isNeko());
        nbt.putDouble("NekoEnergy", this.getNekoEnergy());
        nbt.putInt("NekoLevel", (int) this.getNekoLevel());
        CompoundTag owners = new CompoundTag();
        this.getOwners().forEach((uuid, owner) -> {
            CompoundTag ownerInfo = new CompoundTag();
            ownerInfo.putInt("Xp", owner.getXp());
            ListTag aliasTags = new ListTag();
            owner.getAliases().stream()
                    .map(StringTag::valueOf)
                    .forEach(aliasTags::add);
            ownerInfo.put("Aliases", aliasTags);

            owners.put(uuid.toString(), ownerInfo);
        });
        nbt.putString("NickName", this.getNickName());
        nbt.put("Owners", owners);
    }
    default void loadNekoNBTData(@NotNull CompoundTag nbt){
        if(nbt.contains("IsNeko")){
            this.setNeko(nbt.getBoolean("IsNeko"));
        }
        if (nbt.contains("NekoEnergy")) {
            this.setNekoEnergy(nbt.getFloat("NekoEnergy"));
        }
        if (nbt.contains("NekoLevel")) {
            this.setNekoLevel(nbt.getFloat("NekoLevel"));
        }
        if (nbt.contains("Owners")){
            CompoundTag owners = nbt.getCompound("Owners");
            for (String key : owners.getAllKeys()){
                CompoundTag ownerInfo = owners.getCompound(key);
                List<String> aliases;
                int xp;
                if (ownerInfo.contains("Aliases")) {
                    aliases = ownerInfo.getList("Aliases", ListTag.TAG_STRING).stream().map(Tag::toString).toList();
                }else {
                    aliases = new ArrayList<>();
                }
                if (ownerInfo.contains("Xp")){
                    xp = ownerInfo.getInt("Xp");
                }else {
                    xp = 0;
                }
                this.addOwner(UUID.fromString(key), new Owner(aliases, xp));
            }
        }
        if (nbt.contains("NickName")){
            this.setNickName(this.getNickName());
        }
    }

    default void serverNekoSlowTick(){
        // 如果是猫娘
        if (this.isNeko()){
            increaseEnergy();
        }
    }

    default void increaseEnergy(){
        // 如果满了，则忽略
        float max = this.getMaxNekoEnergy();
        float energy = this.getNekoEnergy();
        if (energy >= max){
            this.setNekoEnergy(max);
            return;
        }
        // 根据自身猫猫等级来增加
        float increase = (float) (this.getNekoAbility() * 0.01);
        // 根据周围猫猫数量来增加
        List<INeko> nekoes = EntityUtil.getNekoInRange(this.getEntity(), this.getEntity().level(), 3);
        for (INeko neko : nekoes){
            if (neko.isNeko()){
                increase += (float) (neko.getNekoAbility() * 0.1);
            }
        }
        this.setNekoEnergy(energy + increase);
        if (this.getNekoEnergy() >= max){
            this.setNekoEnergy(max);
        }
    }

    record BlockedWord(String block, String replace, BlockMethod method) {
            @Getter
            public enum BlockMethod{
                WORD("word"),
                ALL("all");
                private final String method;

                BlockMethod(String word) {
                    this.method = word;
                }
                public static BlockMethod fromString(String method){
                     for (BlockMethod value : values()) {
                        if (value.method.equals(method)){
                            return value;
                        }
                    }
                     return null;
                }
            }
    }
    @Data @AllArgsConstructor
    class Owner{
        private List<String> aliases;
        private int xp;
    }

}
