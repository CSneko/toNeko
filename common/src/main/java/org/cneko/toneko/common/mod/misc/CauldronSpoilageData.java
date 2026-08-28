package org.cneko.toneko.common.mod.misc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.cneko.toneko.common.mod.items.SpoiledWaterBucketItem;
import org.cneko.toneko.common.mod.util.ResourceLocationUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 炼药锅变质水的存档数据：记录每个炼药锅位置的水质变质程度（0~100）与气味来源。
 * 以 ServerLevel 的 SavedData 持久化，服务器重启后仍在。
 */
public class CauldronSpoilageData extends SavedData {
    /** 一锅变质水：等级 + 气味来源（最近穿着者显示名，空串=无） */
    public record SpoiledWater(int level, String wearer) {
        public static final Codec<SpoiledWater> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("spoilage").forGetter(SpoiledWater::level),
                Codec.STRING.fieldOf("wearer").forGetter(SpoiledWater::wearer)
        ).apply(instance, SpoiledWater::new));
    }

    private static final Codec<Map<BlockPos, SpoiledWater>> CAULDRONS_CODEC =
            ExtraCodecs.strictUnboundedMap(BlockPos.CODEC, SpoiledWater.CODEC);

    private static final Codec<CauldronSpoilageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CAULDRONS_CODEC.fieldOf("cauldrons").forGetter(data -> data.spoilage)
    ).apply(instance, spoilage -> new CauldronSpoilageData(spoilage)));

    // 26.x：SavedData.Factory 改为 SavedDataType（Identifier + 构造器 + Codec）
    public static final SavedDataType<CauldronSpoilageData> TYPE = new SavedDataType<>(
            ResourceLocationUtil.toNekoLoc("toneko_spoiled_cauldrons"),
            CauldronSpoilageData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<BlockPos, SpoiledWater> spoilage;

    private CauldronSpoilageData(Map<BlockPos, SpoiledWater> spoilage) {
        this.spoilage = spoilage;
    }

    /** SavedDataType 需要的默认构造器 */
    public CauldronSpoilageData() {
        this.spoilage = new HashMap<>();
    }

    public static CauldronSpoilageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
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
}
