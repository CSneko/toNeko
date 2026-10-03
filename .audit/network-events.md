# toNeko 审计报告：网络包与事件总线 (packets / events / misc / mixin)

## 审计范围与方法

实际通读（read/grep 全文阅读，非抽样）的代码：

| 模块 | 文件数 | 行数 | 说明 |
|---|---|---|---|
| `common/.../mod/packets/` | 39 | 1052 | 26 个根目录 payload + 13 个 `interactives/`，含 `ToNekoPackets` 注册类 |
| `common/.../mod/events/` | 7 | 1823 | 核心 `ToNekoNetworkEvents`(898)、`ToNeoEvents`(229)、`CommonPlayerInteractionEvent`(223)、`NekoMultiToolEvents`(265)、`CommonChatEvent`(142) 等 |
| `common/.../mod/misc/` | 16+1 | 1049 | `Messaging`(270)、`CauldronSpoilageData`(108)、`Charm`、`ScentUtil`、`WetnessUtil`、`ZettaiRyouiki`、`ToNekoComponents`(128) 等 + `mixininterface/SlowTickable` |
| `common/.../mod/mixin/`（非 client） | 8 | 672 | `PlayerEntityMixin`(374)、`EntityMixin`(81)、`ServerPlayerEntityMixin`、`BlockPlaceMixin`、`CreeperMixin`、`FishingHookMixin`、`CatEntityMixin`、`ServerLevelMixin` |

合计约 **4596 行 / 70 个文件**。另为验证线程模型与跨包语义，查阅了：gradle 缓存中 Fabric API networking 6.3.1 与 Forgified Fabric API 4.3.1 的源码 jar（确认 `PlayPayloadHandler` 在两个平台均在主线程回调）、`ClimbWallHandler`、`StompSessionManager`、`EntityPoseManager`、`Genome.load`、`AIUtil` 冷却逻辑、`PermissionUtil`/`Permissions`、`QuirkRegister`、bukkit 模块 `NetworkingEvents`、以及《开发规范.md》§8.2/§10 网络规范原文。

方法：以"恶意客户端伪造包"为主线逐个 handler 做威胁建模（权限→上下文→数值范围→距离→频率），再核对线程模型、静态可变状态、Mixin 注入点质量，并与《开发规范.md》声明逐条比对。

## 总体评价

**6 / 10。** 新写的功能包（legwear 三件套、stomp、climb）严格落实了《开发规范.md》§10.2 的 C2S 安全校验链，是全仓库规范性最好的部分；payload 层 record+StreamCodec 结构高度统一。但存在一个影响全服性能的核心 Mixin 缺陷（所有实体每 2 tick 无条件广播姿态包）、一个客户端可任意写入自身数据并可注入 null 引发 NPE 的处理器（QuirkQuery）、一个完全未鉴权的转发处理器（PlayerLeadByPlayer），以及多处资源无上限与静默吞异常问题。

## 优点

1. **安全校验链在多数新包中落地且与规范一致**：`onLegwearAdjust`（ToNekoNetworkEvents.java:196-231）依次做菜单类型校验 → 物品种类校验 → 服务端权威 `Mth.clamp(payload.denier(),5,120)` → 幂等去重（值未变不写），正是规范 §10.2 点名的"教科书范例"；`LegwearAdjustPayload.java:13` 的 javadoc 也明确写明该设计。
2. **距离与 UUID 校验统一收敛在 `processNekoInteractive`**（ToNekoNetworkEvents.java:623-636）：UUID 解析 try-catch + `distanceToSqr(player) > 64` 拒绝，交配/骑乘/送礼/跟随等 7 个交互包全部复用，避免各写一套。
3. **AI 回调的线程切换纪律严格**：所有 AI 后台回调均先 `player.getServer().execute(...)` 再触碰实体（如 ToNekoNetworkEvents.java:316-324 注释明示原因），`ServerPlayNetworking.send` 一律 try-catch 忽略断线——与规范 §8.2 完全吻合。
4. **服务端状态变更后显式补发原版同步包**：丝袜工作台改组件后回发 `ClientboundContainerSetSlotPacket`（:191-192, :228-229），下客后补发 `ClientboundSetPassengersPacket`（:303, :607），符合规范"补发原版同步包"条目。
5. **服务端权威的能力系统**：`ClimbWallPayload` 处理器只接受开关与方向符号，服务端在 tick 里重新做贴墙检测（`isAgainstWall`）、能量扣费与越界清理（ClimbWallHandler.java:69-97, :143-151）；客户端传来的 `verticalInput` float 仅用于 >0.5/<-0.5 阈值判断，伪造大数无害。
6. **会话生命周期管理完整**：`StompSessionManager` 有 3 格距离上限常量（MAX_DISTANCE=3）、断线恢复目标姿态（onPlayerQuit）、目标死亡清理会话（onTargetDeath），并在 `ToNekoEvents.init()` 中注册了对应死亡钩子（ToNekoEvents.java:89）。
7. **持久化数据类干净**：`CauldronSpoilageData` 作为 SavedData 实现 clamp（:59,:102）、幂等写 + setDirty（:62-68）、0 值自动移除条目，读写对称。
8. **misc 工具类多为无状态纯函数**：`Charm`/`ScentUtil`/`WetnessUtil`/`ZettaiRyouiki` 不持有任何字段、输入输出明确 clamp，天然可测试；`ScentUtil.accumulate/wash`（ScentUtil.java:50-69）语义清晰注释到位。
9. **Mixin 整体克制且命名规范**：非 client 仅 8 个文件 672 行；新增成员一律 `toneko$` 前缀 @Unique；`ServerLevelMixin` 只做 12 行的事件转发，把天气变化桥接到自定义 `WorldEvents.ON_WEATHER_CHANGE`，侵入性低。

