package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.cneko.toneko.common.mod.items.SpoiledWaterBucketItem;

import java.util.HashMap;
import java.util.Map;

/**
 * 炼药锅变质水的存档数据：记录每个炼药锅位置的水质变质程度（0~100）与气味来源。
 * 以 ServerLevel 的 SavedData 持久化，服务器重启后仍在。
 */
public class CauldronSpoilageData extends SavedData {
    private static final String KEY = "toneko_spoiled_cauldrons";
    private static final SavedData.Factory<CauldronSpoilageData> FACTORY = new SavedData.Factory<>(
            CauldronSpoilageData::new,
            CauldronSpoilageData::load,
            DataFixTypes.LEVEL
    );

    /** 一锅变质水：等级 + 气味来源（最近穿着者显示名，空串=无） */
    public record SpoiledWater(int level, String wearer) {}

    private final Map<BlockPos, SpoiledWater> spoilage = new HashMap<>();

    public static CauldronSpoilageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public int getSpoilage(BlockPos pos) {
        SpoiledWater water = spoilage.get(pos);
        return water == null ? 0 : water.level();
    }

    public String getWearer(BlockPos pos) {
        SpoiledWater water = spoilage.get(pos);
        return water == null ? "" : water.wearer();
    }

    public SpoiledWater getWater(BlockPos pos) {
        SpoiledWater water = spoilage.get(pos);
        return water == null ? new SpoiledWater(0, "") : water;
    }

    public void setSpoilage(BlockPos pos, int value) {
        SpoiledWater old = spoilage.get(pos);
        setSpoilage(pos, value, old == null ? "" : old.wearer());
    }

    public void setSpoilage(BlockPos pos, int value, String wearer) {
        int clamped = Mth.clamp(value, 0, SpoiledWaterBucketItem.MAX_SPOILAGE);
        String w = wearer == null ? "" : wearer;
        SpoiledWater previous = spoilage.get(pos);
        if (previous != null && previous.level() == clamped && previous.wearer().equals(w)) return;
        if (clamped <= 0) {
            if (spoilage.remove(pos) != null) setDirty();
        } else {
            spoilage.put(pos, new SpoiledWater(clamped, w));
            setDirty();
        }
    }

    public void clearSpoilage(BlockPos pos) {
        if (spoilage.remove(pos) != null) setDirty();
    }

    public Map<BlockPos, SpoiledWater> getSpoilageMap() {
        return spoilage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, SpoiledWater> entry : spoilage.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            entryTag.putInt("x", pos.getX());
            entryTag.putInt("y", pos.getY());
            entryTag.putInt("z", pos.getZ());
            entryTag.putInt("spoilage", entry.getValue().level());
            entryTag.putString("wearer", entry.getValue().wearer());
            list.add(entryTag);
        }
        tag.put("cauldrons", list);
        return tag;
    }

    public static CauldronSpoilageData load(CompoundTag tag, HolderLookup.Provider registries) {
        CauldronSpoilageData data = new CauldronSpoilageData();
        ListTag list = tag.getList("cauldrons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z"));
            int value = Mth.clamp(entryTag.getInt("spoilage"), 0, SpoiledWaterBucketItem.MAX_SPOILAGE);
            String wearer = entryTag.getString("wearer");
            data.spoilage.put(pos, new SpoiledWater(value, wearer));
        }
        return data;
    }
}
