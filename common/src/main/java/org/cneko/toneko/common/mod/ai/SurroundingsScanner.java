package org.cneko.toneko.common.mod.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.cneko.toneko.common.mod.ai.Prompts.translateOrReadable;
import static org.cneko.toneko.common.mod.ai.PromptRegistry.sanitize;

/**
 * 环境感知扫描器：生成 AI 猫娘当前感知到的周围信息（附近实体 + 环境特征）。
 * <p>
 * 设计目标：
 * - 只输出"有/无/数量"类事实，不列方块 id 清单——token 可控、不泄露过多信息；
 * - 事实部分先裁剪到 {@link #MAX_FACTS_LENGTH}，防幻觉的 instruction 恒在末尾且恒完整；
 * - 必须在服务器主线程调用（Level.getEntities / getBlockState 均为主线程操作）。
 */
public final class SurroundingsScanner {
    // ---- 扫描范围（与区域聊天 NEKO_AI_RANGE 语义一致） ----
    private static final double ENTITY_RANGE = 16.0;
    private static final int BLOCK_RANGE = 12;
    private static final int BLOCK_STEP = 3;
    private static final int ORE_RANGE = 8;
    private static final int ORE_STEP = 2;
    // ---- 数量上限（防 token 膨胀） ----
    private static final int MAX_PLAYERS = 3;
    private static final int MAX_NEKOS = 3;
    private static final int MAX_MONSTER_GROUPS = 3;
    private static final int MAX_ANIMAL_GROUPS = 2;
    private static final int MAX_OTHERS = 2;
    private static final int MAX_FACTS_LENGTH = 200;
    /** 玩家/猫娘昵称等玩家可控文本的显示长度上限 */
    private static final int MAX_NAME_LENGTH = 32;
    /** 人工建筑信号方块（火把/箱子/工作台/熔炉/铁砧/附魔台等） */
    private static final Set<Block> BUILDING_BLOCKS = Set.of(
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL,
            Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.ANVIL, Blocks.ENCHANTING_TABLE
    );
    /** 矿石 tag 列表（仅地下时扫描） */
    private static final List<TagKey<Block>> ORE_TAGS = List.of(
            BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.GOLD_ORES,
            BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.DIAMOND_ORES,
            BlockTags.EMERALD_ORES, BlockTags.COPPER_ORES
    );

    // ---- 缓存：同一猫娘 5 秒内且位移 < 8 格直接复用（同批次多只猫娘触发时最多 1 次真扫描） ----
    private static final long CACHE_TTL_MS = 5000;
    private static final double CACHE_MOVE_SQ = 64.0;
    private static final Map<UUID, CachedScan> CACHE = new ConcurrentHashMap<>();

    private record CachedScan(long time, BlockPos pos, String text) {}

    private SurroundingsScanner() {}

    /**
     * 主入口：返回猫娘当前感知文本段（可为空串）。缓存命中直接返回，否则扫描并缓存。
     *
     * @param speaker 正在说话的一方（通常为玩家），会被从"附近玩家"列表中排除
     */
    public static String describe(NekoEntity neko, INeko speaker) {
        if (neko == null) return "";
        long now = System.currentTimeMillis();
        CachedScan cached = CACHE.get(neko.getUUID());
        if (cached != null
                && now - cached.time() < CACHE_TTL_MS
                && neko.blockPosition().distSqr(cached.pos()) < CACHE_MOVE_SQ) {
            return cached.text();
        }
        String text = scan(neko, speaker);
        CACHE.put(neko.getUUID(), new CachedScan(now, neko.blockPosition(), text));
        if (CACHE.size() > 128) {
            CACHE.entrySet().removeIf(e -> now - e.getValue().time() > CACHE_TTL_MS);
        }
        return text;
    }

