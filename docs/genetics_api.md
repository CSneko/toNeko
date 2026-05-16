# toNeko 遗传系统 API - 第三方模组集成文档

## 概述

toNeko 的遗传系统是一个通用的、可扩展的基因引擎。它允许任意 `LivingEntity` 拥有可遗传的基因，并通过显隐性规则表达为实体的属性、行为和自定义效果。

**核心概念：**
- **基因座 (Locus)** — 染色体上的一个"位置"，相当于一个基因坑位
- **等位基因 (Allele)** — 占据某个基因座的具体基因变体，携带具体的表达能力
- **核型 (Karyotype)** — 定义某个物种（实体类）有多少条染色体，以及上面有哪些基因座
- **基因组 (Genome)** — 一个个体的完整基因信息，包含所有染色体对的等位基因分布
- **基因表达 (Expression)** — 从基因组中根据显隐性计算，将表达的基因效果应用到实体上

---

## 快速开始

### 依赖

在您的 `build.gradle` 中添加 toNeko 作为依赖：

```gradle
repositories {
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
    }
}

dependencies {
    // 将 version 替换为实际版本
    implementation "maven.modrinth:toneko:${project.toneko_version}"
}
```

### 1. 实现遗传接口

让您的实体类实现 `IGeneticEntity` 接口：

```java
import org.cneko.toneko.common.mod.genetics.api.*;

public class YourEntity extends PathfinderMob implements IGeneticEntity {
    private Genome genome = new Genome();
    private final CompoundTag geneticData = new CompoundTag();
    private final List<ExpressedTrait> activeTraits = new ArrayList<>();
    private final List<Goal> activeGeneticGoals = new ArrayList<>();

    // --- IGeneticEntity 实现 ---

    @Override
    public Genome getGenome() { return this.genome; }

    @Override
    public void setGenome(Genome genome) { this.genome = genome; }

    @Override
    public CompoundTag getGeneticData() { return this.geneticData; }

    @Override
    public List<ExpressedTrait> getActiveTraits() { return this.activeTraits; }

    @Override
    public List<Goal> getActiveGeneticGoals() { return this.activeGeneticGoals; }

    @Override
    public void expressTraits() {
        if (!this.level().isClientSide) {
            this.genome.express(this);
        }
    }

    // --- 持久化 (NBT) ---

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("Genome", this.genome.save());
        compound.put("GeneticData", this.geneticData);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Genome")) {
            this.genome.load(compound.getCompound("Genome"));
        }
        if (compound.contains("GeneticData")) {
            this.geneticData.merge(compound.getCompound("GeneticData"));
        }
        this.expressTraits(); // 加载后必须重新表达基因
    }

    // --- 繁殖 ---

    @Override
    public void spawnChildFromBreeding(ServerLevel level, YourEntity mate) {
        Gamete paternal = this.genome.createGamete(this.random);
        Gamete maternal = mate.genome.createGamete(mate.random);
        Genome childGenome = Genome.combine(paternal, maternal, YourKaryotype.INSTANCE);

        YourEntity child = new YourEntity(level);
        child.setGenome(childGenome);
        child.expressTraits();
        // ... 设置位置、年龄等
        level.addFreshEntity(child);
    }

    // --- 自然生成 ---

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        // 生成两个随机配子，模拟野生基因库
        Gamete g1 = Genome.generateFallbackGamete(this.random, YourKaryotype.INSTANCE);
        Gamete g2 = Genome.generateFallbackGamete(this.random, YourKaryotype.INSTANCE);
        this.genome = Genome.combine(g1, g2, YourKaryotype.INSTANCE);
        this.expressTraits();
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }
}
```

### 2. 定义基因座和等位基因

在您的 Mod 初始化类中注册遗传组件：

