package org.cneko.toneko.common.mod.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoCropGatheringGoal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdventurerNeko extends NekoEntity{
    public static final List<String> nekoSkins = new ArrayList<>();
    static {
        nekoSkins.addAll(List.of("grmmy","aquarter"));
    }
    public AdventurerNeko(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }


    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(5, new NekoCropGatheringGoal(this));
    }

    @Override
    public boolean isFavoriteItem(ItemStack stack) {
        return super.isFavoriteItem(stack) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
    }

    @Override
    public @Nullable AdventurerNeko getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new AdventurerNeko(ToNekoEntities.ADVENTURER_NEKO, level);
    }

    public static AttributeSupplier.Builder createAdventurerNekoAttributes(){
        return createNekoAttributes();
    }

    // ====== 合成武器 ======

    @Override
    protected void equipBestMeleeWeapon() {
        tryCraftWeapon();
        super.equipBestMeleeWeapon();
    }

    /**
     * 如果背包中没有近战武器，尝试利用背包材料合成最好的武器
     */
    private void tryCraftWeapon() {
        // 已有近战武器则不需要合成
        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && getMeleeWeaponDamage(stack) > 0) {
                return;
            }
        }

        // 从最优到最差尝试合成（钻石 > 铁 > 石 > 木）
        if (tryCraftSword(Items.DIAMOND, Items.DIAMOND_SWORD)) return;
        if (tryCraftSword(Items.IRON_INGOT, Items.IRON_SWORD)) return;
        if (tryCraftSword(Items.COBBLESTONE, Items.STONE_SWORD)) return;
        tryCraftWoodenSword();
    }

    /**
     * 尝试合成矿物剑：2个材料 + 1个木棍
     */
    private boolean tryCraftSword(Item material, Item result) {
        int materialCount = countItem(material);
        int stickCount = countItem(Items.STICK);

        if (materialCount >= 2 && stickCount >= 1) {
            consumeItem(material, 2);
            consumeItem(Items.STICK, 1);
            this.getInventory().add(new ItemStack(result));
            return true;
        }
        return false;
    }

    /**
     * 尝试合成木剑（可用任意木板替代木棍）
     */
    private boolean tryCraftWoodenSword() {
        int stickCount = countItem(Items.STICK);
        int plankCount = countItemTag(ItemTags.PLANKS);

        // 2木板(刃) + 1木棍(柄)
        if (plankCount >= 2 && stickCount >= 1) {
            consumeItemTag(ItemTags.PLANKS, 2);
            consumeItem(Items.STICK, 1);
            this.getInventory().add(new ItemStack(Items.WOODEN_SWORD));
            return true;
        }
        // 没木棍但有足够木板：3木板 = 2刃 + 1柄代用
        if (plankCount >= 3 && stickCount < 1) {
            consumeItemTag(ItemTags.PLANKS, 3);
            this.getInventory().add(new ItemStack(Items.WOODEN_SWORD));
            return true;
        }
        return false;
    }

    private int countItem(Item item) {
        int count = 0;
        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countItemTag(TagKey<Item> tag) {
        int count = 0;
        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && stack.is(tag)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeItem(Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < this.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = this.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    private void consumeItemTag(TagKey<Item> tag, int amount) {
        int remaining = amount;
        for (int i = 0; i < this.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = this.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.is(tag)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }
}