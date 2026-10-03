# 内容注册与遗传学领域审计报告（items / blocks / genetics / recipes / effects / worldgen / advencements / codecs）

## 审计范围与方法

**实际通读的核心范围**（common 模块 8 个包，共 **85 个 Java 文件、8312 行**）：

| 包 | 文件数 | 行数 | 代表文件 |
|---|---|---|---|
| `mod/items`（含 ammo/） | 31 | 4109 | NekoEnergyBurstItem(841)、NekoMultiToolItem(387)、LegwearItem(373)、ShengDengItem(197)、BazookaItem(256) |
| `mod/blocks` | 9 | 1485 | ShengDengBlock(742)、NekoAggregatorBlock(272)、ClotheslineBlockEntity(142)、LegwearWorkbenchBlock(111) |
| `mod/genetics`（含 api/） | 12 | 1150 | Genome(202)、GeneticsDataLoader(264)、ToNekoAlleles(162)、GeneticsRegistry(137)、Allele(117) |
| `mod/recipes`（含 emi/） | 6 | 621 | NekoAggregatorRecipePattern(247)、NekoAggregatorInput(153)、NekoAggregatorRecipe(85) |
| `mod/effects` | 4 | 140 | Bewitched/HissIntimidation/Exciting + ToNekoEffects |
| `mod/worldgen` | 3 | 122 | NekoHutProcessor、NekoHutEntitySpawner、ToNekoStructures |
| `mod/advencements`（目录名即此拼写） | 18 | 631 | ToNekoCriteria + 17 个 Trigger |
| `mod/codecs` | 2 | 54 | Scent、CountCodecs |

**延伸交叉阅读**：`misc/`（ToNekoComponents 128 行、ScentUtil/WetnessUtil/CauldronSpoilageData/Charm/ZettaiRyouiki 等约 500 行）、平台 twin 注册类（fabric/neoforge 的 ToNekoItems、ToNekoBlocks，约 900 行）、`ToNekoNetworkEvents`（GenomeData C2S 处理）、`ModBootstrap`、两平台入口初始化序列、6 个 `toneko_genetics` 数据包 JSON、`NekoEntity` 繁殖段（1040–1145、1880–1930）。另用 javap 反编译了 1.21.1 merged jar 验证 `Level.addParticle` 与 `MobEffectInstance.tick` 的运行时语义。

方法：逐文件精读 + grep 交叉验证调用点 + 平台 twin 差异比对 + 字节码验证关键怀疑点。

## 总体评价

**6.5 / 10**

架构骨架明显优于一般 mod：双层注册模式执行严格、Codec/StreamCodec 成对出现率高、遗传系统的分层建模（Locus/Allele/Genome/Karyotype + 数据包热重载）是亮点。但存在一批"双平台 twin 手工同步"导致的真实分歧 bug（食物数值、方块属性、物品组不一致），两个遗留数据组件违反项目自己写进《开发规范》的 `.networkSynchronized` 铁律，基因表达存在非确定性重掷问题，以及若干静态可变状态的维度盲区。

## 优点

1. **双层注册模式执行到位**：common 声明可空字段 → 平台注册回填；neoforge 侧 `reg()` 统一放在 `FMLLoadCompleteEvent`（`neoforge/ToNekoNeoForge.java:121-123`），符合规范 §3.2；数据包驱动注册表（附魔 8 个、唱片 2 个）完全不碰平台注册代码。
2. **Codec 使用现代且规范**：配方 serializer 以 `MapCodec` + `StreamCodec` 成对出现（`NekoAggregatorRecipe.java:55-70`，完全符合 §6.4）；legwear 全部 8 个 DataComponent 同时配 `.persistent` + `.networkSynchronized`（`misc/ToNekoComponents.java:40-127`）；进度触发器用 record + `RecordCodecBuilder`（`advencements/NekoLevelTrigger.java:26-32`）。
3. **遗传系统分层建模质量高**：孟德尔分离定律与自由组合定律实现正确——`Genome.createGamete()` 每对染色体独立随机取单链（`api/Genome.java:24-36`），受精 `combine()` 按核型合并父源 strandA/母源 strandB（41-53）；显隐性权重语义清晰；野生池轮盘赌选择（83-93）正确处理了权重和为 0 之外的边界。
4. **数据包驱动的三阶段加载设计周到**：等位基因→基因座→核型补丁顺序保证依赖就绪（`GeneticsDataLoader.java:50-83`）；`clearDynamicData()` 区分"动态等位基因/动态基因座/硬编码池中的动态条目"三类清理（`GeneticsRegistry.java:70-94`），`/reload` 可安全热重载；fabric/neoforge 双端均注册了该 listener。
5. **C2S 安全校验链在 genetics 入口落实**：基因编辑包要求手持编辑器或管理员权限 + 实体距离 <64 + `server.execute()` 主线程切换（`events/ToNekoNetworkEvents.java:79-103`），是 §10.2 六步链的良好范例。
6. **ClotheslineBlockEntity 是教科书式 BE**：save/load key 对称（`ContainerHelper.saveAllItems/loadAllItems`）、覆写 `getUpdatePacket/getUpdateTag` 并配 `sendBlockUpdated`、`onRemove` 掉落内容、tick 按 20t 节流（`blocks/ClotheslineBlockEntity.java:60-105`）。
7. **纯函数计算器风格统一**：`ScentUtil`/`WetnessUtil`/`Charm`/`ZettaiRyouiki` 不持状态、从组件现算、clamp 归零清空穿着者等边界处理一致，Javadoc 说明用途与调用方（如 `misc/ScentUtil.java:37-49`）。
8. **无 BE 菜单模式与规范一致**：`LegwearWorkbenchBlock.LegwearWorkbenchMenu` 双构造器（vanilla 签名 + 完整签名）、`stillValid` 经 `ContainerLevelAccess` 校验方块存活、`removed()` 归还物品（`blocks/LegwearWorkbenchBlock.java:63-118`），与 `NekoAggregatorBlock` 形成正反对照。