```java
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class YourMod {
    // --- 基因座 ---
    public static final Locus YOUR_LOCUS = new Locus(toNekoLoc("your_locus"));
    public static final Locus ANOTHER_LOCUS = new Locus(toNekoLoc("another_locus"));

    // --- 等位基因 ---

    // 属性修改型：使用 AttributeModifier
    public static final Allele STRONG_ALLELE = new Allele(toNekoLoc("strong"), 20, null, null)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, "boost", 5.0,
                    AttributeModifier.Operation.ADD_VALUE);

    // 自定义回调型：通过 onExpress/onRemove 修改 NBT 或执行任意逻辑
    public static final Allele SPECIAL_ALLELE = new Allele(toNekoLoc("special"), 15,
            (entity, data) -> data.putBoolean("is_special", true),
            (entity, data) -> data.remove("is_special")
    );

    // AI 注入型：为实体添加行为
    public static final Allele AGGRESSIVE_ALLELE = new Allele(toNekoLoc("aggressive"), 10, null, null)
            .addAIGoal(5, mob -> new MeleeAttackGoal((PathfinderMob) mob, 1.2, true));

    // 混合型：同时具备多种效果
    public static final Allele HYBRID_ALLELE = new Allele(toNekoLoc("hybrid"), 20, null, null)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "speed", 0.1,
                    AttributeModifier.Operation.ADD_VALUE)
            .addAIGoal(4, mob -> new FloatGoal(mob));

    // --- 核型 (Karyotype) ---

    // 独立核型：您的实体独有
    public static final SpeciesKaryotype YOUR_KARYOTYPE = new SpeciesKaryotype(2)
            .bindLocus(1, YOUR_LOCUS)
            .bindLocus(2, ANOTHER_LOCUS);

    // 或继承 toNeko 的基础核型 (实体将拥有原版生物学中速度/攻击/体型等基因)
    public static final SpeciesKaryotype DERIVED_KARYOTYPE =
            new SpeciesKaryotype(ToNekoLocus.BASE_MOB_KARYOTYPE, 1)
                    .bindLocus(6, YOUR_LOCUS);

    public static void init() {
        // 1. 注册基因座
        GeneticsRegistry.registerLocus(YOUR_LOCUS);
        GeneticsRegistry.registerLocus(ANOTHER_LOCUS);

        // 2. 注册等位基因
        GeneticsRegistry.registerAllele(STRONG_ALLELE);
        GeneticsRegistry.registerAllele(SPECIAL_ALLELE);
        GeneticsRegistry.registerAllele(AGGRESSIVE_ALLELE);

        // 3. 注册核型到实体类
        GeneticsRegistry.registerKaryotype(YourEntity.class, YOUR_KARYOTYPE);

        // 4. 添加到野生基因池（权重越高越常见）
        GeneticsRegistry.addWildAllele(YOUR_LOCUS.id(),
                ToNekoAlleles.WILD_TYPE.getId(), 60); // 60% 野生型
        GeneticsRegistry.addWildAllele(YOUR_LOCUS.id(),
                STRONG_ALLELE.getId(), 25);           // 25% 强力
        GeneticsRegistry.addWildAllele(YOUR_LOCUS.id(),
                SPECIAL_ALLELE.getId(), 15);           // 15% 特殊
    }
}
```

---

## API 参考

### `IGeneticEntity` — 实体遗传接口

您的实体类必须实现此接口才能参与遗传系统。

```java
public interface IGeneticEntity {
    Genome getGenome();
    void setGenome(Genome genome);
    CompoundTag getGeneticData();
    List<ExpressedTrait> getActiveTraits();
    List<Goal> getActiveGeneticGoals();
    void expressTraits();
}
```

| 方法 | 用途 |
|---|---|
| `getGenome()` / `setGenome()` | 存取实体的基因组 |
| `getGeneticData()` | 返回一个 `CompoundTag`，供等位基因的 `onExpress`/`onRemove` 回调读写自定义数据 |
| `getActiveTraits()` | 返回当前表达的基因列表 `(Locus, Allele)`，引擎自动维护 |
| `getActiveGeneticGoals()` | 返回基因注入的 AI Goal 列表，引擎自动维护 |
| `expressTraits()` | 调用 `genome.express(this)` 触发表达，应该在 NBT 加载、生成、繁殖后调用 |

