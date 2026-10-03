# 平台适配层审计报告（fabric / neoforge / bukkit）

> 审计人：ox-alpha（平台适配层领域）
> 对照规范：《开发规范.md》§2.3（datagen 同步）、§3.1–§3.4、§4

## 审计范围与方法

- **fabric**：`fabric/src/**` 全部 25 个 Java 文件，共 **1989 行**。重点精读：入口 `ToNeko.java`(108)、`items/ToNekoItems.java`(261)、`entities/ToNekoEntities.java`(111)、客户端 `ToNekoClient.java`(78)、datagen `AdvancementsProvider.java`(388)/`ChestLootTablesProvider.java`(54)、Trinkets 集成 `LegwearTrinkets.java`(140)/`NekoArmorTrinkets.java`(102)/两个渲染器(126+116)、`msic/*` 注册壳全部。
- **neoforge**：`neoforge/src/**` 全部 16 个文件，共 **1379 行**。逐行读了 `ToNekoNeoForge.java`(138)、`items/ToNekoItems.java`(438)、`entities/ToNekoEntities.java`(184)、客户端 `ToNekoNeoForgeClient.java`(134)、`msic/*` 全部。
- **bukkit**：`bukkit/src/**` 25 个文件共 **3309 行**，覆盖约 85%（精读入口 `ToNeko.java`(88)、`ChatEvent.java`(212)、`EnergyManager.java`(74)、`items/NekoItems.java`(252)、`PayloadSender.java`(101)、调度器双实现(40+45)、events/api/util 小类全部；命令类(1000+ 行)只审权限结构与代表性片段；`Metrics.java`(905) 只读头部与结构）。
- **对照物**：common 的 `ToNekoEntities`、`ChestLootTableRegistry`、`LegwearUtil`/`LegwearUtilImpl`、`ToNekoAttributes(+Impl)`、`NekoSkinRegistry`；`开发规范.md` §3.3/§3.4；`settings.gradle`、两份 `build.gradle(.kts)`、`fabric.mod.json`、`neoforge.mods.toml`、mixins 配置。
- **数据核对手段**：`git log --since=2026-06-01`（bukkit 9 次 / fabric 14 次 / neoforge 15 次）；`git ls-files | grep -c '\.class$'`=38；用 `diff`/`ls` 对比 `fabric/src/main/generated`（23 个 JSON）与 `neoforge/src/main/resources/data/toneko|minecraft`（31+ 个 JSON）；反编译 paper-api sources jar 确认 Folia 调度器 API 语义。

## 总体评价

**6 / 10。**

平台层的架构意图是对的且大部分执行到位：平台类确实是"注册壳"，业务逻辑基本留在 common（实体构造参数委托 common supplier、战利品注入委托 `ChestLootTableRegistry`、`@ExpectPlatform` 双实现极薄），命名 twin-class 对称易导航。但"同一份内容在两处手工维护"的模式已经产生实际漂移：NeoForge 侧数据包与代码路径**双重注入**（战利品、生物生成各来一遍）、物品/实体属性两端不一致、datagen 产物与 NeoForge 手工同步目录已经双向脱节；neoforge 还有成片的死代码；bukkit 是"被排除出构建但仍在维护"的僵尸模块，内含会在 Paper 上直接启动失败的潜在崩溃点。

## 优点