## 问题

### [严重] EntityMixin.tick：所有实体每 2 tick 无条件广播姿态同步包
- 位置：`mixin/EntityMixin.java:73-96` + `util/EntityUtil.java:101-106`
- 描述：注入点在 `Entity.tick()` HEAD，作用于**每一个实体**。每 2 tick 对每个实体执行一次 AABB 范围查询（`world.getEntitiesOfClass(Player.class, box)`，16 格盒），然后**无论该实体是否有自定义姿态都向范围内玩家发包**（pose 为 null 时也构造 `status=false, pose=STANDING` 的包发送）。满载服务器数千实体意味着每秒上万次范围查询；每个玩家周围几十个实体时，仅此一项就产生数百 pkt/s 的纯冗余自定义流量（内容永远是"没有姿态"），属于可被放大利用的性能/带宽 DoS 面。
```java
var pose = EntityPoseManager.getPose(entity);
if (pose == null){ status = false; pose = Pose.STANDING; } // 仍然继续发包！
var players = EntityUtil.getPlayersInRange(entity, entity.level(), 16);
for (Player player : players) { ServerPlayNetworking.send(...); }
```
- 建议：仅在 `EntityPoseManager.contains(entity)` 时才同步；或维护 dirty 标记，只在姿态增/删/变时发包一次，由客户端超时自行回落。

### [较高] QuirkQueryPayload：客户端可整体覆写自身性癖列表并注入 null 引发 NPE
- 位置：`events/ToNekoNetworkEvents.java:643-653`、`quirks/QuirkRegister.java:34-36`（`getById` 标注 `@Nullable`）、`entities/INeko.java:183-186`
- 描述：处理器直接 `quirks.clear(); quirks.addAll(payload.getQuirks().stream().map(QuirkRegister::getById).toList())` —— 数据方向反了（服务端应为主权威，客户端最多请求修改）。两重后果：
  1. 未知 id 经 `getById` 得 **null 并被 addAll 进玩家 quirk 列表**；`fixQuirks()` 的 `removeIf(quirk -> ...quirk.getId())` 在下次 onJoin 触发时 NPE（INeko.java:184），`QuirkCommand.listQuirks` 的 `map(Quirk::getId)` 同样 NPE——玩家可自伤至无法正常使用相关命令/流程。
  2. 权限门槛形同虚设：未装 LuckPerms 时 `PermissionUtil.getPermLevel("command.quirk")` 返回 0（PermissionUtil.java），即**全员放行**，任何客户端可给自己配任意性癖组合。
  - 佐证：`CommonPlayerInteractionEvent.java:92,196,215` 已对 quirk 做 `q != null` 防御，说明 null 已实际出现过，但防御不彻底。
- 建议：改为服务端校验每个 id（`QuirkRegister.hasQuirk` 过滤后再映射，拒绝 null）；语义上应改成"申请变更白名单"而非整表覆写。

### [较高] onPlayerLeadByPlayer：零鉴权的转发处理器（伪造拴绳 + NPE 吞没）
- 位置：`events/ToNekoNetworkEvents.java:271-281`
- 描述：服务端收到包后不校验发送者是否为 holder/target 本人、不做距离检查，直接向 `holder`、`target` 两个 UUID 对应玩家原样转发；客户端收到后执行 `target.setLeashedTo(holder,false)`（ClientNetworkEvents.java:59-67）。恶意玩家 A 可让 B、C 两名在线玩家的客户端显示一条**服务端不存在的拴绳**（视觉欺骗/干扰）。此外 `getPlayerByUUID` 返回 null 时 `(ServerPlayer) holder` 抛 NPE 被 `catch (Exception ignored)` 吞掉。而合法路径其实已在 `PlayerEntityMixin.hurt`（PlayerEntityMixin.java:253-270）里由服务端主动发包，这个 C2S 接收端没有存在价值。
- 建议：直接删除该接收端；若必须保留，则校验 sender ∈ {holder,target} 且两者距离 ≤ 拴绳长度。