---

### `Locus` — 基因座

```java
public record Locus(ResourceLocation id) {}
```

| 方法 | 说明 |
|---|---|
| `Locus(ResourceLocation id)` | 创建一个基因座。ID 需全局唯一，建议使用 `toNekoLoc("name")` |

---

### `Allele` — 等位基因

```java
public Allele(ResourceLocation id, int dominance,
              BiConsumer<LivingEntity, CompoundTag> onExpress,
              BiConsumer<LivingEntity, CompoundTag> onRemove)
```

| 参数 | 说明 |
|---|---|
| `id` | 等位基因的唯一 ID |
| `dominance` | 显性值。**越高越显性**。相等时随机共显性。|
| `onExpress` | 表达时回调。可 null。 |
| `onRemove` | 移除时回调。可 null。 |

**Fluent 方法（返回 `this`，可链式调用）：**

```java
// 添加属性修饰符
Allele addAttributeModifier(Holder<Attribute> attr, String suffix,
                            double amount, AttributeModifier.Operation op);

// 添加 AI 行为
Allele addAIGoal(int priority, Function<Mob, Goal> goalFactory);
```

**显性值参考：**
| 值 | 含义 | 示例 |
|---|---|---|
| 5 | 隐性 | `LARGE_BODY`, `LARGE_CHEST` |
| 10 | 野生型（普通） | `WILD_TYPE` |
| 15 | 半显性/显性 | `FLAT_CHEST`, `GRAVITY_SENSITIVE` |
| 20 | 显性 | `SMALL_BODY`, `SLOW_SPEED` |

---

### `SpeciesKaryotype` — 物种核型

**构造：**

```java
// 从头创建
SpeciesKaryotype(int chromosomePairs)

// 从父核型继承 (+ 额外的染色体对数)
SpeciesKaryotype(SpeciesKaryotype parent, int extraChromosomePairs)
```

**方法：**

```java
SpeciesKaryotype bindLocus(int chromosomeId, Locus locus)  // 绑定基因座到染色体
int getChromosomePairs()                                     // 染色体对数
List<Locus> getLociOnChromosome(int chromosomeId)            // 某条染色体上的所有基因座
```

> **注意：** 如果您的实体继承 toNeko 的基础核型，它将自动获得原版生物的速度、攻击、体型、生命、抗性等基因。这意味着原版的僵尸、猪等实体默认就有基因槽位。

---

### `Genome` — 基因组

```java
Gamete createGamete(RandomSource random)                                         // 减数分裂 → 配子
static Genome combine(Gamete paternal, Gamete maternal, SpeciesKaryotype kt)    // 受精 → 新基因组
static Genome generateWildGenome(RandomSource random, SpeciesKaryotype kt)       // 生成随机野生基因组
static Gamete generateFallbackGamete(RandomSource random, SpeciesKaryotype kt)  // 生成随机野生配子
void express(LivingEntity entity)                // 表达全部基因（引擎调用，一般不需直接调用）
CompoundTag save()                                // 序列化到 NBT
void load(CompoundTag tag)                        // 从 NBT 加载
```

---

### `GeneticsRegistry` — 全局注册表

**静态方法：**

```java
registerAllele(Allele allele)
registerLocus(Locus locus)
registerKaryotype(Class<? extends LivingEntity> entityClass, SpeciesKaryotype karyotype)
addWildAllele(ResourceLocation locusId, ResourceLocation alleleId, int weight)

getAllele(ResourceLocation id)
getLocus(ResourceLocation id)
getKaryotype(LivingEntity entity)
getKaryotype(Class<? extends LivingEntity> entityClass)
```

**静态映射（直接访问）：**