    private static String scan(NekoEntity neko, INeko speaker) {
        List<String> facts = new ArrayList<>();
        String entities = scanEntities(neko, speaker);
        String blocks = scanBlocks(neko);
        if (!entities.isEmpty()) facts.add(entities);
        if (!blocks.isEmpty()) facts.add(translateOrReadable("misc.toneko.surroundings.blocks", blocks));
        String factText = String.join(translateOrReadable("misc.toneko.surroundings.separator"), facts);
        // 先裁剪事实部分：instruction 恒在末尾且恒完整，不受任何截断影响
        if (factText.length() > MAX_FACTS_LENGTH) {
            factText = factText.substring(0, MAX_FACTS_LENGTH);
        }
        StringBuilder sb = new StringBuilder(translateOrReadable("misc.toneko.surroundings.intro"));
        sb.append(factText.isEmpty() ? translateOrReadable("misc.toneko.surroundings.empty") : factText);
        sb.append('\n').append(translateOrReadable("misc.toneko.surroundings.instruction"));
        return sb.toString();
    }

    /**
     * 附近实体：单趟 getEntities 查询后分类。分类顺序必须为 Player → NekoEntity → Monster → Animal → 其他：
     * NekoEntity 继承 AgeableMob（不是 Animal）；村民/铁傀儡等是 AbstractVillager/AbstractGolem（也不是 Animal），
     * 会掉进"其他生物"桶，因此必须保留第 5 桶。
     */
    private static String scanEntities(NekoEntity neko, INeko speaker) {
        Level level = neko.level();
        if (level == null) return "";
        AABB box = neko.getBoundingBox().inflate(ENTITY_RANGE);
        // except = neko 排除猫娘本体
        List<Entity> found = level.getEntities(neko, box, e -> e.isAlive() && e instanceof LivingEntity);
        found.sort(Comparator.comparingDouble(e -> neko.distanceToSqr(e.getX(), e.getY(), e.getZ())));

        UUID speakerUuid = null;
        if (speaker != null && speaker.getEntity() instanceof Player sp) speakerUuid = sp.getUUID();

        List<String> players = new ArrayList<>();
        List<String> nekos = new ArrayList<>();
        Map<String, Integer> monsters = new LinkedHashMap<>();
        Map<String, Integer> animals = new LinkedHashMap<>();
        List<String> others = new ArrayList<>();
        for (Entity e : found) {
            if (e instanceof Player p) {
                // 排除说话者（对话对象已知）与旁观者
                if (speakerUuid != null && p.getUUID().equals(speakerUuid)) continue;
                if (p.isSpectator()) continue;
                if (players.size() < MAX_PLAYERS) {
                    players.add(sanitize(p.getName().getString(), MAX_NAME_LENGTH));
                }
            } else if (e instanceof NekoEntity n) {
                if (nekos.size() < MAX_NEKOS) {
                    String nick = n.getNickName();
                    String name = (nick != null && !nick.isEmpty()) ? nick : n.getName().getString();
                    // 附加表现状态（综合心情：精力+健康加权），让 AI 感知到附近猫娘的心情
                    String mood = Prompts.NEKO_MOOD.getPrompt(n, null);
                    nekos.add(translateOrReadable("misc.toneko.surroundings.neko_with_mood",
                            sanitize(name, MAX_NAME_LENGTH), mood));
                }
            } else if (e instanceof Monster m) {
                monsters.merge(m.getType().getDescriptionId(), 1, Integer::sum);
            } else if (e instanceof Animal a) {
                animals.merge(a.getType().getDescriptionId(), 1, Integer::sum);
            } else {
                if (others.size() < MAX_OTHERS) {
                    others.add(sanitize(translateOrReadable(e.getType().getDescriptionId()), MAX_NAME_LENGTH));
                }
            }
        }

        String listSep = translateOrReadable("misc.toneko.surroundings.list_separator");
        String sep = translateOrReadable("misc.toneko.surroundings.separator");
        String etc = translateOrReadable("misc.toneko.surroundings.etc");
        List<String> parts = new ArrayList<>();
        if (!players.isEmpty()) {
            parts.add(translateOrReadable("misc.toneko.surroundings.players", String.join(listSep, players)));
        }
        if (!nekos.isEmpty()) {
            parts.add(translateOrReadable("misc.toneko.surroundings.nekos", String.join(listSep, nekos)));
        }
        appendGroups(parts, monsters, "misc.toneko.surroundings.monsters", MAX_MONSTER_GROUPS, listSep, etc);
        appendGroups(parts, animals, "misc.toneko.surroundings.animals", MAX_ANIMAL_GROUPS, listSep, etc);
        if (!others.isEmpty()) {
            parts.add(translateOrReadable("misc.toneko.surroundings.others", String.join(listSep, others)));
        }
        return String.join(sep, parts);
    }