### [较高] GenomeDataPayload：恶意 NBT 可中断基因加载且无数量上限
- 位置：`events/ToNekoNetworkEvents.java:79-103`；`genetics/api/Genome.java:177-194`
- 描述：权限与距离校验做得不错（手持编辑器或 op+perm、8 格内，符合规范⑥双校验），但 `geneticEntity.getGenome().load(payload.genomeNbt())` 里 `ResourceLocation.parse(k)` 抛出的运行时异常未被捕获（load 只 catch `NumberFormatException`）：`pairs.clear()` 先执行，解析中途抛异常会让实体基因组停留在半空状态；同时染色体条目数量无上限（受 32KB 包限制约束但仍可达数千键）。另外 `expressTraits()` 对垃圾基因 id 无过滤。
- 建议：`load` 内对单条解析加 try-catch 跳过坏键；对染色体对数量设上限；解析失败整体回滚（先构建临时 map 再替换）。

### [较高] think 动画的资源消耗无上限 + 盔甲架持久化残留
- 位置：`events/ToNekoNetworkEvents.java:386-508`
- 描述：开启 `ai.show_think` 后按 think 文本生成分行盔甲架（每 40 字符一行）并**为每个字符建一个延时任务**（animateLine:516-529）。think 长度不受任何配置限制，一次长回复即可创建上百个任务队列和数十个 ArmorStand；AI 冷却默认仅 5 秒（fabric/run/config/toneko.json:39 `"ai.cooldown": "5"`）且允许配 0 关闭。更糟的是这些 ArmorStand 是普通持久实体：动画未走完时宕机/重启，隐形 marker 盔甲架将永久残留在区块存档中无人清理。
- 建议：限制最大行数/总字符数；给盔甲架打 tag 并在实体加载时兜底清理；逐字动画改为单个循环任务而非 N 个一次性任务。

### [中] findNearbyEntityByUuid 忽略 range 参数，骑乘距离校验失效
- 位置：`events/ToNekoNetworkEvents.java:663-668`（调用处 :600）
- 描述：
```java
private static Entity findNearbyEntityByUuid(ServerPlayer player, UUID targetUuid, double range) {
    ServerLevel world = (ServerLevel) player.level();
    return world.getEntity(targetUuid);   // range 从未使用
}
```
`onRideEntity` 传入的 5 格范围完全不生效，猫娘可骑乘同维度内任意位置的实体；参数的存在还会误导维护者以为有距离保护。（neko 自身的距离由 processNekoInteractive 兜底，vehicle 则没有。）
- 建议：删除参数或真正实现距离过滤（`entity.distanceToSqr(player) <= range*range`）。

### [中] 聊天历史读取：主线程磁盘 IO 且无节流
- 位置：`events/ToNekoNetworkEvents.java:814-867, 889-896`
- 描述：`onChatHistoryRequest` 直接在处理器里调 `FileStorageUtil.readConversation`（落盘式存储）+ `resolveStorageId` 遍历所有维度找实体，无冷却/频率限制，客户端刷包即可反复占用主线程做 IO。响应体大小取决于历史条数：默认 `ai.max_history=50` 尚可，但配置为 0 表示不限制，长期对话可能逼近客户端bound自定义包 ~1MB 上限导致断线。
- 建议：读盘挪到异步线程后 `server.execute` 回切；对该请求加简单节流；响应裁剪最近 N 条。

### [中] EntityPoseManager.poseMap 公开静态 HashMap + 强引用泄漏风险
- 位置：`api/EntityPoseManager.java:12`
- 描述：`public static Map<Entity, Pose> poseMap = new HashMap<>()` 完全裸奔，任何代码可绕过 set/remove 直改；以 Entity 强引用为 key，只有踩踏/趴下等少数路径调用 remove，实体卸载、非踩踏死亡等场景下条目残留，既泄漏内存又让已死实体保持可达。`EntityMixin.getPose/setPose` 每个 tick 都查询它，放大的脏数据面。
- 建议：字段私有化；用弱引用 key 或挂到实体的附加数据上；在实体 remove 钩子里统一清理。

### [中] 静态可变集合不随玩家退出清理
- 位置：`events/ToNekoNetworkEvents.java:234`（LAST_LEGWEAR_PULL_UP）、`:799`（CHAT_MODES）；`AIUtil.lastRequestTime` 同类
- 描述：均为 ConcurrentHashMap 但从不移除离线玩家条目，长期运行的服务器上随历史玩家数缓慢增长。`StompSessionManager.onPlayerQuit` 有清理（ToNekoEvents.java:221），说明作者知道该模式，这三处漏掉了。
- 建议：在 DISCONNECT 钩子统一清理；或改用 `Cache`/定时清扫。