```java
GeneticsRegistry.ALLELES      // Map<ResourceLocation, Allele>
GeneticsRegistry.LOCI         // Map<ResourceLocation, Locus>
GeneticsRegistry.WILD_POOLS   // Map<ResourceLocation, List<WeightedAllele>>
```

> **提示：** `registerKaryotype` 的实体类可以设为 `Mob.class`，所有继承自 `Mob` 的生物都将自动获得该核型。系统会自动向上查找继承链，所以子类可以使用父类的核型。

---

### `MoeGenetics` — 萌属性基因系统（参考实现）

toNeko 的萌属性系统是通过遗传 API 构建的参考实现。它为 `NekoEntity` 提供三个萌属性基因座（`MOE_SLOT_0` ~ `MOE_SLOT_2`），动态为每个萌属性标签（傲娇、病娇等）生成等位基因。

```java
public static final Locus MOE_SLOT_0;  // 总是有一个萌属性
public static final Locus MOE_SLOT_1;  // 65% 空，35% 有一个萌属性
public static final Locus MOE_SLOT_2;  // 稀有萌属性槽
```

---

## 调用 toNeko 现有基因

您的模组可以直接使用 toNeko 已注册的基因座和等位基因：

```java
// 获取 toNeko 已有的基因
Allele wildType = GeneticsRegistry.getAllele(ResourceLocation.parse("toneko:wild_type"));
Locus speedSlot = GeneticsRegistry.getLocus(ResourceLocation.parse("toneko:speed_slot_0"));

// 为 toNeko 的基因座添加您的等位基因
GeneticsRegistry.addWildAllele(
    ResourceLocation.parse("toneko:speed_slot_0"),
    ResourceLocation.parse("yourmod:your_allele"),
    5  // 5% 权重
);

// 检查实体的核型
SpeciesKaryotype karyotype = GeneticsRegistry.getKaryotype(someEntity);
```

---

## 在 GeneticsScreen 中显示您的基因

toNeko 的基因查看器 (`GeneticsScreen`) 会自动显示所有已注册的基因和基因座，无需额外操作。如需本地化名称，在您的语言文件中添加翻译键：

```json
{
  "locus.yourmod.your_locus": "Your Locus",
  "allele.yourmod.strong": "Strong",
  "allele.yourmod.special": "Special"
}
```

翻译键格式：
- 基因座：`locus.<命名空间>.<路径>` → 例如 `locus.yourmod.your_locus`
- 等位基因：`allele.<命名空间>.<路径>` → 例如 `allele.yourmod.strong`

---

## 最佳实践

1. **初始化时机：** 在您的 Mod 的初始化方法（Fabric 的 `onInitialize` / Forge 的 `FMLCommonSetupEvent`）中注册所有基因和核型

2. **基因座 ID 唯一性：** 使用 `toNekoLoc()` 或您自己的 `ResourceLocation`，确保命名空间前缀唯一

3. **显性值设计：**
   - 稀有/隐性基因使用低值（5 或更低）
   - 通用/野生型使用中等值（10）
   - 常见/显性基因使用高值（15-20）
   
4. **等位基因权重：** `addWildAllele` 的权重是相对值。所有权重之和为分母，单个权重为分子。70 表示 70/(70+20+10)=70% 概率

5. **NBT 数据：** 自定义回调中使用 `getGeneticData()` 的 `CompoundTag` 存储额外数据，引擎会自动序列化和反序列化。如需同步到客户端，请使用 `SynchedEntityData`

6. **表达安全：** `expressTraits()` 只在服务端调用（`!level.isClientSide`）。客户端渲染需要的数据应通过 `SynchedEntityData` 同步（参见 `NekoEntity.CHEST_SCALE_ID` 的实现）

7. **繁殖兼容性：** 如果您的实体与 toNeko 的猫娘繁殖，一方无遗传接口时会自动生成降级配子，不会崩溃

