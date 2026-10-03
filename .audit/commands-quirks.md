# 审计报告：命令系统与特质系统 (commands / quirks / impl / api)

> 审计人：ox-alpha（Java/Minecraft mod 代码审计）
> 范围：common 源码中命令、特质（quirk）、impl、api（含 events）四个包，并交叉核对事件分发、mixin 存储层与《开发规范.md》。

## 审计范围与方法

实际通读（read/cat 全文）的文件：

| 包 | 文件 | 行数 |
|---|---|---|
| commands | ToNekoAdminCommand.java | 496 |
| commands | ToNekoCommand.java | 422 |
| commands | NekoCommand.java | 327 |
| commands | QuirkCommand.java | 121 |
| commands | GeneticsCommand.java | 44 |
| commands/arguments | WordSuggestionProvider.java | 63 |
| commands/arguments | NekoArgument.java | 60 |
| commands/arguments | CustomStringArgument.java | 58 |
| commands/arguments | NekoSuggestionProvider.java | 48 |
| quirks | ZakoQuirk.java | 114 |
| quirks | CrystalNekoQuirk.java | 114 |
| quirks | QuirkRegister.java | 65 |
| quirks | CaressQuirk.java | 59 |
| quirks | ModQuirk.java | 49 |
| quirks | Quirk.java | 23 |
| quirks | ToNekoQuirks.java / Quirks.java | 13 / 8 |
| impl | FabricLanguageImpl.java | 42 |
| api | StompSessionManager.java 125、ExplorationLevelFactor.java 101、NekoLevelRegistry.java 42、NekoSkinRegistry.java 41、EntityPoseManager.java 27、NekoNameRegistry/NekoLevelFactor 各19、ChatEvents 48、WorldEvents 18、Interaction/Fishing/Homestead/Combat/Base LevelFactor 共75 | 515 |

**小计：31 个文件，约 2641 行**。另有针对性地核对了关联链路：`INeko.java`（quirk/owner/blockedWord 默认方法段）、`events/CommonPlayerInteractionEvent.java`（232 行全文）、`events/ToNekoEvents.java` 与 `events/ToNekoNetworkEvents.java`（quirk GUI 与 owner-map 相关段落）、`util/PermissionUtil.java` + `api/Permissions.java` 全文、`mixin/PlayerEntityMixin.java`（quirk 持久化）、`ServerLevelMixin→WorldEvents→CommonWorldEvent` 天气链路、`AIUtil` 回调线程模型、`JsonConfiguration` 锁实现，以及 fabric/neoforge 两端入口对命令 init 的注册顺序和《开发规范.md》全文（365 行）。方法上以"逐命令核对权限树 → 逐 executes 核对参数校验与返回值 → 追踪每个 quirk 钩子的完整调用链"进行，全部结论均有行号证据。

## 总体评价

**5.5 / 10**

命令系统的骨架是健康的：权限常量集中、40 处 `.requires` 全部走统一的 `PermissionUtil.has`、自定义 ArgumentType/SuggestionProvider 服务端权威校验，符合《开发规范.md》§12.1 的方向。但实现层 bug 密度明显偏高：存在一个**每次上线即清空玩家全部 quirk 的数据丢失级逻辑反转**（`fixQuirks`），一个**所有权校验字段声明了却从未生效导致的越权写**，以及多处"发了错误提示却不 return 继续执行"的漏 return 型缺陷；同时 quirk 子系统两个实现类近乎全量复制，扩展成本被人为放大。

## 优点