1. **平台层确实很薄，业务逻辑成功收敛到 common**：NeoForge 实体注册完全复用 common 的构造器 `org.cneko.toneko.common.mod.entities.ToNekoEntities.getXxx()`（`neoforge/entities/ToNekoEntities.java:35-67`）；箱子战利品的物品池定义只在 common 一处（`common/mod/util/ChestLootTableRegistry.java:132-146`），两个平台的事件壳各只有 10~30 行（`fabric/msic/ChestLootInjection.java:13-19`）。
2. **twin-class 命名对称，可导航性好**：`ToNekoItems/ToNeoBlocks/ToNekoBlockEntities/ToNekoArmorMaterials/ChestLootInjection/ChestLootTablesProvider` 在 fabric 与 neoforge 一一同名对应，17 个进度触发器两端注册键完全一致（`fabric/msic/ToNekoCriteriaFabric.java:10-34` ↔ `neoforge/msic/ToNekoCriteriaNeoForge.java:12-36`）。
3. **`@ExpectPlatform` 桥接干净且有文档**：`LegwearUtil` 的两个平台实现各仅 20~47 行，Fabric 版对 Trinkets 可选依赖做了运行期反射保护并写明原因（`common/mod/misc/fabric/LegwearUtilImpl.java:16-17`）；接口注释明确解释了 Trinkets 槽位 vs 盔甲槽的平台差异（`common/mod/misc/LegwearUtil.java:11-13`）。
4. **NeoForge 的回填时机遵守了规范 §3.2**：`DeferredHolder.get()` 回填统一放在 `FMLLoadCompleteEvent`（`ToNekoNeoForge.java:121-123`），避免注册表冻结前解引用。
5. **注释解释"为什么"而非"是什么"**：如省凳法棍 BEWLR 两端注册差异的说明（`fabric/client/ToNekoClient.java:69-71`、`neoforge/client/ToNekoNeoForgeClient.java:63`）、Folia scoreboard API 损坏的兼容说明（`bukkit/api/NekoStatus.java:29-33`）。
6. **datagen 本身质量好且产物入库**：`AdvancementsProvider.java` 按进度线分节组织（入门/社交/战斗/等级/丝袜线），criteria 复用 common 触发器；23 个 JSON 提交在 `fabric/src/main/generated/`，符合规范 §2.3。
7. **bukkit 并非死代码而是"活体维护中"**：git 显示 2026-08-09/10 仍有提交同步 common API 变更（如 `AIUtil.sendMessage` 签名变化后补 `.toString()`，commit 7ce97cc6），README_Bukkit.md 明确标注功能差异表，定位诚实。
8. **bukkit 的调度抽象设计合理**：`SchedulerPoolProvider` + 启动时按 `RegionizedServer` 类存在性选择 Folia/Paper 实现（`bukkit/ToNeko.java:47-54`），`ChatModeHolder` 用 `ConcurrentHashMap` 存跨线程聊天模式。

## 问题

（按严重级别排序）

### [较高] 1. NeoForge 战利品双重注入：代码注入 + 数据包 loot_modifier 同时生效
- **位置**：`neoforge/src/main/java/org/cneko/toneko/neoforge/msic/ChestLootInjection.java:17`（激活 common 运行时注入）＋ `neoforge/src/main/resources/data/toneko/loot_modifier/add_*.json`（5 个文件）＋ `common/mod/util/ChestLootTableRegistry.java:48-128`
- **描述**：common 的 `addToTable()` 通过平台事件把猫娘物品池加进原版地牢/矿井/村庄等 15 组战利品表；而 NeoForge 的 5 个 `neoforge:add_table` loot_modifier 把内容几乎相同的 `toneko:chests/neko_dungeon_loot` 等 JSON 再追加进**同一批原版表**（如 `add_neko_dungeon_loot.json` 目标含 `minecraft:chests/simple_dungeon`、`abandoned_mineshaft`）。入口第 115 行调用了 `ChestLootInjection.init()`，两条链路同时活跃 → NeoForge 玩家开地牢箱时猫娘物品出现两次机会（Fabric 只有一次）。
- **建议**：二选一——删掉 5 个 loot_modifier JSON（推荐，保留 common 单一来源），或让 neoforge 的 `ChestLootInjection` 不再注册并全面转向数据驱动；并在规范 §3.1 补一条"战利品注入禁止双通道"。