## 问题

### [较高]

1. **同物品双平台属性分歧（手工 twin 同步事故）**
   - `CATNIP_SANDWICH`：fabric `FoodProperties(10, 12f, …)` vs neoforge `FoodProperties(6, 3.0f, …)`（`fabric/items/ToNekoItems.java:52` vs `neoforge/items/ToNekoItems.java:125`）——营养值与饱和度完全不同。
   - `BAZOOKA`：fabric `new Item.Properties()` vs neoforge `stacksTo(1).rarity(Rarity.RARE)`（fabric:66 vs neoforge:136）。
   - 建议：把"实例构造参数"收敛到 common（工厂方法/常量表），平台模块只负责 `Registry.register(id, CommonFactory::xxx)`，杜绝两处手写构造。

2. **NEKO_AGGREGATOR 方块属性不一致**
   - fabric：`Properties.of().strength(5.0f).requiresCorrectToolForDrops()`（`fabric/items/ToNekoBlocks.java:30-31`）；neoforge：`Block.Properties.of()` 裸默认（`neoforge/items/ToNekoBlocks.java:29`）——挖掘硬度、掉落工具判定两平台不同。同文件其余 5 个方块属性一致，唯独这个漏配。

3. **两个遗留数据组件缺 `.networkSynchronized`，违反项目自己的铁律**
   - `NEKO_PROGRESS_COMPONENT`（`misc/ToNekoComponents.java:17-21`）与 `ITEM_ID_COMPONENT`（23-27）只有 `.persistent(...)`。规范 §6.2 明文："只配 persistent 的组件在回包时被静默丢弃"。后果：猫娘收集器的进度值、火箭炮装填弹药类型在客户端栈上不可见/丢失（tooltip 恒显示旧值、客户端逻辑失真）。legwear 八组件全部成对配置，说明团队已知此坑但未迁移这两个历史组件。

4. **共显性等位基因的表达非确定性重掷**
   - `resolveDominance` 在两侧显性度相等时 `random.nextBoolean()` 二选一（`genetics/api/Genome.java:149-156`），而 `expressTraits()` 在每次读档时重新执行（`GhostNekoEntity.java:142` 注释确认 readAdditionalSaveData 末尾自动表达）。HEALTHY(20)/WEAK(20)、SLOW_SPEED(20)/SUPER_SPEED(20) 等组合的后代表型会在每次存档-载入间随机翻转；用 GeneEditor 改一个基因座也会连带重掷其它同显性度座位。建议：同显性度时确定性地偏向父源/按 id 字典序，或引入真正的共显性叠加规则。另：`NekoEntity.getBreedOffspring` 先随机生成全基因组并表达，随后 `breed()` 再覆盖为亲本基因组二次表达（`NekoEntity.java:1129-1132` vs `1069-1087`），属重复功+瞬时错误表型。
   - 附带观察：全代码库没有任何突变（mutation）实现——系统只覆盖分离/自由组合/显隐性，若规划中有突变机制则尚属空白。

5. **ShengDengBlock 拔凳状态机以裸 BlockPos 为 key 的全局静态 Map**
   - `PULLING_STATES`/`INTERNAL_REMOVAL` 为 `static Map/Set<BlockPos,…>`（`blocks/ShengDengBlock.java:58-59`）：①主世界与下界同一坐标的两根省凳柱共享同一个 PullingState，互相污染节奏/张力；②静态状态不随服务器关闭清理，单人模式切档后残留条目会命中新世界同坐标方块。建议改 `GlobalPos` 或 `(ResourceKey<Level>, BlockPos)` 复合键，并挂 `ServerStoppedEvent` 清空。