8. **AI Goal 清理：** 使用 `addAIGoal()` 注入的 Goal 会被引擎自动追踪和清理，无需手动管理

---

## 完整示例

一个完整的第三方模组集成示例，包含实体、基因、繁殖和本地化：

### `YourEntity.java`

```java
package com.example.mod.entity;

import com.example.mod.genetics.ExampleGenetics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.cneko.toneko.common.mod.genetics.api.*;
import java.util.ArrayList;
import java.util.List;

public class ExampleEntity extends AgeableMob implements IGeneticEntity {
    private Genome genome = new Genome();
    private final CompoundTag geneticData = new CompoundTag();
    private final List<ExpressedTrait> activeTraits = new ArrayList<>();
    private final List<Goal> activeGeneticGoals = new ArrayList<>();

    public ExampleEntity(EntityType<? extends ExampleEntity> type, Level level) {
        super(type, level);
    }

    // --- IGeneticEntity ---

    @Override
    public Genome getGenome() { return genome; }

    @Override
    public void setGenome(Genome genome) { this.genome = genome; }

    @Override
    public CompoundTag getGeneticData() { return geneticData; }

    @Override
    public List<ExpressedTrait> getActiveTraits() { return activeTraits; }

    @Override
    public List<Goal> getActiveGeneticGoals() { return activeGeneticGoals; }

    @Override
    public void expressTraits() {
        if (!level().isClientSide) {
            genome.express(this);
        }
    }

    // --- NBT ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Genome", genome.save());
        tag.put("GeneticData", geneticData);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Genome")) genome.load(tag.getCompound("Genome"));
        if (tag.contains("GeneticData")) geneticData.merge(tag.getCompound("GeneticData"));
        expressTraits();
    }

    // --- 繁殖 ---

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        ExampleEntity child = new ExampleEntity(ExampleEntities.EXAMPLE.get(), level);
        Gamete paternal = genome.createGamete(random);
        Gamete maternal = ((IGeneticEntity) other).getGenome().createGamete(other.getRandom());
        child.setGenome(Genome.combine(paternal, maternal, ExampleGenetics.KARYOTYPE));
        child.expressTraits();
        return child;
    }

    // --- 生成 ---

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff,
                                         MobSpawnType reason, SpawnGroupData data) {
        Gamete g1 = Genome.generateFallbackGamete(random, ExampleGenetics.KARYOTYPE);
        Gamete g2 = Genome.generateFallbackGamete(random, ExampleGenetics.KARYOTYPE);
        genome = Genome.combine(g1, g2, ExampleGenetics.KARYOTYPE);
        expressTraits();
        return super.finalizeSpawn(level, diff, reason, data);
    }
}
```

### `ExampleGenetics.java`

```java
package com.example.mod.genetics;

import com.example.mod.entity.ExampleEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.cneko.toneko.common.mod.genetics.ToNekoAlleles;
import org.cneko.toneko.common.mod.genetics.api.*;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ExampleGenetics {
    // 基因座
    public static final Locus SIZE_LOCUS = new Locus(toNekoLoc("size_locus"));
    public static final Locus COLOR_LOCUS = new Locus(toNekoLoc("color_locus"));

    // 等位基因
    public static final Allele TINY = new Allele(toNekoLoc("tiny"), 20, null, null)
            .addAttributeModifier(Attributes.SCALE, "size", -0.5,
                    AttributeModifier.Operation.ADD_VALUE);
    public static final AlleLE GIANT = new Allele(toNekoLoc("giant"), 5, null, null)
            .addAttributeModifier(Attributes.SCALE, "size", 0.5,
                    AttributeModifier.Operation.ADD_VALUE);
    public static final AlleLE RED = new Allele(toNekoLoc("red"), 15,
            (e, d) -> d.putString("color", "red"),
            (e, d) -> d.remove("color"));

    // 核型
    public static final SpeciesKaryotype KARYOTYPE = new SpeciesKaryotype(2)
            .bindLocus(1, SIZE_LOCUS)
            .bindLocus(2, COLOR_LOCUS);

    public static void init() {
        GeneticsRegistry.registerLocus(SIZE_LOCUS);
        GeneticsRegistry.registerLocus(COLOR_LOCUS);
        GeneticsRegistry.registerAllele(TINY);
        GeneticsRegistry.registerAllele(GIANT);
        GeneticsRegistry.registerAllele(RED);
        GeneticsRegistry.registerKaryotype(ExampleEntity.class, KARYOTYPE);

        GeneticsRegistry.addWildAllele(SIZE_LOCUS.id(), ToNekoAlleles.WILD_TYPE.getId(), 60);
        GeneticsRegistry.addWildAllele(SIZE_LOCUS.id(), TINY.getId(), 25);
        GeneticsRegistry.addWildAllele(SIZE_LOCUS.id(), GIANT.getId(), 15);
        GeneticsRegistry.addWildAllele(COLOR_LOCUS.id(), ToNekoAlleles.WILD_TYPE.getId(), 70);
        GeneticsRegistry.addWildAllele(COLOR_LOCUS.id(), RED.getId(), 30);
    }
}
```