### [较高] 2. NeoForge 生物生成双重注册：biome_modifier JSON + FFAPI 代码生成并存
- **位置**：`ToNekoNeoForge.java:121-130`（FMLLoadComplete 里调 common `registerBiomeSpawns`，走 FFAPI `BiomeModifications.addSpawn`）＋ `data/toneko/neoforge/biome_modifier/add_*_spawns.json`（7 个 spawn 文件）
- **描述**：`add_adventurer_neko_spawns.json` 的群系集合和权重（35,1,3）与 `common/mod/entities/ToNekoEntities.java:139-160` 中代码注册的完全一致——同一种猫娘在同一群系被登记两次，生成权重近似翻倍；幽灵/水晶/战斗/樱花变体同样重复。野生猫薄荷则只有 JSON 一条路（fabric 用代码 `ToNeko.java:59-71`），三端行为不一致但无重复。
- **建议**：删除 7 个 `add_*_spawns.json`（保留 `add_wild_catnip.json` 或反之统一走 JSON），并把"群系生成只允许一条通道"写进 §3.1 已验证清单的注意事项。

### [较高] 3. 同一物品/实体在两端属性不一致（手工双维护已实际漂移）
- **位置与证据**：
  - 幽灵猫娘碰撞箱：fabric `sized(0.4f,1.2f)`（`fabric/entities/ToNekoEntities.java:45`）vs common supplier `sized(0.5f,1.6f)`（`common/.../ToNekoEntities.java:78-81`，NeoForge 用这份）；
  - Ravenn 生物类别：fabric `MobCategory.MONSTER`（fabric:62-64）vs common `MobCategory.CREATURE`（common:110-116）→ 影响刷新与消失规则；
  - Mouflet Boss：fabric `sized(0.5f,1.7f)` 无 `updateInterval` vs common `sized(0.5f,1.6f).updateInterval(3)`（common:100-104）；
  - 猫薄荷三明治食物值：fabric `FoodProperties(10,12f,…)`（`fabric/items/ToNekoItems.java:54`）vs NeoForge `(6,3.0f,…)`（`neoforge/items/ToNekoItems.java:126-129`）；
  - 火箭筒：fabric 无 `stacksTo/rarity`（fabric:59）vs NeoForge `.stacksTo(1).rarity(RARE)`（neoforge:138）；
  - 猫猫聚合器方块硬度：fabric `strength(5.0f)+requiresCorrectToolForDrops`（`fabric/items/ToNekoBlocks.java:20-21`）vs NeoForge 裸 `Properties.of()`（`neoforge/items/ToNekoBlocks.java:26`）。
- **描述**：这些不是风格差异而是玩法差异——同一玩家在不同平台获得不同体验，且未来每次改动都要靠人肉记得改两边。
- **建议**：把物品属性（食物值、stack 大小、稀有度）和实体尺寸/类别下沉为 common 常量或 builder 参数，平台层只负责"注册方式"；至少先修 Ravenn 类别与幽灵碰撞箱这类影响手感/刷怪机制的项。

### [较高] 4. datagen 产物与 NeoForge 手工同步目录已双向脱节（规范 §2.3 的风险已兑现）
- **位置**：`fabric/src/main/generated/`（22 个 advancement + 1 个 chest 表 = 23 JSON）vs `neoforge/src/main/resources/data/minecraft/advancement/toneko/`（25 个）与 `…/loot_table/chests/`（6 个）
- **描述**：NeoForge 多出 `spoiled_water_first/drink/collector` 三个进度 JSON 和 5 个 `neko_dungeon/endgame/rare/treasure/village_loot.json`——datagen 里没有它们（`AdvancementsProvider.java` 未包含变质水线；`ChestLootTablesProvider.java` 只生成 `chests/neko_loot` 一个表）。结果：**Fabric 端玩家拿不到这 3 个进度**；那 5 张表在 Fabric 根本不存在（若被结构引用则是缺失内容）。`neko_loot.json` 内容目前一致（diff 为空），说明"曾经同步过、后来各自长歪"。
- **建议**：短期把这 3 个进度补进 `AdvancementsProvider`；中期给 NeoForge 也上 datagen（Architectury 项目常见做法是共享 datagen 源集或在 CI 加一步复制校验），用 `diff -r` 做 CI 门禁，杜绝"手工复制"这一步。