    /** 把类型分组计数（如 {僵尸:3, 骷髅:2}）拼成 "3 只僵尸；2 只骷髅"，超限追加 etc */
    private static void appendGroups(List<String> parts, Map<String, Integer> groups, String key,
                                     int maxGroups, String listSep, String etc) {
        if (groups.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> entry : groups.entrySet()) {
            if (i >= maxGroups) {
                sb.append(etc);
                break;
            }
            if (i > 0) sb.append(listSep);
            sb.append(String.format(translateOrReadable(key),
                    entry.getValue(), translateOrReadable(entry.getKey())));
            i++;
        }
        parts.add(sb.toString());
    }

    /** 周围环境特征采样：XZ 步进 + 3 个 Y 层（脚底-1/脚底/头顶），每特征命中即早退；仅地下时额外采样矿石 */
    private static String scanBlocks(NekoEntity neko) {
        Level level = neko.level();
        if (level == null) return "";
        BlockPos origin = neko.blockPosition();
        int yBase = origin.getY();
        boolean underground = !level.canSeeSkyFromBelowWater(origin);
        boolean water = false, lava = false, forest = false, crops = false, building = false;

        scan:
        for (int dx = -BLOCK_RANGE; dx <= BLOCK_RANGE; dx += BLOCK_STEP) {
            for (int dz = -BLOCK_RANGE; dz <= BLOCK_RANGE; dz += BLOCK_STEP) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockState state = level.getBlockState(new BlockPos(origin.getX() + dx, yBase + dy, origin.getZ() + dz));
                    if (!water && state.is(Blocks.WATER)) water = true;
                    if (!lava && state.is(Blocks.LAVA)) lava = true;
                    if (!forest && (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES))) forest = true;
                    if (!crops && state.is(BlockTags.CROPS)) crops = true;
                    if (!building && BUILDING_BLOCKS.contains(state.getBlock())) building = true;
                    if (water && lava && forest && crops && building) break scan;
                }
            }
        }

        boolean ore = false;
        if (underground) {
            oreScan:
            for (int dx = -ORE_RANGE; dx <= ORE_RANGE; dx += ORE_STEP) {
                for (int dz = -ORE_RANGE; dz <= ORE_RANGE; dz += ORE_STEP) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockState state = level.getBlockState(new BlockPos(origin.getX() + dx, yBase + dy, origin.getZ() + dz));
                        for (TagKey<Block> tag : ORE_TAGS) {
                            if (state.is(tag)) {
                                ore = true;
                                break oreScan;
                            }
                        }
                    }
                }
            }
        }

        List<String> features = new ArrayList<>();
        if (water) features.add(translateOrReadable("misc.toneko.surroundings.block.water"));
        if (lava) features.add(translateOrReadable("misc.toneko.surroundings.block.lava"));
        if (forest) features.add(translateOrReadable("misc.toneko.surroundings.block.forest"));
        if (crops) features.add(translateOrReadable("misc.toneko.surroundings.block.crops"));
        if (building) features.add(translateOrReadable("misc.toneko.surroundings.block.building"));
        if (underground) features.add(translateOrReadable("misc.toneko.surroundings.block.cave"));
        if (ore) features.add(translateOrReadable("misc.toneko.surroundings.block.ore"));
        return String.join(translateOrReadable("misc.toneko.surroundings.list_separator"), features);
    }
}