---

## 常见问题

**Q: 为什么我的基因没有在 GeneticsScreen 中显示？**
A: 确保已调用 `GeneticsRegistry.registerLocus()` 和 `GeneticsRegistry.registerAllele()`，且实体类通过 `GeneticsRegistry.registerKaryotype()` 绑定了核型。

**Q: 客户端看不到基因表达的效果？**
A: `expressTraits()` 只会在服务端执行。如果需要客户端感知效果（如模型缩放），请使用 `SynchedEntityData` 同步相关值。

**Q: 旧存档实体会出问题吗？**
A: 不会。遗传系统在读取 NBT 时，不存在的染色体/基因座会优雅降级为该基因组空缺，`express()` 会跳过空缺槽位。不存在的数据会被赋予默认行为。

**Q: 可以在原版生物上使用吗？**
A: 可以！`GeneticsRegistry.registerKaryotype(Zombie.class, yourKaryotype)` 会让僵尸也拥有您的基因。toNeko 本身已经为 `Mob.class` 注册了基础核型。

---

## 数据驱动遗传学 (Data-Driven Genetics)

从 toNeko 1.8.3 开始，遗传系统支持通过**数据包 (Data Pack)** 加载遗传学数据，无需编写 Java 代码即可添加新的等位基因、基因座和核型修改。

### 目录结构

将 JSON 文件放在数据包的以下目录中：

```
data/<命名空间>/toneko_genetics/
├── alleles/<id>.json          # 定义基于属性的等位基因
├── loci/<id>.json             # 定义新的基因座（遗传学坑位）
└── karyotypes/<file>.json     # 修改现有核型（添加基因座/染色体）
```

文件路径中的 `<id>` 即为该资源的 ID。例如 `data/mymod/toneko_genetics/alleles/super_jump.json` 将注册 ID 为 `mymod:super_jump` 的等位基因。

### JSON 格式

#### 等位基因 (Allele)

```json
{
  "dominance": 20,
  "attributes": [
    {
      "attribute": "minecraft:generic.jump_strength",
      "suffix": "jump_boost",
      "operation": "add_multiplied_base",
      "amount": 0.5
    }
  ],
  "wild_pool": [
    { "locus": "toneko:speed_slot_0", "weight": 10 }
  ]
}
```

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `dominance` | int | 是 | 显性值，越高越显性。范围参考：5=隐性，10=野生型，15=半显性，20=显性 |
| `attributes` | array | 否 | 属性修饰符列表。每个条目包含 `attribute`、`suffix`、`operation`、`amount` |
| `wild_pool` | array | 否 | 野生基因池条目列表。每个条目包含 `locus`（目标基因座ID）和 `weight`（权重） |

**操作类型 (`operation`)：**