### [较高] 5. bukkit 在非 Folia 的 Paper 上会启动失败（与 README 承诺矛盾）
- **位置**：`bukkit/EnergyManager.java:18-20`、`bukkit/items/NekoItems.java:44`、`bukkit/events/QuirkEventHandler.java:18` 直接调用 `Bukkit.getGlobalRegionScheduler().runAtFixedRate(...)`；而 `bukkit/ToNeko.java:47-54` 明确检测到非 Folia 时走 `BukkitSchedulerPool`
- **描述**：Paper（非 Folia）上 `getGlobalRegionScheduler()` 抛 `UnsupportedOperationException`，`onEnable()` 会中断导致插件启用失败。README_Bukkit.md 却宣称支持 "Paper / Purpur / Folia 1.21.x"。该模块因从 `settings.gradle` 注释掉（`settings.gradle:15`）而不参与构建，所以问题处于潜伏状态——但也意味着没有任何构建验证能拦住它。
- **建议**：三处调用改为经由 `SchedulerPoolProvider.INSTANCE.scheduleSync(...)`；若要保留 Paper 支持，恢复 bukkit 进 CI 编译（哪怕只是 `compileJava`）。

### [中] 6. NeoForge 创造模式栏第二个填充分支是死代码，且内部有重复添加 bug
- **位置**：`neoforge/items/ToNekoItems.java:304-374`（`@SubscribeEvent buildContents`）
- **描述**：该类从未被注册到任何事件总线（全仓库无 `bus.register(ToNekoItems.class)`/`addListener`），整个方法不可达。它与创造栏 builder 的 `displayItems` lambda（226-290 行）是两份几乎相同却又不一致的清单：此处 320 行未注释的 `PLOT_SCROLL_HOLDER` 会显示（displayItems 中被注释）、319 行 NOELLE 刷怪蛋被反向注释、**325-329 行愚人节唱片会无条件加一次再条件加一次（重复堆叠）**、373 行还会再跑一遍 `reg()`。谁要是"顺手"把它注册上总线，就会立刻触发上述 bug。
- **建议**：删除该方法与 `reg()` 调用；创造栏清单只保留 `displayItems` 一份。

### [中] 7. `ToNekoItems.reg()` 变成了全模组回填总调度（隐藏耦合）
- **位置**：`neoforge/items/ToNekoItems.java:376-437`
- **描述**：物品类的 `reg()` 末尾串行调用 `ToNekoEffectNeoForge.reg()/ToNekoBlocks.reg()/ToNekoEntities.reg()/ToNekoCriteriaNeoForge.reg()/ToNekoMenuTypesNeo.reg()/ToNekoRecipesNeo.reg()`（431-436 行）。新增一个注册类别必须记得往"物品类"里塞一行，否则 common 静态字段保持 null 直到某处 NPE——这是典型的知识藏在错误位置的写法，也违反 §3.2"回填通过各自 reg() 方法"的本意。另外 `ToNekoCriteriaNeoForge.init()` 是空方法仍被入口调用（`ToNekoCriteriaNeoForge.java:38-39`），误导读者以为那里有初始化。
- **建议**：入口的 FMLLoadComplete 监听里逐一显式调用各类自己的 `reg()`；删除空 `init()`。

