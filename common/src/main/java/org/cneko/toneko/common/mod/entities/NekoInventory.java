package org.cneko.toneko.common.mod.entities;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NekoInventory implements Container, Nameable {
    public static final int POP_TIME_DURATION = 5;
    public static final int INVENTORY_SIZE = 36;
    private static final int SELECTION_SIZE = 9;
    public static final int SLOT_OFFHAND = 40;
    public static final int NOT_FOUND_INDEX = -1;
    public static final int[] ALL_ARMOR_SLOTS = new int[]{0, 1, 2, 3};
    public static final int[] HELMET_SLOT_ONLY = new int[]{3};
    public final NonNullList<ItemStack> items;
    public final NonNullList<ItemStack> armor;
    public final NonNullList<ItemStack> offhand;
    private final List<NonNullList<ItemStack>> compartments;
    public int selected;
    public final NekoEntity neko;
    private int timesChanged;

    public NekoInventory(NekoEntity neko) {
        this.items = NonNullList.withSize(36, ItemStack.EMPTY);
        this.armor = NonNullList.withSize(4, ItemStack.EMPTY);
        this.offhand = NonNullList.withSize(1, ItemStack.EMPTY);
        this.compartments = ImmutableList.of(this.items, this.armor, this.offhand);
        this.neko = neko;
    }

    public ItemStack getSelected() {
        return isHotbarSlot(this.selected) ? (ItemStack)this.items.get(this.selected) : ItemStack.EMPTY;
    }

    public static int getSelectionSize() {
        return 9;
    }

    private boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin) {
        return !destination.isEmpty() && ItemStack.isSameItemSameComponents(destination, origin) && destination.isStackable() && destination.getCount() < this.getMaxStackSize(destination);
    }

    public int getFreeSlot() {
        for(int i = 0; i < this.items.size(); ++i) {
            if (((ItemStack)this.items.get(i)).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void setPickedItem(ItemStack stack) {
        int i = this.findSlotMatchingItem(stack);
        if (isHotbarSlot(i)) {
            this.selected = i;
        } else {
            if (i == -1) {
                this.selected = this.getSuitableHotbarSlot();
                if (!((ItemStack)this.items.get(this.selected)).isEmpty()) {
                    int j = this.getFreeSlot();
                    if (j != -1) {
                        this.items.set(j, (ItemStack)this.items.get(this.selected));
                    }
                }

                this.items.set(this.selected, stack);
            } else {
                this.pickSlot(i);
            }

        }
    }

    public void pickSlot(int index) {
        this.selected = this.getSuitableHotbarSlot();
        ItemStack itemStack = (ItemStack)this.items.get(this.selected);
        this.items.set(this.selected, (ItemStack)this.items.get(index));
        this.items.set(index, itemStack);
    }

    public static boolean isHotbarSlot(int index) {
        return index >= 0 && index < 9;
    }

    public int findSlotMatchingItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            if (!((ItemStack)this.items.get(i)).isEmpty() && ItemStack.isSameItemSameComponents(stack, (ItemStack)this.items.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public int findSlotMatchingUnusedItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = (ItemStack)this.items.get(i);
            if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemStack) && !itemStack.isDamaged() && !itemStack.isEnchanted() && !itemStack.has(DataComponents.CUSTOM_NAME)) {
                return i;
            }
        }

        return -1;
    }

    public int getSuitableHotbarSlot() {
        int i;
        int j;
        for(i = 0; i < 9; ++i) {
            j = (this.selected + i) % 9;
            if (((ItemStack)this.items.get(j)).isEmpty()) {
                return j;
            }
        }

        for(i = 0; i < 9; ++i) {
            j = (this.selected + i) % 9;
            if (!((ItemStack)this.items.get(j)).isEnchanted()) {
                return j;
            }
        }

        return this.selected;
    }

    public void swapPaint(double direction) {
        int i = (int)Math.signum(direction);

        for(this.selected -= i; this.selected < 0; this.selected += 9) {
        }

        while(this.selected >= 9) {
            this.selected -= 9;
        }

    }

    private int addResource(ItemStack stack) {
        int i = this.getSlotWithRemainingSpace(stack);
        if (i == -1) {
            i = this.getFreeSlot();
        }

        return i == -1 ? stack.getCount() : this.addResource(i, stack);
    }

    private int addResource(int slot, ItemStack stack) {
        int i = stack.getCount();
        ItemStack itemStack = this.getItem(slot);
        if (itemStack.isEmpty()) {
            itemStack = stack.copyWithCount(0);
            this.setItem(slot, itemStack);
        }

        int j = this.getMaxStackSize(itemStack) - itemStack.getCount();
        int k = Math.min(i, j);
        if (k == 0) {
            return i;
        } else {
            i -= k;
            itemStack.grow(k);
            itemStack.setPopTime(5);
            return i;
        }
    }

    public int getSlotWithRemainingSpace(ItemStack stack) {
        if (this.hasRemainingSpaceForItem(this.getItem(this.selected), stack)) {
            return this.selected;
        } else if (this.hasRemainingSpaceForItem(this.getItem(40), stack)) {
            return 40;
        } else {
            for(int i = 0; i < this.items.size(); ++i) {
                if (this.hasRemainingSpaceForItem((ItemStack)this.items.get(i), stack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public void tick() {
        Iterator var1 = this.compartments.iterator();

        while(var1.hasNext()) {
            NonNullList<ItemStack> nonNullList = (NonNullList)var1.next();

            for(int i = 0; i < nonNullList.size(); ++i) {
                if (!((ItemStack)nonNullList.get(i)).isEmpty()) {
                    ((ItemStack)nonNullList.get(i)).inventoryTick(this.neko.level(), this.neko, i, this.selected == i);
                }
            }
        }

    }

    public boolean add(ItemStack stack) {
        return this.add(-1, stack);
    }

    public boolean add(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            Throwable throwable;
            CrashReport crashReport;
            CrashReportCategory crashReportCategory;
            int i;
            label81: {
                try {
                    if (!stack.isDamaged()) {
                        do {
                            i = stack.getCount();
                            if (slot == -1) {
                                stack.setCount(this.addResource(stack));
                            } else {
                                stack.setCount(this.addResource(slot, stack));
                            }
                        } while(!stack.isEmpty() && stack.getCount() < i);

                        if (stack.getCount() == i && this.neko.hasInfiniteMaterials()) {
                            stack.setCount(0);
                            return true;
                        }
                        break label81;
                    }
                } catch (Throwable var10) {
                    throwable = var10;
                    crashReport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                    crashReportCategory = crashReport.addCategory("Item being added");
                    crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                    crashReportCategory.setDetail("Item data", stack.getDamageValue());
                    crashReportCategory.setDetail("Item name", () -> {
                        return stack.getHoverName().getString();
                    });
                    throw new ReportedException(crashReport);
                }

                try {
                    if (slot == -1) {
                        slot = this.getFreeSlot();
                    }

                    if (slot >= 0) {
                        this.items.set(slot, stack.copyAndClear());
                        ((ItemStack)this.items.get(slot)).setPopTime(5);
                        return true;
                    }
                } catch (Throwable var9) {
                    throwable = var9;
                    crashReport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                    crashReportCategory = crashReport.addCategory("Item being added");
                    crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                    crashReportCategory.setDetail("Item data", stack.getDamageValue());
                    crashReportCategory.setDetail("Item name", () -> {
                        return stack.getHoverName().getString();
                    });
                    throw new ReportedException(crashReport);
                }

                try {
                    if (this.neko.hasInfiniteMaterials()) {
                        stack.setCount(0);
                        return true;
                    }
                } catch (Throwable var8) {
                    throwable = var8;
                    crashReport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                    crashReportCategory = crashReport.addCategory("Item being added");
                    crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                    crashReportCategory.setDetail("Item data", stack.getDamageValue());
                    crashReportCategory.setDetail("Item name", () -> {
                        return stack.getHoverName().getString();
                    });
                    throw new ReportedException(crashReport);
                }

                try {
                    return false;
                } catch (Throwable var7) {
                    throwable = var7;
                    crashReport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                    crashReportCategory = crashReport.addCategory("Item being added");
                    crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                    crashReportCategory.setDetail("Item data", stack.getDamageValue());
                    crashReportCategory.setDetail("Item name", () -> {
                        return stack.getHoverName().getString();
                    });
                    throw new ReportedException(crashReport);
                }
            }

            try {
                return stack.getCount() < i;
            } catch (Throwable var6) {
                throwable = var6;
                crashReport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                crashReportCategory = crashReport.addCategory("Item being added");
                crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                crashReportCategory.setDetail("Item data", stack.getDamageValue());
                crashReportCategory.setDetail("Item name", () -> {
                    return stack.getHoverName().getString();
                });
                throw new ReportedException(crashReport);
            }
        }
    }


    public ItemStack removeItem(int slot, int amount) {
        List<ItemStack> list = null;

        NonNullList nonNullList;
        for(Iterator var4 = this.compartments.iterator(); var4.hasNext(); slot -= nonNullList.size()) {
            nonNullList = (NonNullList)var4.next();
            if (slot < nonNullList.size()) {
                list = nonNullList;
                break;
            }
        }

        return list != null && !((ItemStack)list.get(slot)).isEmpty() ? ContainerHelper.removeItem(list, slot, amount) : ItemStack.EMPTY;
    }

    public void removeItem(ItemStack stack) {
        Iterator var2 = this.compartments.iterator();

        while(true) {
            while(var2.hasNext()) {
                NonNullList<ItemStack> nonNullList = (NonNullList)var2.next();

                for(int i = 0; i < nonNullList.size(); ++i) {
                    if (nonNullList.get(i) == stack) {
                        nonNullList.set(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }

            return;
        }
    }

    public ItemStack removeItemNoUpdate(int slot) {
        NonNullList<ItemStack> nonNullList = null;

        NonNullList nonNullList2;
        for(Iterator var3 = this.compartments.iterator(); var3.hasNext(); slot -= nonNullList2.size()) {
            nonNullList2 = (NonNullList)var3.next();
            if (slot < nonNullList2.size()) {
                nonNullList = nonNullList2;
                break;
            }
        }

        if (nonNullList != null && !((ItemStack)nonNullList.get(slot)).isEmpty()) {
            ItemStack itemStack = (ItemStack)nonNullList.get(slot);
            nonNullList.set(slot, ItemStack.EMPTY);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void setItem(int slot, ItemStack stack) {
        NonNullList<ItemStack> nonNullList = null;

        NonNullList nonNullList2;
        for(Iterator var4 = this.compartments.iterator(); var4.hasNext(); slot -= nonNullList2.size()) {
            nonNullList2 = (NonNullList)var4.next();
            if (slot < nonNullList2.size()) {
                nonNullList = nonNullList2;
                break;
            }
        }

        if (nonNullList != null) {
            nonNullList.set(slot, stack);
        }

    }

    public float getDestroySpeed(BlockState state) {
        return ((ItemStack)this.items.get(this.selected)).getDestroySpeed(state);
    }

    public ListTag save(ListTag listTag) {
        int i;
        CompoundTag compoundTag;
        for(i = 0; i < this.items.size(); ++i) {
            if (!((ItemStack)this.items.get(i)).isEmpty()) {
                compoundTag = new CompoundTag();
                compoundTag.putByte("Slot", (byte)i);
                listTag.add(((ItemStack)this.items.get(i)).save(this.neko.registryAccess(), compoundTag));
            }
        }

        for(i = 0; i < this.armor.size(); ++i) {
            if (!((ItemStack)this.armor.get(i)).isEmpty()) {
                compoundTag = new CompoundTag();
                compoundTag.putByte("Slot", (byte)(i + 100));
                listTag.add(((ItemStack)this.armor.get(i)).save(this.neko.registryAccess(), compoundTag));
            }
        }

        for(i = 0; i < this.offhand.size(); ++i) {
            if (!((ItemStack)this.offhand.get(i)).isEmpty()) {
                compoundTag = new CompoundTag();
                compoundTag.putByte("Slot", (byte)(i + 150));
                listTag.add(((ItemStack)this.offhand.get(i)).save(this.neko.registryAccess(), compoundTag));
            }
        }

        return listTag;
    }

    public void load(ListTag listTag) {
        this.items.clear();
        this.armor.clear();
        this.offhand.clear();

        for(int i = 0; i < listTag.size(); ++i) {
            CompoundTag compoundTag = listTag.getCompound(i);
            int j = compoundTag.getByte("Slot") & 255;
            ItemStack itemStack = (ItemStack)ItemStack.parse(this.neko.registryAccess(), compoundTag).orElse(ItemStack.EMPTY);
            if (j >= 0 && j < this.items.size()) {
                this.items.set(j, itemStack);
            } else if (j >= 100 && j < this.armor.size() + 100) {
                this.armor.set(j - 100, itemStack);
            } else if (j >= 150 && j < this.offhand.size() + 150) {
                this.offhand.set(j - 150, itemStack);
            }
        }

    }

    public int getContainerSize() {
        return this.items.size() + this.armor.size() + this.offhand.size();
    }

    public boolean isEmpty() {
        Iterator var1 = this.items.iterator();

        ItemStack itemStack;
        do {
            if (!var1.hasNext()) {
                var1 = this.armor.iterator();

                do {
                    if (!var1.hasNext()) {
                        var1 = this.offhand.iterator();

                        do {
                            if (!var1.hasNext()) {
                                return true;
                            }

                            itemStack = (ItemStack)var1.next();
                        } while(itemStack.isEmpty());

                        return false;
                    }

                    itemStack = (ItemStack)var1.next();
                } while(itemStack.isEmpty());

                return false;
            }

            itemStack = (ItemStack)var1.next();
        } while(itemStack.isEmpty());

        return false;
    }

    public ItemStack getItem(int slot) {
        List<ItemStack> list = null;

        NonNullList nonNullList;
        for(Iterator var3 = this.compartments.iterator(); var3.hasNext(); slot -= nonNullList.size()) {
            nonNullList = (NonNullList)var3.next();
            if (slot < nonNullList.size()) {
                list = nonNullList;
                break;
            }
        }

        return list == null ? ItemStack.EMPTY : (ItemStack)list.get(slot);
    }

    public Component getName() {
        return Component.translatable("container.inventory");
    }

    public ItemStack getArmor(int slot) {
        return (ItemStack)this.armor.get(slot);
    }

    public void dropAll() {
        Iterator var1 = this.compartments.iterator();

        while(var1.hasNext()) {
            List<ItemStack> list = (List)var1.next();

            for(int i = 0; i < list.size(); ++i) {
                ItemStack itemStack = (ItemStack)list.get(i);
                if (!itemStack.isEmpty()) {
                    this.neko.drop(itemStack, true, false);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }

    }

    public void setChanged() {
        ++this.timesChanged;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    public boolean stillValid(Player player) {
        return player.canInteractWithEntity(this.neko, 4.0);
    }

    public boolean contains(ItemStack stack) {
        Iterator var2 = this.compartments.iterator();

        while(var2.hasNext()) {
            List<ItemStack> list = (List)var2.next();
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
                ItemStack itemStack = (ItemStack)var4.next();
                if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, stack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean contains(TagKey<Item> tag) {
        Iterator var2 = this.compartments.iterator();

        while(var2.hasNext()) {
            List<ItemStack> list = (List)var2.next();
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
                ItemStack itemStack = (ItemStack)var4.next();
                if (!itemStack.isEmpty() && itemStack.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean contains(Predicate<ItemStack> predicate) {
        Iterator var2 = this.compartments.iterator();

        while(var2.hasNext()) {
            List<ItemStack> list = (List)var2.next();
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
                ItemStack itemStack = (ItemStack)var4.next();
                if (predicate.test(itemStack)) {
                    return true;
                }
            }
        }

        return false;
    }


    public void clearContent() {
        Iterator var1 = this.compartments.iterator();

        while(var1.hasNext()) {
            List<ItemStack> list = (List)var1.next();
            list.clear();
        }

    }


    public ItemStack removeFromSelected(boolean removeStack) {
        ItemStack itemStack = this.getSelected();
        return itemStack.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, removeStack ? itemStack.getCount() : 1);
    }
}