1. **StompSessionManager 是全仓库标杆级实现**（api/StompSessionManager.java:1-125）：`ConcurrentHashMap`、私有构造器、断线（`onPlayerQuit`:70）与目标死亡（`onTargetDeath`:78）双清理路径、javadoc 完整记录语义与边界情况。
2. **权限树覆盖规范**：5 个命令类共 75 个 literal 节点、40 处 `.requires(...)` 全部经 `PermissionUtil.has` 统一入口；37 个权限常量集中在 common/api/Permissions.java:10-47，无散落硬编码字符串，符合规范 §12.1。
3. **common 单份命令逻辑两端复用**：所有命令类直接用 FFAPI `CommandRegistrationCallback.EVENT`（如 ToNekoAdminCommand.java:39），fabric/neoforge 入口各只加一行 init()（fabric/src/.../ToNeko.java:49-53、neoforge/src/.../ToNekoNeoForge.java:108-112），完全符合 §3.1 "只写一份在 common"。
4. **quirk 分发框架设计良好**：CommonPlayerInteractionEvent.java:44-88 用 sealed interface `EventContext` + record 子类 + 泛型 `processInteractEvent` 统一处理三类事件的遍历、SUCCESS 拦截与 XP 发放，且 XP 只在 `hasOwner` 时发放（:117-121）；新增 quirk 无需触碰分发代码。
5. **建议补全是服务端权威的**：NekoSuggestionProvider.java:38-48 在服务端校验目标是否 neko 及属主关系后才给出建议；CustomStringArgument.java:37-58 自定义解析带 Pattern 校验 + 1~40 长度上限 + 空值拒绝。
6. **配置读写线程安全**：JsonConfiguration 所有访问 `synchronized(lock)`（JsonConfiguration.java:69-77），ToNekoAdminCommand 的 CONFIG_KEYS 建议器动态来自 ConfigBuilder（ToNekoAdminCommand.java:34-37）。
7. **后台线程切换有正确示范**：NekoCommand.chatCommand.java:312-319 在 AI 回调里先 `player.getServer().execute(...)` 再执行动作/发消息，Messaging.sendNekoChat 发包带 try-catch 忽略断线（misc/Messaging.java:128-133），严格符合 §8.2。
8. **注册表模式统一简洁**：QuirkRegister/NekoLevelRegistry 均为静态 LinkedHashMap + register/get/has 三件套（QuirkRegister.java:10-64），`getQuirkIds().stream().toList()` 对外只读视图意图明确。

## 问题

### [严重]

**S1. `INeko.fixQuirks()` 谓词写反——猫娘玩家每次登录清空全部合法 quirk（持久化数据丢失）**
- 位置：entities/INeko.java:182-185；调用点 events/ToNekoEvents.java:202
```java
default void fixQuirks(){
    this.getQuirks().removeIf(quirk -> QuirkRegister.hasQuirk(quirk.getId()));
}
```
- 描述：注释为"修复quirks"，本意应是删除**未注册**的失效 id，但条件少了 `!`——凡已注册的 quirk 一律移除。该方法在 `onPlayerJoin` 中对 `isNeko()` 玩家无条件调用（ToNekoEvents.java:201-202），而 NBT 加载（PlayerEntityMixin readAdditionalSaveData）发生在 join 之前，因此时序为"读档 → 清空 → 下线写空档"，玩家的 `/quirk add` 成果每次重登被永久清空。
- 建议：改为 `removeIf(q -> !QuirkRegister.hasQuirk(q.getId()))`；并为该路径补一个单测（构造含合法+非法 id 的列表断言结果）。

### [较高]

**H1. `NekoArgument.requireOwned` 从未被读取——所有权校验整体失效，任意玩家可改任意猫娘的聊天屏蔽词**
- 位置：commands/arguments/NekoArgument.java:13,16-18,30-51
- 描述：`ownedNeko()` 的 javadoc 写明"校验目标是否是 Neko 并且是主人"（:25），但 `parse()` 只校验在线与非 neko，`requireOwned` 字段存而不用（全库 grep 仅 NekoSuggestionProvider 用自己的同名副本）。后果：
  - `/toneko block add <任意neko> <词> <替换> <方式>` 直接调 `neko.addBlockedWord(...)`（ToNekoCommand.java:244），**任何玩家可篡改任何猫娘的聊天过滤**；
  - `/toneko aliases add/remove` 对非属主会因 `getOwner(uuid)==null` 抛 NPE 被 catch 吞掉（ToNekoCommand.java:286,300），用户无反馈；
  - 对照组证明这是缺陷而非设计：GUI 路径 `handleGuiAddAlias` 有 `if (!neko.hasOwner(player.getUUID())) return;`（ToNekoNetworkEvents.java:754-757）。
- 建议：在 `NekoArgument.parse` 中读取 requireOwned 并抛出 CommandSyntaxException；或退一步在每个 executes 里补属主校验。