### [中] 8. NeoForge 战利品注入依赖反射且静默吞异常
- **位置**：`neoforge/msic/ChestLootInjection.java:33-46`
- **描述**：通过 `getDeclaredField("pools")` 反射读取临时 LootTable 的池列表，`catch (ReflectiveOperationException ignored)` 连日志都没有。一旦映射名变更或加了模块封锁，箱子注入整体失效且零提示。仓库平台层共有 **18 处** `catch … ignored` 式静默吞异常（渲染、网络、IO 都有），这里是后果最重的一处。
- **建议**：改用 `event.getTable()` 的公开 API 或 NeoForge loot modifier 单通道（见问题 1）；catch 里至少 `LOGGER.error` 一次。渲染处的 `catch (Exception ignored) {}`（如 `fabric/client/items/NekoArmorTrinketsRenderer.java` 渲染回调内）也应降频记录。

### [中] 9. bukkit 异步线程直接改共享状态与调用主线程 API
- **位置**：`bukkit/events/ChatEvent.java:32-66`
- **描述**：Paper 的 `AsyncChatEvent` 在异步线程触发，处理器里：`neko.addLevel(...)`（64 行）在异步线程改写 NekoQuery 共享数据（与 `PlayerConnectionEvents.onPlayerQuit` 主线程的 `saveAndRemoveNeko` 竞争）；`modify()`/`sendAIResponse()` 调 `Bukkit.getPlayer(uuid)`（140、201 行）与遍历在线玩家取坐标（77-103 行）均为非线程安全用法。另外 `ClientStatus.INSTALLED` 是普通 `HashMap<Player,Boolean>` 且键为强引用的 Player 对象，`onPlayerQuit` 从不清理（`api/ClientStatus.java:11` + `events/PlayerConnectionEvents.java:32-35`）——内存泄漏 + 插件消息监听器默认异步写入的竞态；同文件的 `ChatModeHolder` 却用了并发容器，标准不一致。
- **建议**：`onChat` 里先把消息打包，`Bukkit.getScheduler().runTask` 回主线程处理全部游戏逻辑；`INSTALLED` 改为 `Map<UUID,Boolean>` 并在 quit 时移除。

### [中] 10. 38 个编译产物 .class 被 git 跟踪（含奇怪的 stash-dir）
- **位置**：`git ls-files | grep -c '\.class$'` = 38，全部位于 `bukkit/build/classes/java/main/...` 及其下 `stash-dir/*.class.uniqueId*`
- **描述**：最近的 bukkit 提交（如 0ac45d36 "AI AI喵！"）改的全是这些二进制 class 而不是源码——IDE 的本地历史/stash 目录混进了版本库，既污染 diff 又会随每次构建制造冲突。
- **建议**：`git rm -r --cached bukkit/build` 并补 `.gitignore`（根目录应忽略 `build/`）。

### [中] 11. EMI 集成两端不对等，NeoForge 工作站图标错误
- **位置**：`fabric/client/msic/ToNekoEmiPlugin.java:36`（工作站 = `NEKO_AGGREGATOR_ITEM`，且注册了完整 `StandardRecipeHandler` 支持配方搬运）vs `neoforge/client/msic/ToNekoEmiPlugin.java:27`（工作站图标是 `Items.CRAFTING_TABLE`，无配方 handler）
- **描述**：NeoForge 玩家在 EMI 里看到的工作站是工作台而非聚合器，且无法一键填充/移动材料；同一功能两端体验不同。
- **建议**：NeoForge 侧换成聚合器物品（DeferredHolder.get()）并补齐 handler。

