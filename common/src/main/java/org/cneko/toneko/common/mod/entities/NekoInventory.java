package org.cneko.toneko.common.mod.entities;

import com.google.common.collect.EvictingQueue;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public class NekoInventory {
    public static final int INVENTORY_SIZE = 27;

    public final NekoEntity neko;
    private final EvictingQueue<ItemStack> inventory;
    private final int maxSize;
    public NekoInventory(NekoEntity neko){
        this(neko, INVENTORY_SIZE);
    }
    public NekoInventory(NekoEntity neko, int size){
        this.neko = neko;
        this.maxSize = size;
        this.inventory = EvictingQueue.create(size);
    }
    public void addItem(ItemStack stack){
        if (inventory.size()< maxSize){
            inventory.add(stack);
        }else {
            // 掉落物品
            neko.spawnAtLocation(stack);
        }

    }
    public void removeItem(ItemStack stack){
        inventory.remove(stack);
    }

    public List<ItemStack> getItems(){
        return inventory.stream().toList();
    }
    public boolean isEmpty(){
        return inventory.isEmpty();
    }
    public boolean contains(ItemStack stack){
        return inventory.contains(stack);
    }
    public CompoundTag getNBT(){
        CompoundTag nbt = new CompoundTag();
        int i = 0;
        for (ItemStack item : inventory){
            CompoundTag itemNbt = new CompoundTag();
            item.getComponents().forEach((component)->{
                String key = component.type().toString();
                String value = item.getOrDefault(component.type(), component.type()).toString();
                itemNbt.putString(key, value);
            });
            nbt.put(i+"", itemNbt);
            i++;
        }
        return nbt;
    }



    public void clear(){
        inventory.clear();
    }
    public EvictingQueue<ItemStack> getInventory(){
        return inventory;
    }

}