**H2. `giveEffect` 能量不足不 return——照常扣能量并发效果，能量可为负**
- 位置：commands/NekoCommand.java:266-291
```java
}else if (player.getNekoEnergy()<100){
    player.sendSystemMessage(translatable("command.neko.effect.not_enough_energy"));
}   // ← 缺 return
player.setNekoEnergy(player.getNekoEnergy()-100);
```
- 描述：提示"能量不足"后继续扣 100 能量并授予药水效果；`setNekoEnergy(-x)` 使能量为负。经济系统（能量）形同虚设。
- 建议：该分支补 `return 1;` 并按规范返回 0。

**H3. quirk GUI 网络 handler 权限降级 + 无 id 校验（null 可注入 quirk 列表）**
- 位置：events/ToNekoNetworkEvents.java:643-654
```java
if (!PermissionUtil.has(player, Permissions.COMMAND_QUIRK)){ ... return; }
var quirks = player.getQuirks();
quirks.clear();
quirks.addAll(payload.getQuirks().stream().map(QuirkRegister::getById).toList());
```
- 描述：① 命令侧 `/quirk add`、`/quirk remove` 分别要求 `COMMAND_QUIRK_ADD/REMOVE`，此处仅查基础 `COMMAND_QUIRK`，持有客户端 mod 的玩家可绕过细分权限任意改自己的 quirk；② `getById` 对未知 id 返回 null 且未过滤，恶意 payload 可把 null 塞进列表，后续 `Quirk::getId` 调用处（如 QuirkCommand.listQuirks:78）NPE。违反规范 §10.2 校验链第③步（不可信输入须校验）。
- 建议：按 payload 语义分别要求 ADD/REMOVE 权限；`map(::getById).filter(Objects::nonNull).distinct()`。

**H4. 命令块可执行全部管理命令（权限检查对非实体来源一律放行）**
- 位置：util/PermissionUtil.java:81-84
```java
public static boolean has(CommandSourceStack source, String permission){
    if (source.getEntity() == null) { return true; }
```
- 描述：命令块的 `CommandSourceStack.getEntity()` 同样为 null，因此命令块绕过 tonekoadmin→op4 的映射，可执行 `/tonekoadmin config set`、`/tonekoadmin ai config`（写入 AI API key）等全部管理命令。控制台放行合理，命令块不应同等对待。
- 建议：改为 `source.hasPermission(4) || source.getEntity() == null && source.source instanceof MinecraftServer` 之类的显式区分，或对 null 来源回落到 `source.hasPermission(getPermLevel(perm))`。

**H5. 天气事件把 `isRaining` 当 `isThundering` 传——雷暴状态永久失真**
- 位置：api/events/WorldEvents.java:11
```java
listener.onWeatherChange(world,clearTime, weatherTime, isRaining, isRaining);
```
- 描述：链路 ServerLevelMixin → WorldEvents → CommonWorldEvent → CrystalNekoQuirk/ZakoQuirk.onWeatherChange。雷暴分支（`isThundering`）永远收不到 true；雷暴天显示的是雨天台词，`weather.thunder` 文案成为死资源。上游 mixin 传参正确，纯属本行笔误。
- 建议：末参改 `isThundering`。

**H6. `ai config` 把 API key 明文回显到聊天与日志**
- 位置：commands/ToNekoAdminCommand.java:461
```java
source.sendSystemMessage(translatable("command.tonekoadmin.ai.config", providerId, key, value));
```
- 描述：`key=...` 时 value 即 API 密钥，回显后进入聊天记录/日志文件；且整个 `ai` 子树只挂基础 `command.tonekoadmin`（:127），没有独立的 `...ai.*` 权限常量，被授权基础节点的管理员也能读写密钥并触发外呼（`ai test`:468）。
- 建议：key 类值脱敏显示；为 ai 子树补充独立权限节点并登记 Permissions.java。

### [中]

**M1. `getConfig` 缺 return + `setConfig` 的 `"1"` 反设为 false + 吞异常返回成功**
- 位置：commands/ToNekoAdminCommand.java:203-210（not_found 后未 return，继续 `CONFIG.get(key)` 得 null 拼进 translatable，异常再被 :209 吞掉）；:185-186（接受 `"1"/"0"` 但用 `Boolean.parseBoolean(value)`，`"1"` → false，与直觉相反）；:197-198（`catch (Exception ignored){}` 后仍 return 1）。
- 建议：not_found 后 return；布尔分支用 `"1".equals(value) || Boolean.parseBoolean(value)`；catch 中向来源发送失败消息并 return 0。

