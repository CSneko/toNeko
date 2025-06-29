package org.cneko.toneko.common.mod.recipes;

import lombok.Getter;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.ArrayList;
import java.util.List;

public class NekoAggregatorInput implements RecipeInput {
    public static final NekoAggregatorInput EMPTY = new NekoAggregatorInput(0, 0, List.of(),0);
    private final int width;
    private final int height;
    private final List<ItemStack> items;
    private final StackedContents stackedContents = new StackedContents();
    private final int ingredientCount;
    @Getter
    private final double energy;

    private NekoAggregatorInput(int width, int height, List<ItemStack> item,double energy) {
        this.width = width;
        this.height = height;
        this.items = item;
        this.energy = energy;
        int i = 0;

        for(ItemStack itemStack : item) {
            if (!itemStack.isEmpty()) {
                ++i;
                this.stackedContents.accountStack(itemStack, 1);
            }
        }

        this.ingredientCount = i;
    }


    public static NekoAggregatorInput of(int width, int height, List<ItemStack> items,double energy) {
        return ofPositioned(width, height, items,energy).input();
    }

    public static NekoAggregatorInput.Positioned ofPositioned(int width, int height, List<ItemStack> items,double energy) {
        if (width != 0 && height != 0) {
            int i = width - 1;
            int j = 0;
            int k = height - 1;
            int l = 0;

            for(int m = 0; m < height; ++m) {
                boolean bl = true;

                for(int n = 0; n < width; ++n) {
                    ItemStack itemStack = (ItemStack)items.get(n + m * width);
                    if (!itemStack.isEmpty()) {
                        i = Math.min(i, n);
                        j = Math.max(j, n);
                        bl = false;
                    }
                }

                if (!bl) {
                    k = Math.min(k, m);
                    l = Math.max(l, m);
                }
            }

            int m = j - i + 1;
            int o = l - k + 1;
            if (m > 0 && o > 0) {
                if (m == width && o == height) {
                    return new NekoAggregatorInput.Positioned(new NekoAggregatorInput(width, height, items,energy), i, k);
                } else {
                    List<ItemStack> list = new ArrayList<>(m * o);

                    for(int p = 0; p < o; ++p) {
                        for(int q = 0; q < m; ++q) {
                            int r = q + i + (p + k) * width;
                            list.add(items.get(r));
                        }
                    }

                    return new NekoAggregatorInput.Positioned(new NekoAggregatorInput(m, o, list,energy), i, k);
                }
            } else {
                return NekoAggregatorInput.Positioned.EMPTY;
            }
        } else {
            return NekoAggregatorInput.Positioned.EMPTY;
        }
    }

    public ItemStack getItem(int index) {
        return (ItemStack)this.items.get(index);
    }

    public ItemStack getItem(int row, int column) {
        return (ItemStack)this.items.get(row + column * this.width);
    }

    public int size() {
        return this.items.size();
    }

    public boolean isEmpty() {
        return this.ingredientCount == 0;
    }

    public StackedContents stackedContents() {
        return this.stackedContents;
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public int ingredientCount() {
        return this.ingredientCount;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof NekoAggregatorInput craftingInput)) {
            return false;
        } else {
            return this.width == craftingInput.width && this.height == craftingInput.height && this.ingredientCount == craftingInput.ingredientCount && ItemStack.listMatches(this.items, craftingInput.items);
        }
    }

    public int hashCode() {
        int i = ItemStack.hashStackList(this.items);
        i = 31 * i + this.width;
        i = 31 * i + this.height;
        return i;
    }

    public static record Positioned(NekoAggregatorInput input, int left, int top) {
        public static final NekoAggregatorInput.Positioned EMPTY;

        static {
            EMPTY = new NekoAggregatorInput.Positioned(NekoAggregatorInput.EMPTY, 0, 0);
        }
    }
}
