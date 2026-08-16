package org.cneko.toneko.common.mod.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.mod.misc.ZettaiRyouiki;
import org.cneko.toneko.common.util.LanguageUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.mod.ai.PromptRegistry.register;
import static org.cneko.toneko.common.mod.ai.PromptRegistry.PromptFactory;
public class Prompts {
    /**
     * 翻译 key 为可读文本（走模组自己的服务端翻译 LanguageUtil）；
     * 未命中时返回可读化的 key（去命名空间/前缀、下划线转空格），
     * 保证模型看到的永远是文本而不是原始 id（如 minecraft:forest / quirk 键）。
     */
    static String translateOrReadable(String key, Object... args) {
        String text;
        if (LanguageUtil.LANG != null && LanguageUtil.LANG.contains(key)) {
            text = LanguageUtil.LANG.getString(key);
        } else if (LanguageUtil.EN_US_LANG != null && LanguageUtil.EN_US_LANG.contains(key)) {
            text = LanguageUtil.EN_US_LANG.getString(key);
        } else {
            int idx = Math.max(key.lastIndexOf('.'), key.lastIndexOf(':'));
            text = idx >= 0 ? key.substring(idx + 1) : key;
            text = text.replace('_', ' ');
        }
        return args.length > 0 ? String.format(text, args) : text;
    }

    // ===== 身份 =====
    public static final PromptFactory NEKO_NAME = (neko,other)-> {
        // 优先使用玩家设置的昵称（AI 对话中猫娘自称的名字与昵称一致），未设置时回退实体名
        String nick = neko.getNickName();
        return (nick != null && !nick.isEmpty()) ? nick : neko.getName().getString();
    };
    public static final PromptFactory NEKO_TYPE = (neko,other)-> translateOrReadable(neko.getTypeName().getString());
    public static final PromptFactory NEKO_DES = (neko,other)-> translateOrReadable(neko.getDescription());
    public static final PromptFactory NEKO_HEIGHT = (neko,other)-> new DecimalFormat("0.00").format(neko.getBbHeight());
    /**
     * 般配的萌属性组合：组合内的萌属性一起出现时使用专门的组合描述
     * （键名 moe.toneko.combo.&lt;t1&gt;_&lt;t2&gt;，组合内需按字母序排列）。
     * 优先匹配最长的组合（如三属性组合优先于其中的两两组合）。
     */
    private static final List<List<String>> MOE_COMBOS = List.of(
            List.of("baka", "dojikko"),
            List.of("baka", "narenareshi"),
            List.of("baka", "tennen_boke"),
            List.of("baka", "dojikko", "tennen_boke"),
            List.of("chunibyo", "haraguro"),
            List.of("chunibyo", "narenareshi"),
            List.of("dojikko", "tennen_boke"),
            List.of("gentleness", "shizukana"),
            List.of("gentleness", "yowaki"),
            List.of("haraguro", "shoakuma"),
            List.of("haraguro", "mesugaki", "shoakuma"),
            List.of("mesugaki", "shoakuma"),
            List.of("mesugaki", "tsundere"),
            List.of("shizukana", "yowaki"),
            List.of("tsundere", "yandere"),
            List.of("yandere", "paranoia"),
            List.of("yandere", "yuri"),
            List.of("gentleness", "shizukana", "yowaki")
    );
    public static final PromptFactory NEKO_MOE_TAGS = (neko,other)-> {
        // 般配组合优先使用组合描述，其余使用单个萌属性的详细描述
        List<String> tags = new ArrayList<>(neko.getMoeTags());
        List<String> parts = new ArrayList<>();
        while (!tags.isEmpty()) {
            String first = tags.remove(0);
            // 找包含 first 的最长组合（其余成员都还在剩余列表里）
            List<String> bestCombo = null;
            for (List<String> combo : MOE_COMBOS) {
                if (!combo.contains(first)) continue;
                if (bestCombo != null && combo.size() <= bestCombo.size()) continue;
                if (tags.containsAll(combo.subList(1, combo.size()))) {
                    bestCombo = combo;
                }
            }
            if (bestCombo != null) {
                for (int i = 1; i < bestCombo.size(); i++) {
                    tags.remove(bestCombo.get(i));
                }
                parts.add(translateOrReadable("moe.toneko.combo." + String.join("_", bestCombo)));
            } else {
                parts.add(translateOrReadable("moe.toneko.desc." + first));
            }
        }
        return String.join(",", parts);
    };