**M2. deny 分支复用 accept 的翻译键**
- 位置：commands/ToNekoCommand.java:203 `Component.translatable("command.toneko.accept", ...)`（RED 样式）；同文件 GUI 版 handleGuiDeny 用的是正确的 `command.toneko.deny`（ToNekoNetworkEvents.java:737），证明是复制粘贴错。猫娘拒绝主人时会看到"接受"文案。

**M3. `ownerMap` 强引用 Player 且断线不清理**
- 位置：commands/ToNekoCommand.java:189 `private static Map<Player,Player> ownerMap = new HashMap<>();`
- 描述：以 Player 实例为 key（身份等价），请求发出后若对方永不响应或任一方断线，条目永久滞留（ToNekoEvents.onPlayerQuit:218-221 只清理 StompSessionManager）；重登后旧实例成死引用，既泄漏又可能让旧点击按钮命中过期状态。
- 建议：key 改 UUID，或挂接 DISCONNECT 事件清理；加超时过期。

**M4. `addOrRemoveQuirk` 用扫描 `context.getNodes()` 区分 add/remove**
- 位置：commands/QuirkCommand.java:97-113
- 描述：Brigadier 反模式——靠字面量节点名猜意图，脆弱且难读；应拆成两个 executes 方法引用（`QuirkCommand::addQuirk` / `::removeQuirk`），共享一个私有 doApply。

**M5. ZakoQuirk 与 CrystalNekoQuirk 114 行 ×2 近乎全量复制**
- 位置：quirks/ZakoQuirk.java 与 quirks/CrystalNekoQuirk.java
- 描述：两者除翻译键前缀与随机台词范围（16/17 vs 5）外逐行相同（onDamage/onJoin/onNekoAttack/onWeatherChange/startSleep/stopSleep 六个钩子结构一致），重复率约 95%。每新增一个"台词型" quirk 就要再抄 114 行。
- 建议：抽 `MessageBroadcastQuirkBase(String id, String keyPrefix, int variantRange)`，子类一行构造；随机范围也可做成构造参数。

**M6. EntityPoseManager 公有可变 HashMap、强引用实体、无自动清理**
- 位置：api/EntityPoseManager.java:14 `public static Map<Entity, Pose> poseMap = new HashMap<>();`
- 描述：与 StompSessionManager 的标准（ConcurrentHashMap + 生命周期清理）形成鲜明反差；躺倒的实体死亡/卸载后条目残留，`poseMap` 公开可直接被外部污染。
- 建议：字段私有化、按需弱引用或在实体 remove 钩子里清理。

**M7. i18n 多处裸串，违反规范 §12.1/§14**
- 位置：GeneticsCommand.java:35 `sendFailure(Component.literal("该实体没有基因组！"))`；CustomStringArgument.java:50,54（中文异常文案）；NekoArgument.java:39（中文）与 ：45（translatable）混用；ToNekoAdminCommand.nekoInfo/aiList 整段英文 `§x` 硬编码（:277-308, :410-417）。
- 建议：统一走 `TextUtil.translatable` + lang 五文件同步。

**M8. `aiTest` 在 AI 回调线程直接发包，违反 §8.2**
- 位置：commands/ToNekoAdminCommand.java:486-489 —— `AIUtil.sendMessage(..., response -> source.sendSystemMessage(...))` 未切主线程（回调执行于 AIUtil 的 100 线程池，AIUtil.java:265,400）；对照 NekoCommand.chatCommand:314 的正确写法。另外 ：491 把"非猫娘"提示错用于"命令来源不是玩家"的场景。

### [低]

**L1. PermissionUtil.register()/registerAll() 是无效仪式代码**
- util/PermissionUtil.java:33-49,52-64：`Permissions.check(uuid, perm)` 只是查询并丢弃结果，并非任何"注册 API"；registerAll 还漏了 7 个常量（COMMAND_QUIRK_REMOVE、COMMAND_NEKO_RIDE/RIDE_HEAD/CHAT/GUI/HELP、COMMAND_TONEKOADMIN_NEKO），制造"已注册"假象。另 ：92-95 存在 `@Deprecated` 的参数顺序颠倒重载。