### [中]

6. **服务端 `Level.addParticle` 无效调用（字节码已验证为空方法）**：1.21.1 中仅 `ClientLevel` 覆写 `addParticle`。`NekoEnergyBurstItem.use()` 内的治疗爱心（196-199）与受伤粒子（252-255）、`CatnipItem.hitOnAir` 的粒子均在服务端分支内调用，**实际从不显示**（对比同文件 `spawnHissSpray` 正确使用 `sendParticles`）。建议统一替换为 `ServerLevel.sendParticles`。
7. **ShengDengBlock 大类职责混杂（742 行）**：方块行为 + 拔凳小游戏引擎（张力/节奏/摇晃判定）+ 柱子数据结构工具 + 进度 HUD 全在一个类里，且大量 static 方法与实例方法混排。建议拆出 `StoolColumnUtil`（柱子操作）与 `StoolPullingEngine`（小游戏状态机）。同类问题：`NekoEnergyBurstItem`（841 行 = 物品 + 连击引擎 + BossBar HUD 管理器 + 五层粒子特效 + 全服播报系统）。
8. **硬编码字面量绕过 lang 系统**：拔凳进度条 `"急了！"/"稳！"`（ShengDengBlock.java:563-581）、BossBar 标题 `"超越耄耋"` 等（NekoEnergyBurstItem.java:703-728）、`PlotScrollItem` tooltip `"暂时没用，不要管它"`（PlotScrollItem.java:33）、FlySword 英文硬编码升级面板（FlySwordItem.java:31-38）。直接违反 §12/§14，5 语言翻译体系对这些文本无效。
9. **混色省凳柱破坏时丢失颜色**：颜色仅存于方块状态 `COLOR` 属性（红/蓝可逐格染色），而破坏整柱统一掉 `ShengDengItem.createStackedItem(count)` 无任何颜色信息，再放置时固定回到 RED（ShengDengItem.useOn:115）——玩家的染色成果静默蒸发。建议给 stacked item 增加颜色组件或按色分组掉落。
10. **创造物品组跨平台不一致（规范 §18.11 明令禁止的场景再次发生）**：`FURRY_BOHE` 注册了但 fabric 物品组从未 accept（fabric/items/ToNekoItems.java 全文无 FURRY_BOHE accept），neoforge 两处都有（229、309）→ fabric 创造栏拿不到；`NOELLE_MAID_NEKO_SPAWN_EGG` fabric 注释掉（~197）而 neoforge displayItems 包含（~239）。
11. **NekoHutEntitySpawner 是带缺陷的死代码**：`checkChunk` 全仓库零调用者；若未来接线，其 `static Set<ChunkPos> processedChunks`（worldgen/NekoHutEntitySpawner.java:16）无维度键（跨维度误判）且永不清理（内存无界增长）、不持久化（重启后对同一小屋重复生成猫娘）。建议删除或改为结构放置时一次性生成（`NekoHutProcessor.spawnNeko` 已承担该职责）。
12. ** GeneticsRegistry 并发策略混搭**：`KARYOTYPE_CACHE` 用 ConcurrentHashMap 而 ALLELES/LOCI/WILD_POOLS/KARYOTYPES 全是 HashMap（api/GeneticsRegistry.java:11-24）——要么全线程不安全要么全并发，注释暗示缓存会被多线程访问，那么其余 Map 同样暴露在竞态下。另外 `Genome.load` 吞掉 `NumberFormatException`（Genome.java:192），且 `ResourceLocation.parse` 对非法字符抛未检查异常，恶意 C2S 包（GenomeDataPayload 直接 load）可刷日志（有权限门槛，故降为中危）。
13. **NekoAggregatorMenu 细节缺陷**：`stillValid` 恒 true（NekoAggregatorBlock.java:242-245），走远仍可合成（对比 LegwearWorkbench 的正确写法）；`InputSlot.setChanged` → `updateResult` 在 `consumeInputs` 缩减 9 个槽位时连锁触发 9 次配方查找+同步包；`useWithoutItem` 服务端返回 PASS 应为 SUCCESS/CONSUME。
14. **全服广播用施法者语言预解析**：`broadcastRaw` 把 `Component.translatable(...).getString()` 后以 literal 发给所有玩家（NekoEnergyBurstItem.java:650-655、NineLivesCharmItem.broadcastUse:168-173），其他语言玩家看到的是使用者的语言。应直接发送 translatable 组件由各客户端本地化。

### [低]

