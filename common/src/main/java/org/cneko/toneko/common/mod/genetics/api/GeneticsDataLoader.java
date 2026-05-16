package org.cneko.toneko.common.mod.genetics.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 从数据包加载遗传学数据（等位基因、基因座、核型修改）。
 * 监听 {namespace}/toneko_genetics/ 目录下的 JSON 文件，支持 /reload 热重载。
 */
public class GeneticsDataLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final ResourceLocation ID = toNekoLoc("genetics_data_loader");
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneticsDataLoader.class);

    public GeneticsDataLoader() {
        super(GSON, "toneko_genetics");
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        // 清除上一次加载的动态数据
        GeneticsRegistry.clearDynamicData();

        // Phase 1: 加载所有等位基因
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            String path = entry.getKey().getPath();
            if (!path.startsWith("alleles/")) continue;
            ResourceLocation id = filePathToId(entry.getKey(), "alleles/");
            try {
                loadAllele(id, entry.getValue().getAsJsonObject());
            } catch (Exception e) {
                LOGGER.warn("无法加载等位基因 {}: {}", id, e.getMessage());
            }
        }

        // Phase 2: 加载所有基因座
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            String path = entry.getKey().getPath();
            if (!path.startsWith("loci/")) continue;
            ResourceLocation id = filePathToId(entry.getKey(), "loci/");
            try {
                loadLocus(id, entry.getValue().getAsJsonObject());
            } catch (Exception e) {
                LOGGER.warn("无法加载基因座 {}: {}", id, e.getMessage());
            }
        }

        // Phase 3: 加载所有核型补丁（此时基因座已就绪）
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            String path = entry.getKey().getPath();
            if (!path.startsWith("karyotypes/")) continue;
            try {
                loadKaryotypePatch(entry.getValue().getAsJsonObject());
            } catch (Exception e) {
                LOGGER.warn("无法应用核型补丁 {}: {}", entry.getKey(), e.getMessage());
            }
        }

        LOGGER.info("遗传学数据包加载完成：{} 个等位基因, {} 个基因座",
                GeneticsRegistry.DYNAMIC_ALLELES.size(), GeneticsRegistry.DYNAMIC_LOCI.size());
    }

    /**
     * 将文件路径（如 "alleles/super_jump"）转换为 ResourceLocation。
     * 命名空间取自文件所属数据包，路径为文件名部分。
     */
    private ResourceLocation filePathToId(ResourceLocation fileId, String prefix) {
        String name = fileId.getPath().substring(prefix.length());
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), name);
    }

    /**
     * 从 JSON 加载单个等位基因。
     * <p>JSON 格式示例：</p>
     * <pre>
     * {
     *   "dominance": 20,
     *   "attributes": [
     *     {
     *       "attribute": "minecraft:generic.movement_speed",
     *       "suffix": "speed_boost",
     *       "operation": "add_value",
     *       "amount": 0.05
     *     }
     *   ],
     *   "wild_pool": [
     *     { "locus": "toneko:speed_slot_0", "weight": 10 }
     *   ]
     * }
     * </pre>
     */
    private void loadAllele(ResourceLocation id, JsonObject json) {
        int dominance = 10;
        if (json.has("dominance")) {
            dominance = json.get("dominance").getAsInt();
        }

        Allele allele = new Allele(id, dominance, null, null);

        // 解析属性修饰符
        if (json.has("attributes")) {
            for (JsonElement elem : json.getAsJsonArray("attributes")) {
                JsonObject attrJson = elem.getAsJsonObject();
                String attrId = attrJson.get("attribute").getAsString();
                String suffix = attrJson.get("suffix").getAsString();
                String operationStr = attrJson.get("operation").getAsString();
                double amount = attrJson.get("amount").getAsDouble();

                AttributeModifier.Operation op = switch (operationStr) {
                    case "add_value" -> AttributeModifier.Operation.ADD_VALUE;
                    case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    default -> {
                        LOGGER.warn("等位基因 {} 包含未知操作类型: {}", id, operationStr);
                        yield null;
                    }
                };
                if (op == null) continue;

                // 解析属性引用
                ResourceLocation attrRl = ResourceLocation.parse(attrId);
                ResourceKey<Attribute> attrKey = ResourceKey.create(Registries.ATTRIBUTE, attrRl);
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attrKey).orElse(null);
                if (attribute == null) {
                    LOGGER.warn("等位基因 {} 引用了不存在的属性: {}", id, attrId);
                    continue;
                }

                allele.addAttributeModifier(attribute, suffix, amount, op);
            }
        }

        GeneticsRegistry.registerAllele(allele);
        GeneticsRegistry.DYNAMIC_ALLELES.add(id);

        // 解析野生基因池
        if (json.has("wild_pool")) {
            for (JsonElement elem : json.getAsJsonArray("wild_pool")) {
                JsonObject poolEntry = elem.getAsJsonObject();
                ResourceLocation locusId = ResourceLocation.parse(poolEntry.get("locus").getAsString());
                int weight = poolEntry.get("weight").getAsInt();
                GeneticsRegistry.addWildAllele(locusId, id, weight);
                GeneticsRegistry.DYNAMIC_WILD_POOLS
                        .computeIfAbsent(locusId, k -> new HashSet<>())
                        .add(id);
            }
        }
    }

    /**
     * 从 JSON 加载单个基因座。
     * <p>JSON 格式示例：</p>
     * <pre>
     * {
     *   "wild_pool": [
     *     { "allele": "toneko:wild_type", "weight": 70 },
     *     { "allele": "mymod:super_speed", "weight": 30 }
     *   ]
     * }
     * </pre>
     */
    private void loadLocus(ResourceLocation id, JsonObject json) {
        Locus locus = new Locus(id);
        GeneticsRegistry.registerLocus(locus);
        GeneticsRegistry.DYNAMIC_LOCI.add(id);

        // 解析野生基因池
        if (json.has("wild_pool")) {
            for (JsonElement elem : json.getAsJsonArray("wild_pool")) {
                JsonObject poolEntry = elem.getAsJsonObject();
                ResourceLocation alleleId = ResourceLocation.parse(poolEntry.get("allele").getAsString());
                int weight = poolEntry.get("weight").getAsInt();

                // 等位基因可能尚未加载（不同数据包加载顺序），但 GeneticRegistry 中应已存在
                if (GeneticsRegistry.getAllele(alleleId) == null) {
                    LOGGER.warn("基因座 {} 的野生池引用了不存在的等位基因: {}", id, alleleId);
                    continue;
                }

                GeneticsRegistry.addWildAllele(id, alleleId, weight);
                GeneticsRegistry.DYNAMIC_WILD_POOLS
                        .computeIfAbsent(id, k -> new HashSet<>())
                        .add(alleleId);
            }
        }
    }

    /**
     * 从 JSON 加载核型补丁。
     * <p>JSON 格式示例：</p>
     * <pre>
     * {
     *   "target": "toneko:neko",
     *   "add_loci": {
     *     "9": ["mymod:custom_locus", "mymod:another_locus"],
     *     "10": ["mymod:third_locus"]
     *   }
     * }
     * </pre>
     */
    private void loadKaryotypePatch(JsonObject json) {
        if (!json.has("target")) {
            LOGGER.warn("核型补丁缺少 'target' 字段");
            return;
        }
        ResourceLocation targetId = ResourceLocation.parse(json.get("target").getAsString());
        SpeciesKaryotype karyotype = GeneticsRegistry.getKaryotypeById(targetId);
        if (karyotype == null) {
            LOGGER.warn("核型补丁指向了不存在的核型: {}", targetId);
            return;
        }

        if (json.has("add_loci")) {
            JsonObject addLoci = json.getAsJsonObject("add_loci");
            for (String chrKey : addLoci.keySet()) {
                int chromosomeId;
                try {
                    chromosomeId = Integer.parseInt(chrKey);
                } catch (NumberFormatException e) {
                    LOGGER.warn("核型补丁包含无效的染色体编号: {}", chrKey);
                    continue;
                }

                // 确保核型支持该染色体编号（自动扩展）
                karyotype.ensureChromosomeCapacity(chromosomeId);

                for (JsonElement elem : addLoci.getAsJsonArray(chrKey)) {
                    ResourceLocation locusId = ResourceLocation.parse(elem.getAsString());
                    Locus locus = GeneticsRegistry.getLocus(locusId);
                    if (locus == null) {
                        LOGGER.warn("核型补丁引用了不存在的基因座: {}", locusId);
                        continue;
                    }
                    karyotype.bindLocus(chromosomeId, locus);
                }
            }
        }
    }
}