**L2. 装/不装 LuckPerms 行为翻转**
- util/PermissionUtil.java:66-79：无 LP 时玩家命令按 op0 全员可用；装 LP 后走 fabric-permissions-api 默认回退（op 级 2），普通玩家反而全部被拒，除非服务器在 LP 里逐条授权。部署行为不可预期，至少应在文档/日志中说明。

**L3. 屏蔽词补全建议的是替换词而非屏蔽词**
- arguments/WordSuggestionProvider.java:34-39：BLOCK 类型 suggest `blockWord.replace()`，而 `removeBlockedWord` 按 `block` 字段匹配（INeko.java:155-157），选中建议后删除静默无效。

**L4. 命名与小瑕疵**
- ToNekoCommand.java:281,295 `AliasesAdd/AliasesRemove` 违反 camelCase；:154 `assert player != null` 运行时不生效；ZakoQuirk.java:15 小写常量 `id` 对照 CrystalNekoQuirk.ID 不一致。

**L5. 死分支与无效调用**
- CrystalNekoQuirk.java:73-74 / ZakoQuirk.java:73-74：`else if (entity.getHealth() <= 0)` 不可达（`ratio <= 0.2` 已兜底）；两处 onDamage 末档 `amount >= 6` 应为 else；CaressQuirk.java:39 服务端 `level().addParticle(...)` 是空操作（服务端无粒子系统），应删。

**L6. 死代码**
- util/CommandUtil.java:19-28 `getOnlinePlayers` 用 `player.getName().toString()` 做 `"literal{...}"` 字符串手术取名字（应为 `getString()`），且全库无调用点，建议连同风险一并删除。

**L7. 失败路径普遍 return 1，违反规范 §12.1 "executes 返回 1 成功/0 失败"**
- 如 QuirkCommand.addOrRemoveQuirk:94（未知 quirk 仍 return 1）、ToNekoCommand 各 catch 块（7 处 `catch (Exception e){ LOGGER.error(e); return 1; }`，异常对用户完全静默）。

**L8. 六个 LevelFactor 类仅差一个常数 C**
- Combat(60)/Fishing(100)/Homestead(180)/Interaction(300)/Exploration(200)/Base：同一 `(sqrt(1+8raw/C)-1)/2` 公式复制六份（如 CombatLevelFactor.java:11-13 与 FishingLevelFactor.java:11-13），可合并为一个 `FormulaLevelFactor(id, C)`。

**L9. quirk 扩展性小结（对应"新增 quirk 成本"关注点）**
- 接口层成本低：新 quirk = extends Quirk + getTooltip/getInteractionValue + ToNekoQuirks 注册三步即可接入全部事件钩子（ModQuirk default 方法设计良好）。但成本被两点拉高：① 台词型 quirk 被迫复制百行模板（见 M5）；② QuirkRegister.register 对重复 id 静默覆盖（LinkedHashMap.put，QuirkRegister.java:17-19），无告警；`Quirks.init()` 空类（quirks/Quirks.java:5-9）是无意义的占位遗留。impl 包仅 FabricLanguageImpl 一个类且规范 §3.1 已自我声明为历史遗留命名，职责尚清晰但建议按规范改名去掉 Fabric 前缀。

## 数据速览

- 审计文件 31 个 / 约 2641 行（commands 1639、quirks 445、impl 42、api+events 515），另精读关联文件 8 个（《开发规范.md》365 行）
- 5 个命令类、75 个 literal 节点、40 处 `.requires` 权限检查、37 个权限常量（Permissions.java）
- registerAll 仅覆盖 30/37 个权限常量（漏 7 个）
- ToNekoCommand 内 7 处 `catch (Exception e){ log; return 1; }` 吞异常模式；ToNekoAdminCommand 另有 2 处 `catch (Exception ignored){}`
- 发现问题 24 项：严重 1、较高 6、中 8、低 9
- ZakoQuirk/CrystalNekoQuirk 各 114 行，估计重复率 ~95%；六个 LevelFactor 类公式完全同构
- ownerMap / EntityPoseManager.poseMap 两处公有或半公有静态 Map 无生命周期清理；StompSessionManager 为唯一具备完整清理路径的同类实现
- quirk 相关持久化 bug 1 个（fixQuirks 反转），影响范围：所有 isNeko 玩家每次登录