| 值 | 对应枚举 | 说明 |
|------|---------|------|
| `add_value` | `ADD_VALUE` | 固定值叠加（如 +0.05 移速） |
| `add_multiplied_base` | `ADD_MULTIPLIED_BASE` | 基于基础值百分比叠加 |
| `add_multiplied_total` | `ADD_MULTIPLIED_TOTAL` | 基于总值百分比叠加 |

#### 基因座 (Locus)

```json
{
  "wild_pool": [
    { "allele": "toneko:wild_type", "weight": 70 },
    { "allele": "toneko:super_speed", "weight": 30 }
  ]
}
```

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `wild_pool` | array | 否 | 野生基因池条目列表。每个条目包含 `allele`（等位基因ID）和 `weight`（权重） |

基因座本身仅定义一个"坑位"，需要通过核型补丁将其绑定到具体核型的染色体上才能生效。

#### 核型补丁 (Karyotype Patch)

```json
{
  "target": "toneko:neko",
  "add_loci": {
    "9": ["mymod:custom_locus", "mymod:another_locus"]
  }
}
```

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `target` | string | 是 | 目标核型的 ID（如 `toneko:neko`、`toneko:base_mob`） |
| `add_loci` | object | 是 | 染色体编号 → 基因座ID数组 的映射 |

> **注意：** `target` 引用的是核型 ID 而非实体类。内建核型 ID 为 `toneko:base_mob`（所有生物）和 `toneko:neko`（猫娘）。如果指定的染色体编号超出当前核型范围，系统会自动扩展核型。

### 工作示例

以下是一组完整的示例，在基础生物核型上添加一个新的步高基因：

**`data/toneko/toneko_genetics/alleles/tall_legs.json`**：
```json
{
  "dominance": 20,
  "attributes": [
    {
      "attribute": "minecraft:generic.step_height",
      "suffix": "step_boost",
      "operation": "add_value",
      "amount": 0.5
    }
  ],
  "wild_pool": [
    { "locus": "toneko:step_slot", "weight": 5 }
  ]
}
```

**`data/toneko/toneko_genetics/loci/step_slot.json`**：
```json
{
  "wild_pool": [
    { "allele": "toneko:wild_type", "weight": 95 },
    { "allele": "toneko:tall_legs", "weight": 5 }
  ]
}
```

**`data/toneko/toneko_genetics/karyotypes/add_step_slot.json`**：
```json
{
  "target": "toneko:base_mob",
  "add_loci": {
    "6": ["toneko:step_slot"]
  }
}
```

### 生命周期与热重载

1. **Mod 初始化**：硬编码的基因（`ToNekoLocus`、`ToNekoAlleles`）通过 Java 代码注册
2. **服务器启动 / `/reload`**：`GeneticsDataLoader` 自动从所有数据包中读取 `toneko_genetics/` 目录下的 JSON 文件，按以下顺序加载：
   - Phase 1：加载所有 `alleles/*` 等位基因
   - Phase 2：加载所有 `loci/*` 基因座
   - Phase 3：应用所有 `karyotypes/*` 核型补丁
3. **热重载安全**：执行 `/reload` 时，之前数据包添加的动态条目会被清理并重新加载，硬编码条目不受影响

### 翻译键

数据驱动的基因也会自动显示在 `GeneticsScreen` 中。为其添加本地化名称：

```json
{
  "allele.mymod.tall_legs": "长腿基因",
  "locus.mymod.step_slot": "步高基因座"
}
```

### 与硬编码遗传学的关系

- 数据包注册的内容与 Java 代码注册的内容**完全共存**
- 数据包可以向已有的硬编码基因座（如 `toneko:speed_slot_0`）添加新的等位基因
- 数据包可以向已有的硬编码核型（如 `toneko:neko`）添加新的染色体和基因座
- 数据包**不能**移除或修改硬编码的等位基因、基因座或核型（这是设计上的安全限制）