### [低] 12. 杂项一致性/卫生问题（合并列出）
- `CRITERION_TRIGGERS` DeferredRegister 以 `"minecraft"` 为命名空间创建（`ToNekoNeoForge.java:56`），虽然注册键带全名可用，但语义错误、报错信息会误导。
- 入口初始化顺序偏离规范 §3.3 声明的"固定顺序"：NeoForge 是 armorMaterials→items→effects→blocks→…→quirks→entities→packets→commands→chestLoot（`ToNekoNeoForge.java:82-118`），commands 从最前变到最后、items 先于 blocks。当前 DeferredRegister 下无实害，但文档与现实二选一需要修正。
- 创造栏注册 ID 不一致：fabric `"item_group"`（`fabric/items/ToNekoItems.java:183`）vs neoforge `"toneko_group"`（`neoforge/items/ToNekoItems.java:223`）。
- `tryClass` 在两个平台的 `ToNekoItems` 各拷贝一份，NeoForge 版无人使用（`neoforge/items/ToNekoItems.java:295-302` 死代码；fabric 版有用）。
- 包名拼写 `msic`（应为 misc）扩散到 17 个平台文件。
- `WorldEvents.onWorldLoad` 名不副实地监听 `WorldUnloadEvent` 且漏写 `@EventHandler`（`bukkit/events/WorldEvents.java:18-21`），实际生效的是 `QuirkEventHandler.onWorldUnload`（24-26 行）里的重复实现。
- `Language.LanguageImpl` 非静态内部类、空实现、无人引用（`bukkit/util/Language.java:14-19`）；`ChatEvent.RANDOM` 声明未使用（`ChatEvent.java:25`）。
- Trinkets 尾巴渲染器重复注册两次（`fabric/client/items/NekoArmorTrinketsRenderer.java:27-28`）；渲染回调每帧 `NEKO_TAIL.getRenderer()` 新建（被注释掉的 null 缓存说明作者知道）。
- 自定义事件 `AttributeEvents.ON_REGISTER_PLAYER_ATTRIBUTES` 全仓无任何监听者（`fabric/api/events/AttributeEvents.java`），mixin 里 `cancellable=true` 也属多余（`fabric/mixins/PlayerEntityMixin.java:14`）。
- Noelle 女仆猫娘皮肤复用 `FightingNekoEntity.NEKO_SKINS`（两端一致：fabric entities:106 / neoforge entities:76），若是有意共用建议加注释，否则像复制粘贴事故。
- bStats `Metrics.java`（905 行）整类 vendor 进仓库，其头部声明"未使用构建工具的项目才可复制"，本项目用 Gradle，官方姿势是依赖 `org.bstats:bstats-bukkit`（`bukkit/ToNeko.java:60` 处 `new Metrics(this, 19899)`）。另 shadowJar 用 40+ 条 `exclude("...*.class")` 手工黑名单裁剪 common（`bukkit/build.gradle.kts`），新增一个触碰 MC 类的 common 类就会以 `NoClassDefFoundError` 的形式炸在用户服上，建议改为白名单或独立 `common-core` 源集。

## 数据速览

- fabric：25 文件 / 1989 行；neoforge：16 文件 / 1379 行（最大单文件 `items/ToNekoItems.java` 438 行）；bukkit：25 文件 / 3309 行（`Metrics.java` 占 905 行 ≈ 27%）。
- 近 2.5 个月提交数：bukkit 9、fabric 14、neoforge 15 —— bukkit 处于"排除构建但仍同步维护"状态；最近一次 bukkit 源码修改 2026-08-09/10（同步 AIUtil 签名变更）。
- git 跟踪的 `.class` 二进制：38 个（全部在 `bukkit/build/classes`，含 stash-dir 变体）。
- 平台层静默吞异常（`catch … ignored` 类）：18 处（fabric+neoforge+bukkit 合计）。
- 进度触发器注册对称性：17 / 17 键完全一致；创造栏物品清单两端各约 50 项、纯手工平行维护。
- datagen 漂移实测：advancement 22(fabric 自动) vs 25(neoforge 手工)；`chests/` 表 1 vs 6；loot_modifier 5 个与 biome_modifier 8 个为 NeoForge 独有格式（其中 7 个 spawn JSON 与 common 代码注册重复）。
- 跨端属性不一致实测 6 处（实体 3、物品 2、方块 1），另有创造栏 ID、EMI 工作站 2 处 UX 分叉。
- bukkit 中无条件使用 Folia-only 调度器的调用点：3 个类 4 处（EnergyManager×2、NekoItems×1、QuirkEventHandler×1）。