### [中] NekoMultiToolEvents：DAMAGE_GUARD 无 try/finally；AoE 绕过逐块保护检查
- 位置：`events/NekoMultiToolEvents.java:58-60, 127-147`
- 描述：①`DAMAGE_GUARD.add(entity); entity.hurt(...); DAMAGE_GUARD.remove(entity);` 若 hurt 抛异常守卫永不移除，该实体此后对工具伤害免疫直到重启；②范围破坏对相邻方块走 `level.removeBlock/destroyBlock` 手动路径，保护类模组（领地/圈地）通常只拦截到中心方块的 PlayerBlockBreakEvents，AoE 部分可能绕过权限检查造成越权破坏。
- 建议：try/finally 包裹守卫；AoE 前对每个目标方块走一次可被取消的保护检查（或至少文档声明兼容边界）。

### [中] 6 处 catch (Exception ignored) 吞掉一切异常
- 位置：`ToNekoNetworkEvents.java:158, 279, 321, 336, 371, 634, 882`（其中 6 处为 ignored）
- 描述：`processNekoInteractive` 把整个业务回调包进 `catch (Exception ignored){}`（:634），tryMating/giftItem 等真实 bug 会无声消失，线上排查几乎不可能；其余几处至少应 debug 日志。
- 建议：区分预期异常（非法 UUID）与非预期异常；后者至少 `LOGGER.warn` 一次。

### [低]
- **死代码/通道复用混淆**：`NekoDiaryRequestPayload` 与 `ToNekoModCheckPayload` 从未注册、从未收发；`EntityPosePayload`（S2C）与 `NekoPosePayload`（C2S）共用通道名 `toneko:entity_set_pose`（packets/EntityPosePayload.java:15、interactives/NekoPosePayload.java:20），方向不同不会冲突但极易误读。
- **splitText 宽度算法与注释不符**（ToNekoNetworkEvents.java:402-403）：注释称"英文字符为1，其他字符为2"，但 `Character.isLetterOrDigit('喵')==true`，中日韩字符按 1 计宽。
- **`new java.util.Random()` 每次新建**（:540）应用 ThreadLocalRandom；同文件 `Messaging.PhraseProcessor.runPetPhrases` 用 `appendReplacement` 时未 `Matcher.quoteReplacement(petPhrase)`（Messaging.java:259），口癖文本含 `$`/`\` 时聊天处理会抛异常。
- **无效微瑕疵**：`onPlayerQuit` 中 `String name` 未使用（ToNekoEvents.java:223）；`PlayerEntityMixin` @Shadow 的 `reducedDebugInfo`/`remove` 疑似未使用；`ToNekoDamageTypes` 直接 `.get()` 解 Optional（坏数据包时 NoSuchElement）；`handleGuiAddAlias/AddBlock` 对字符串长度与列表容量无上限，可缓慢膨胀存档。
- **冗余线程切换**：Fabric 6.3.1 与 Forgified Fabric API（NeoForge 经 `enqueueWork`）均已保证 PlayPayloadHandler 在主线程回调（已核对两侧 sources jar），handler 内大量 `context.server().execute(...)` 属于无害但风格不一的重复包裹；建议在规范里写明"handler 本身已在主线程"，只对真正的后台回调要求 execute。

## 数据速览

- 审计文件：70 个 Java 文件 / 约 4596 行（packets 1052 · events 1823 · misc 1049 · mixin 非 client 672）。
- Payload 类：39 个 record（26 + 13 interactives），100% 基于 `CustomPacketPayload` + `StreamCodec`；双向包均按五步法注册两次（`ToNekoPackets.init` 共 40 行注册语句）。
- C2S 接收端：23 个，全部集中在 `ToNekoNetworkEvents`（符合规范 §10.1）；S2C 客户端接收端 15 个集中在 `ClientNetworkEvents`（216 行，参照阅读）。
- 问题分布：严重 1 · 较高 4 · 中 6 · 低 5（组）。
- `catch (Exception ignored)` 在 ToNekoNetworkEvents 出现 6 次；`context.server().execute` 包裹约 14 处（其中大部分属冗余防御）。
- 静态可变状态：ToNekoNetworkEvents 内 2 个 map（CHAT_MODES、LAST_LEGWEAR_PULL_UP）+ EntityPoseManager.poseMap（public）+ Stomp/Climb/MultiTool 各 1~2 个集合；其中 3 处确认缺少离线清理。
- 每 tick 注册的 ServerTickEvents.START_SERVER_TICK 监听：16 个（ToNekoEvents.java:104-119），含爬墙、气味×6、湿度、魅力等重逻辑 handler。
- AI 速率防线：`ai.cooldown` 默认 5s（可配 0 关闭）、区域聊天并发猫娘默认 3 只；除此之外网络层无独立限频。
