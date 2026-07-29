package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NekoEnergyBatteryItem extends Item {
    public static final String ID = "neko_energy_battery";
    public static final String ID_LARGE = "neko_energy_battery_large";

    private final int capacity;
    private final int dischargeRate;
    private final int chargeRate;

    public NekoEnergyBatteryItem(int capacity, int dischargeRate, int chargeRate) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.capacity = capacity;
        this.dischargeRate = dischargeRate;
        this.chargeRate = chargeRate;
    }

    // ========================
    //  储能读写
    // ========================

    public static float getStoredEnergy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("storedEnergy")) {
            return data.copyTag().getFloat("storedEnergy");
        }
        return 0;
    }

    public static void setStoredEnergy(ItemStack stack, float energy) {
        NekoEnergyBatteryItem battery = (NekoEnergyBatteryItem) stack.getItem();
        float clamped = Math.clamp(energy, 0, battery.capacity);
        CompoundTag tag = getOrCreateTag(stack);
        tag.putFloat("storedEnergy", clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getCapacity(ItemStack stack) {
        if (stack.getItem() instanceof NekoEnergyBatteryItem b) return b.capacity;
        return 0;
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    // ========================
    //  放电：每 20 tick 扫描所有在线玩家，从蓄能石给玩家补能
    // ========================

    public static void dischargeAllPlayers(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isNeko()) continue;
            float maxEnergy = player.getMaxNekoEnergy();
            float current = player.getNekoEnergy();
            if (current >= maxEnergy) continue;

            float deficit = maxEnergy - current;
            Inventory inv = player.getInventory();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!(stack.getItem() instanceof NekoEnergyBatteryItem battery)) continue;

                float stored = getStoredEnergy(stack);
                if (stored <= 0) continue;

                float transfer = Math.min(battery.dischargeRate, Math.min(stored, deficit));
                if (transfer <= 0) continue;

                setStoredEnergy(stack, stored - transfer);
                player.setNekoEnergy(current + transfer);

                current += transfer;
                deficit -= transfer;
                if (deficit <= 0) break;
            }
        }
    }

    // ========================
    //  充电：玩家能量再生溢出时存入蓄能石
    // ========================

    public static void chargeBatteries(Entity entity, float excess) {
        if (!(entity instanceof Player player) || !player.isNeko()) return;

        Inventory inv = player.getInventory();
        float remaining = excess;

        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof NekoEnergyBatteryItem battery)) continue;

            float stored = getStoredEnergy(stack);
            float space = battery.capacity - stored;
            if (space <= 0) continue;

            float transfer = Math.min(battery.chargeRate, Math.min(space, remaining));
            setStoredEnergy(stack, stored + transfer);
            remaining -= transfer;
        }
    }

    // ========================
    //  耐久条 = 充电进度
    // ========================

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getStoredEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        float stored = getStoredEnergy(stack);
        return Math.round(13.0f * stored / capacity);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        float ratio = getStoredEnergy(stack) / capacity;
        if (ratio > 0.7f) return 0x55FF55; // 绿
        if (ratio > 0.3f) return 0xFFFF55; // 黄
        return 0xFF5555; // 红
    }

    // ========================
    //  Tooltip
    // ========================

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        float stored = getStoredEnergy(stack);
        tooltip.add(Component.translatable("item.toneko.neko_energy_battery.tip", stored, capacity));
        tooltip.add(Component.translatable("item.toneko.neko_energy_battery.tip.rate", dischargeRate, chargeRate));
    }
}