15. **`api/ChromosomePair.java` 整类冗余**：与 `Genome.ChromosomePair` 嵌套类字段完全相同（strandA/strandB），前者全仓库无引用（grep 仅定义处）。二选一删除。
16. **CountCodecs 命名混乱**：record 名叫 `FloatCountCodec`（它是数据不是 codec），外层类 `CountCodecs` 只有一个成员；record 手写 `getCount()/getMaxCount()` 与自动生成的 `count()/maxCount()` 并存（codecs/CountCodecs.java:12-20）。
17. **ID 常量不规范**：`NekoCollectorItem.ID` 非 final（items/NekoCollectorItem.java:22）；fabric/items/ToNekoBlocks.java 全部裸写 `ResourceLocation.fromNamespaceAndPath(MODID, …)`，违反 §5 "一律经 toNekoLoc"。
18. **advencements 目录拼写**（应为 advancements，规范已承认）+ 16 个 trigger 类高度模板化重复（每个 34 行，仅字段类型不同），可用泛型基类或代码生成收敛；当前写法胜在与原版风格一致。
19. **recipes 包整段复制原版**：`NekoAggregatorRecipePattern` ≈ 原版 `ShapedRecipePattern`、`NekoAggregatorInput` ≈ `CraftingInput`（含反编译痕迹式代码），合计 ~400 行复制代码，MC 升级时需人工跟移。
20. **EMI 配方 `getResultItem(null)`**（recipes/emi/NekoAggregatorEmiRecipe.java:39）：当前实现忽略 registries 参数不会崩，但契约上不应传 null。
21. **neoforge 冗余回填与双注册**：`buildContents` 末尾再调一次 `reg()`（items/ToNekoItems.java:368-373），而 `FMLLoadCompleteEvent` 已调（ToNeoNeoForge.java:121-123）；物品组同时写 `displayItems` + `BuildCreativeModeTabContentsEvent` ——经查 NeoForge `EventHooks.onCreativeModeTabBuildContents` 用去重 Set 收集两路输出，**无重复显示**，但双写纯属冗余劳动（规范 §6.1 第 5 条可据此修订）。
22. **fireDried 跨维度遍历**：遍历 `getServer().getPlayerList().getPlayers()` 后仅按坐标距离筛选（ClotheslineBlockEntity.java:118-123），其他维度同坐标玩家会误触发成就；应先过滤 `p.level() == level` 或改用 `serverLevel.players()`。
23. **ExplosiveBombItem 名不副实**：`ExplosionInteraction.NONE` 即不破坏方块（ammo/ExplosiveBombItem.java:16），与名字和图标预期不符（若为平衡性决定请加注释）。
24. **杂项**：连击窗口用 `System.currentTimeMillis()` 墙钟（受改表影响，宜用 tick 计数）；`COMBO_MAP` 未命中超时不主动清理（UUID 级小泄漏）；`ShengDengItem.setStackCount` 整体覆写 CUSTOM_DATA 且 inventoryTick 每 tick 写组件；`NekoMultiToolItem.inventoryTick` 每 20t 无条件 set 组件造成脏标记；`GeneticsDataLoader.dominance` 无范围校验（可为负）；`ExcitingEffect` 注册到字段名 `NEKO_EFFECT` 名实不符；`insertStools` 不检查世界高度上限与实体占位（绕过 BlockItem.place 的 canPlace 管线），贴着建筑上限塞凳会吞物品。

## 数据速览

- 核心审计范围：**85 个 Java 文件 / 8312 行**（items 4109 · blocks 1485 · genetics 1150 · recipes 621 · effects 140 · worldgen 122 · advencements 631 · codecs 54）；另有 misc 相关 ~500 行、平台 twin 注册类 ~900 行、网络/事件/实体相关片段
- 注册规模：物品 fabric `Registry.register` ×58 / neoforge `ITEMS.register` ×58；方块 ×8、BlockEntityType ×1、MenuType ×2、RecipeType/Serializer 各 ×1、MobEffect ×3、CriterionTrigger ×17、StructureProcessorType ×1
- 遗传系统：13 个硬编码 Locus + 3 个萌属性槽位、17 个硬编码 Allele、核型 2 套（base_mob 5 对染色体 / neko 派生 8 对）、野生池条目 ~40 条、数据包遗传 JSON ×6（alleles 2 / loci 2 / karyotype 补丁 2）
- 最大单体类：NekoEnergyBurstItem 841 行 > ShengDengBlock 742 行 > NekoMultiToolItem 387 行
- Codec 成对率：配方 serializer 1/1 成对 ✓；DataComponent 10 个中 8 个 persistent+network 成对 ✓、**2 个缺 networkSynchronized ✗**
- 问题计数：**24 项 = 较高 5 + 中 9 + 低 10**（其中 [严重] 级 0 项）；平台一致性分歧 4 项、违反项目自身书面规范 3 项、死代码 2 处（api/ChromosomePair、NekoHutEntitySpawner）
- 编译/行为验证：javap 反编译 1.21.1 merged jar 确认 `Level.addParticle` 为空实现（支撑问题 #6）