    // ===== 状态（描述型，让对话感知猫娘的状态） =====
    public static final PromptFactory NEKO_LEVEL = (neko,other)-> String.valueOf((int) neko.getNekoLevel());
    public static final PromptFactory NEKO_IS_BABY = (neko,other)-> translateOrReadable("misc.toneko.is_or_not." + (neko.isNekoBaby() ? "is" : "not"));
    /** 精力状态：根据能量比例分级描述 */
    public static final PromptFactory NEKO_ENERGY_STATE = (neko,other)-> {
        float ratio = neko.getMaxNekoEnergy() > 0 ? neko.getNekoEnergy() / neko.getMaxNekoEnergy() : 1.0f;
        if (ratio >= 0.75f) return translateOrReadable("misc.toneko.energy_state.energetic");
        if (ratio >= 0.5f) return translateOrReadable("misc.toneko.energy_state.tired");
        if (ratio >= 0.25f) return translateOrReadable("misc.toneko.energy_state.very_tired");
        return translateOrReadable("misc.toneko.energy_state.exhausted");
    };
    /** 健康状态：根据血量比例分级描述 */
    public static final PromptFactory NEKO_HEALTH_STATE = (neko,other)-> {
        float ratio = neko.getEntity().getMaxHealth() > 0 ? neko.getEntity().getHealth() / neko.getEntity().getMaxHealth() : 1.0f;
        if (ratio >= 0.75f) return translateOrReadable("misc.toneko.health_state.healthy");
        if (ratio >= 0.5f) return translateOrReadable("misc.toneko.health_state.slight");
        if (ratio >= 0.25f) return translateOrReadable("misc.toneko.health_state.hurt");
        return translateOrReadable("misc.toneko.health_state.severe");
    };
    /** 综合心情：由精力与健康状态加权得出 */
    public static final PromptFactory NEKO_MOOD = (neko,other)-> {
        float energyRatio = neko.getMaxNekoEnergy() > 0 ? neko.getNekoEnergy() / neko.getMaxNekoEnergy() : 1.0f;
        float healthRatio = neko.getEntity().getMaxHealth() > 0 ? neko.getEntity().getHealth() / neko.getEntity().getMaxHealth() : 1.0f;
        float mood = (energyRatio + healthRatio) / 2;
        if (mood >= 0.75f) return translateOrReadable("misc.toneko.mood.great");
        if (mood >= 0.5f) return translateOrReadable("misc.toneko.mood.good");
        if (mood >= 0.25f) return translateOrReadable("misc.toneko.mood.low");
        return translateOrReadable("misc.toneko.mood.bad");
    };
    /** 背包内容：物品id x 数量（让 AI 知道自己有什么可以给予，id 与动作 JSON 的 item 字段一致） */
    public static final PromptFactory NEKO_INVENTORY = (neko, other)-> {
        List<String> parts = new ArrayList<>();
        int count = 0;
        for (ItemStack stack : neko.getInventory().items) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            parts.add(id + " x " + stack.getCount());
            if (++count >= 12) {
                parts.add(LanguageUtil.translatable("misc.toneko.inventory.etc") + count);
                break;
            }
        }
        return parts.isEmpty()
                ? LanguageUtil.translatable("misc.toneko.inventory.empty")
                : String.join(",", parts);
    };
    /** 跟随状态：猫娘当前是否正跟随某位玩家 */
    public static final PromptFactory NEKO_FOLLOWING = (neko,other)-> {
        var goal = neko.getFollowingOwner();
        if (goal != null && goal.getOwner() != null) {
            return translateOrReadable("misc.toneko.following.following",
                    goal.getOwner().getName().getString());
        }
        return translateOrReadable("misc.toneko.following.alone");
    };

    // ===== 玩家 =====
    public static final PromptFactory PLAYER_NAME = (neko,other)-> other.getEntity().getName().getString();
    public static final PromptFactory PLAYER_IS_OWNER = (neko,other)-> translateOrReadable("misc.toneko.is_or_not." + (neko.hasOwner(other.getEntity().getUUID()) ? "is" : "not"));
    public static final PromptFactory PLAYER_IS_NEKO = (neko,other)-> translateOrReadable("misc.toneko.is_or_not." + (other.isNeko() ? "is" : "not"));
    /** 玩家健康状态 */
    public static final PromptFactory PLAYER_HEALTH_STATE = (neko,other)-> {
        float ratio = other.getEntity().getMaxHealth() > 0 ? other.getEntity().getHealth() / other.getEntity().getMaxHealth() : 1.0f;
        if (ratio >= 0.5f) return translateOrReadable("misc.toneko.player_health_state.ok");
        if (ratio >= 0.25f) return translateOrReadable("misc.toneko.player_health_state.hurt");
        return translateOrReadable("misc.toneko.player_health_state.severe");
    };
    /** 玩家腿部穿搭（丝袜款式/D值/袜口高度/绝对领域等级 + 气味/湿度），用于 AI 评论穿搭 */
    public static final PromptFactory PLAYER_OUTFIT = (neko,other)-> {
        if (other == null || other.getEntity() == null) return "";
        ItemStack legs = LegwearUtil.getWornLegwear(other.getEntity());
        if (!LegwearItem.isLegwear(legs)) return translateOrReadable("misc.toneko.player_outfit.none");
        String itemName = translateOrReadable(BuiltInRegistries.ITEM.getKey(legs.getItem()).toLanguageKey());
        String grade = translateOrReadable("item.toneko.legwear.zettai_ryouiki." + ZettaiRyouiki.compute(legs));
        String desc = translateOrReadable("misc.toneko.player_outfit.desc",
                itemName,
                LegwearItem.getDenier(legs),
                Math.round(LegwearItem.getStockingTopHeight(legs) * 100),
                grade);

        StringBuilder sb = new StringBuilder(desc);
        int scent = ScentUtil.getIntensity(legs);
        if (scent > 0) {
            String wearer = ScentUtil.getWearer(legs);
            sb.append(translateOrReadable("misc.toneko.player_outfit.scent",
                    translateOrReadable("item.toneko.legwear.scent." + ScentUtil.grade(scent)),
                    wearer == null || wearer.isEmpty() ? "?" : wearer));
        }
        int wetness = WetnessUtil.get(legs);
        if (wetness > 0) {
            sb.append(translateOrReadable("misc.toneko.player_outfit.wet",
                    translateOrReadable("item.toneko.legwear.wetness." + WetnessUtil.grade(wetness))));
        }
        return sb.toString();
    };

    // ===== 世界 =====
    public static final PromptFactory WORLD_TIME = (neko,other)-> translateOrReadable("misc.toneko.time." + (neko.level().isDay() ? "day" : "night"));
    public static final PromptFactory WORLD_WEATHER = (neko,other)-> translateOrReadable("misc.toneko.weather." + ((neko.level().isRainingAt(neko.blockPosition()) || neko.level().isThundering()) ? "rain" : "sunny"));
    /** 当前维度（翻译为"主世界/下界/末地"等文本） */
    public static final PromptFactory WORLD_DIMENSION = (neko,other)-> {
        String dimKey = neko.level().dimension().location().getPath();
        return translateOrReadable("dimension.toneko." + dimKey);
    };
    /** 当前生物群系（翻译为"森林/沙漠"等文本） */
    public static final PromptFactory WORLD_BIOME = (neko,other)-> {
        String biome = neko.level().getBiome(neko.blockPosition()).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("unknown");
        return translateOrReadable("biome.toneko." + biome);
    };

    // ===== 环境感知 =====
    /** 周围环境感知：附近实体（玩家/猫娘/怪物/动物）+ 环境特征，由 {@link SurroundingsScanner} 扫描生成 */
    public static final PromptFactory NEKO_SURROUNDINGS = (neko, other) -> SurroundingsScanner.describe(neko, other);

    // ===== 日记 =====
    /** 猫娘日记上下文（最近几篇，token 受控）：让 AI 写新日记时参考已有内容保持风格 */
    public static final PromptFactory NEKO_DIARY = (neko, other) ->
            NekoDiary.buildContext(neko.getDiaryEntries(), 2, 100);
    /** 月相：0=满月，4=新月 */
    public static final PromptFactory WORLD_PHASE = (neko,other)-> {
        int phase = neko.level().getMoonPhase();
        if (phase == 0) return translateOrReadable("misc.toneko.moon_phase.full");
        if (phase == 4) return translateOrReadable("misc.toneko.moon_phase.new");
        if (phase < 4) return translateOrReadable("misc.toneko.moon_phase.waning");
        return translateOrReadable("misc.toneko.moon_phase.waxing");
    };

    public static void init() {
        // 身份
        register("neko_name",NEKO_NAME);
        register("neko_type",NEKO_TYPE);
        register("neko_des",NEKO_DES);
        register("neko_height",NEKO_HEIGHT);
        register("neko_moe_tags",NEKO_MOE_TAGS);
        // 状态
        register("neko_level",NEKO_LEVEL);
        register("neko_is_baby",NEKO_IS_BABY);
        register("neko_energy_state",NEKO_ENERGY_STATE);
        register("neko_health_state",NEKO_HEALTH_STATE);
        register("neko_mood",NEKO_MOOD);
        register("neko_following",NEKO_FOLLOWING);
        register("neko_inventory",NEKO_INVENTORY);
        // 玩家
        register("player_name",PLAYER_NAME);
        register("player_is_owner",PLAYER_IS_OWNER);
        register("player_is_neko",PLAYER_IS_NEKO);
        register("player_health_state",PLAYER_HEALTH_STATE);
        register("player_outfit",PLAYER_OUTFIT);
        // 世界
        register("world_time",WORLD_TIME);
        register("world_weather",WORLD_WEATHER);
        register("world_dimension",WORLD_DIMENSION);
        register("world_biome",WORLD_BIOME);
        register("world_phase",WORLD_PHASE);
        // 环境感知
        register("neko_surroundings",NEKO_SURROUNDINGS);
        // 日记
        register("neko_diary",NEKO_DIARY);
    }
}
