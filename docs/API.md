# toNeko 联动 API 规范

> 面向**第三方模组作者**的接入手册：如何声明/探测 toNeko 依赖、读写猫娘数据、注册自己的内容（基因/怪癖/AI 服务商）、触发 AI 对话、监听事件。
>
> 稳定性分级：🟢 稳定（承诺兼容，放心用）｜🟡 试验（可用，但签名可能变动）｜🔴 内部（仅限阅读，不建议直接调用）。
> 配套：《开发规范》（仓库根目录）、[genetics_api.md](genetics_api.md)（遗传系统完整 API）。
> 版本：toNeko 1.9.5 / Minecraft 1.21.1，Fabric 与 NeoForge 通用（见 §7）。

## 1. 入口概览

| 包 | 内容 | 级别 |
|---|---|---|
| `org.cneko.toneko.common.mod.api` | 对外扩展点：名字/皮肤/等级因子注册表、`ChatEvents`/`WorldEvents` | 🟢 |
| `org.cneko.toneko.common.mod.entities.INeko` | 猫娘数据核心接口（全默认方法） | 🟢 |
| `org.cneko.toneko.common.mod.entities.NekoEntity` | NPC 猫娘实体基类 | 🟢 |
| `org.cneko.toneko.common.mod.quirks` | 怪癖接口与注册表 | 🟢 |
| `org.cneko.toneko.common.mod.genetics.api` | 遗传系统 API | 🟡（详见 genetics_api.md） |
| `org.cneko.toneko.common.mod.ai.provider` | AI 服务商接口与注册表 | 🟡 |
| `org.cneko.toneko.common.mod.ai.actions` / `PromptRegistry` | AI 动作 / 提示词扩展 | 🟡 |
| `org.cneko.toneko.common.util.AIUtil` | AI 对话唯一入口 | 🟢 |
| `org.cneko.toneko.common.mod.misc.Messaging` / `ToNekoAttributes` | 消息显示 / 自定义属性 | 🟢 |
| `org.cneko.toneko.common.mod.util` / `common.api` | 工具类 / 权限常量 | 🟢 |
| `org.cneko.toneko.common.mod.client.*` | 客户端类（服务端代码**不可引用**） | 🟡 |
| `org.cneko.toneko.common.mod.events.Common*`、`packets`、`mixin` | 内部实现 | 🔴 |

AI 相关类型（`AIResponse`、`AIRequest` 等）来自 toNeko 内置的 **NekoAI** 库（`org.cneko.ai.*`，已打包进 jar），直接使用即可。

## 2. 依赖声明与探测

### 2.1 硬依赖

`fabric.mod.json`：

```json
{
  "depends": { "toneko": ">=1.9.5" }
}
```

`neoforge.mods.toml`：

```toml
[[dependencies.yourmod]]
modId = "toneko"
type = "required"
versionRange = "[1.9.5,)"
ordering = "NONE"
side = "BOTH"
```

NeoForge 端 toNeko 自身依赖 FFAPI（`fabric_api`），安装器会自动处理，你无需声明。

### 2.2 软依赖探测

```java
// Fabric 模块内
if (FabricLoader.getInstance().isModLoaded("toneko")) { /* ... */ }

// NeoForge 模块内
if (ModList.get().isLoaded("toneko")) { /* ... */ }

// 平台无关的兜底（在你自己 mod 的代码里使用）：
try {
    Class.forName("org.cneko.toneko.common.mod.entities.INeko");
    // toNeko 存在，走联动逻辑
} catch (ClassNotFoundException ignored) {
    // toNeko 不存在，走降级逻辑
}
```

> 注意：上面「common 禁用 FabricLoader」是 toNeko 自己的约束；你作为第三方，在自己的 fabric 模块里可以正常用 `FabricLoader`。

### 2.3 调用时机

- 读数据类 API（`INeko`、`AIUtil`、`Messaging`）随时可用。
- **注册类 API**（Provider / 怪癖 / 基因 / 名字 / 皮肤 / 等级因子）推荐在**服务端启动完成后**调用（Fabric 端 `ServerLifecycleEvents.SERVER_STARTED`；NeoForge 端对应事件），此时 toNeko 的初始化（`ModBootstrap.bootstrap()`）已结束。
- `AIServiceProviderRegistry.register` 对**重复 provider id 抛 `IllegalStateException`**（id 不区分大小写），请先 `hasProvider(id)` 判断或捕获异常。

## 3. 猫娘数据读写（🟢 核心）

### 3.1 判断「是不是猫娘」

```java
// NPC 猫娘（所有变体：冒险家/水晶/幽灵/战斗/女仆/Boss 都继承 NekoEntity）
if (entity instanceof NekoEntity neko) { /* ... */ }

// 玩家猫娘（PlayerEntityMixin 让玩家实现 INeko，未变猫娘的玩家 isNeko() 为 false）
if (player instanceof INeko neko && neko.isNeko()) { /* ... */ }

// 实体有没有实现猫娘数据接口（含未变猫娘的玩家）
if (entity instanceof INeko neko) { /* ... */ }
```

### 3.2 INeko 接口（核心入口，全默认方法）

接口在 `org.cneko.toneko.common.mod.entities.INeko`，所有方法都有默认实现，可安全直接调用：

**身份**：`isNeko()` / `setNeko(boolean)` / `isPlayer()` / `getEntity()`（返回 `LivingEntity`）/ `getAIStorageId()`（稳定的 AI 存储 id，**调 AI 时用它而不是 UUID**）。

**等级与能力**：`getNekoLevel()`（= `NekoLevelRegistry.computeTotal(this)`）/ `getNekoAbility()`（= 等级 × `neko.degree` 属性）/ `getNekoLevelFactorRaw(String)` / `setNekoLevelFactorRaw(String, double)` / `getNekoLevelFactorData()`。

**能量**：`getNekoEnergy()` / `setNekoEnergy(float)` / `getMaxNekoEnergy()`（属性值）/ `increaseEnergy()`。

**主人系统**：`getOwners()`（`Map<UUID, Owner>`）/ `getOwner(uuid)` / `addOwner(uuid, Owner)` / `addOwnerIfNotExist(uuid)` / `removeOwner(uuid)` / `hasOwner(uuid)` / `getXpWithOwner(uuid)` / `setXpWithOwner(uuid, xp)`。`Owner` 是嵌套 record：`Owner(List<String> aliases, int xp)`。

**昵称与年龄**：`getNickName()` / `setNickName(String)` / `getNekoAge()` / `setNekoAge(int)` / `isNekoBaby()` / `setNekoBaby(boolean)` / `getMaxAge()`。

**怪癖**：`getQuirks()`（`List<Quirk>`）/ `addQuirk(Quirk)` / `hasQuirk(Quirk)` / `removeQuirk(Quirk)` / `fixQuirks()`（剔除已注销的怪癖）。

**屏蔽词**：`getBlockedWords()`（`List<BlockedWord>`）/ `addBlockedWord(BlockedWord)` / `removeBlockedWord(String)`。`BlockedWord` 是嵌套 record：`BlockedWord(String block, String replace, BlockMethod method)`。

**已访问群系**：`getVisitedBiomes()`（`Set<String>`，群系 key）/ `addVisitedBiome(String)`。

**潜行**：`isStealthActive()` / `setStealthActive(boolean)` / `breakStealth()`。

**发送消息**（自动套用口癖/前缀，见 §5.3）：`sendMessageToTarget(String, Entity)` / `sendMessageToAll(String)`。

**等级修饰符**：`updateNekoLevelModifiers()`（服务端调用；日常维护由 toNeko 的 slowTick 负责）。

### 3.3 NekoEntity 独有访问器

`getSkin()` / `setSkin(String)`、`getMoeTags()` / `setMoeTags(List<String>)` / `getMoeTagsString()`（萌属性）。玩家萌属性通过其他渠道表达，请以 `INeko` 接口为准。

### 3.4 自定义属性

`ToNekoAttributes.NEKO_DEGREE`（猫娘程度）、`ToNekoAttributes.MAX_NEKO_ENERGY`（`Holder<Attribute>`）：

```java
double degree = entity.getAttributeValue(ToNekoAttributes.NEKO_DEGREE);
```

### 3.5 数据同步注意事项

- 服务端对 `INeko` 数据的写入由 toNeko 内部同步（`NekoInfoSyncPayload` 每 20 tick / SynchedEntityData）。
- **你作为调用方只负责服务端数据**；需要客户端即时感知时，用 §5.3 的 `Messaging.sendNekoChat` 或自行发网络包，不要碰 🔴 级别的内部 payload。
- 修改 NekoEntity 的基因后必须调用 `expressTraits()`（仅服务端）。

## 4. 注册表扩展（向 toNeko 注册你自己的内容）

### 4.1 猫娘名字 / 皮肤（🟢）

```java
// 注册随机名（NPC 猫娘取名池）
NekoNameRegistry.register("小白");
NekoNameRegistry.register(List.of("咪咪", "团子"));

// 注册皮肤：皮肤 PNG 必须位于 toneko 命名空间 assets/toneko/textures/neko/<skin>.png
// （可通过资源包提供；文件名不带 .png 后缀）
NekoSkinRegistry.register(ToNekoEntities.ADVENTURER_NEKO, "my_custom_skin");
NekoSkinRegistry.register(ToNekoEntities.ADVENTURER_NEKO, List.of("skin_a", "skin_b"));
```

实体类型常量在 `ToNekoEntities`（如 `ADVENTURER_NEKO`、`CRYSTAL_NEKO`、`GHOST_NEKO`、`FIGHTING_NEKO`、`MOUFLET_NEKO_BOSS`、`NOELLE_MAID_NEKO`）。

### 4.2 怪癖（🟢）

继承 `Quirk` 基类并实现 `ModQuirk` 接口（全部钩子均为 default 方法，按需覆写）：

```java
public class MyQuirk extends Quirk implements ModQuirk {
    public MyQuirk() { super("my_quirk"); }  // id：小写 snake_case

    @Override
    public InteractionResult onNekoInteraction(Player owner, Level world, InteractionHand hand,
                                               INeko neko, EntityHitResult hitResult) {
        // 主人与猫娘互动时触发；返回 SUCCESS 将拦截后续处理并结算 XP
        return InteractionResult.PASS;
    }

    @Override
    public void onDamage(INeko neko, DamageSource source, float amount) { }
    @Override
    public void onJoin(INeko neko) { }
}
```

可用钩子（`ModQuirk`）：`onNekoInteraction` / `onInteractionOther` / `onDamage` / `onJoin` / `onNekoAttack` / `onWeatherChange` / `startSleep` / `stopSleep` / `getTooltip` / `getInteractionValue`。

```java
// 注册（推荐在 SERVER_STARTED 之后，见 §2.3）
QuirkRegister.register(new MyQuirk());
```

已注册的怪癖可通过 `QuirkRegister.getById("my_quirk")` 取回；`INeko.fixQuirks()` 会按注册表清理失效怪癖。

### 4.3 等级因子（🟢）

实现 `NekoLevelFactor`（`getId()` / `getLevel(double raw)` / 可选 `getDefaultRawValue()`、`addRaw`、`setRaw`）：

```java
public class MyLevelFactor implements NekoLevelFactor {
    @Override public String getId() { return "my_factor"; }
    @Override public double getLevel(double raw) { return raw; }
}
// 注册
NekoLevelRegistry.register(new MyLevelFactor());
```

之后任意猫娘都可以 `neko.setNekoLevelFactorRaw("my_factor", 10)`；`neko.getNekoLevel()`（= `computeTotal`）自动汇总所有因子。

### 4.4 遗传系统（🟡，完整文档见 genetics_api.md）

**代码方式**（三件套）：

```java
// 1) 等位基因：显性值语义 5 隐性 / 10 野生 / 15 半显性 / 20 显性
Allele myAllele = new Allele(ResourceLocation.fromNamespaceAndPath("yourmod", "my_allele"),
        10,                                    // dominance
        (entity, geneticData) -> { /* 表达时回调，可写萌属性/NBT */ },
        (entity, geneticData) -> { /* 移除时回调 */ })
        .addAttributeModifier(Attributes.MOVEMENT_SPEED, "my_modifier", 0.1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        .addAIGoal(3, mob -> new MyGoal(mob)); // 注入 AI Goal（自动装卸）

// 2) 注册等位基因与基因座
GeneticsRegistry.registerAllele(myAllele);
GeneticsRegistry.registerLocus(new Locus(ResourceLocation.fromNamespaceAndPath("yourmod", "my_locus")));

// 3) 野生池权重（自然生成时按权重轮盘抽样；不加入则野生生物不携带）
GeneticsRegistry.addWildAllele(ResourceLocation.fromNamespaceAndPath("yourmod", "my_locus"),
        ResourceLocation.fromNamespaceAndPath("yourmod", "my_allele"), 10);
```

核型：`registerKaryotype(ResourceLocation id, Class<? extends LivingEntity>, SpeciesKaryotype)` —— toNeko 已把基础核型注册给 `Mob.class`，**原版生物同样拥有基因槽**。

**数据包方式（免代码）**：把 `data/<你的命名空间>/toneko_genetics/` 下的 alleles / loci / karyotypes 三个 JSON（结构见 genetics_api.md）随资源/数据包分发，`/reload` 自动加载，无需任何 Java 代码。

**操作实体基因**：`entity instanceof IGeneticEntity g` 后 `g.getGenome()` / `g.setGenome(...)` / `g.expressTraits()`（**改基因后必须调，且仅服务端**）。

### 4.5 AI 服务商 Provider（🟡）

实现 `AIServiceProvider` 接口并注册；**OpenAI 兼容协议**的服务商推荐复用内置工具 `OpenAIProvider.applyCommon(OpenAIConfig, AIServiceConfig, String defaultHost)`（参考 `provider/impl/OpenAIProvider.java`）：

```java
public class MyProvider implements AIServiceProvider {
    @Override public String getProviderId() { return "my_llm"; }   // 小写，配置 ai.service 用
    @Override public String getDisplayName() { return "My LLM"; }
    @Override public boolean isOpenAICompatible() { return true; }
    @Override public boolean requiresApiKey() { return true; }
    @Override public String getDefaultHost() { return "api.example.com"; }
    @Override public int getDefaultPort() { return 443; }
    @Override public String getDefaultEndpoint() { return "/v1/chat/completions"; }
    @Override public boolean isDefaultTls() { return true; }
    @Override public String getDefaultModel() { return "my-model"; }
    @Override public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        /* 使用 config 的 apiKey/model/host/port/endpoint/tls/proxy 发起请求，返回 AIResponse */
    }
    // 可选：支持流式时覆写 supportsStream() 与 processStream()；不覆写则自动回退"单元素流"
}
```

```java
// 注册（重复 id 抛 IllegalStateException）
if (!AIServiceProviderRegistry.hasProvider("my_llm")) {
    AIServiceProviderRegistry.register(new MyProvider());
}
```

注册后用户需在配置 `ai.service` 填入 `my_llm` 才会被使用；相关配置支持每 provider 独立节（`ai.providers.<id>.*`）。

### 4.6 AI 动作（🟡）

`NekoActionExecutor.register(type, NekoActionHandler)` 注册模型可调用的动作（function-calling 风格）：

```java
NekoActionExecutor.register("hug", new NekoActionExecutor.NekoActionHandler() {
    @Override
    public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
        // 在服务端主线程执行；返回是否执行成功
        neko.sendMessageToTarget("（抱住了%s）".formatted(target.getName().getString()), speaker);
        return true;
    }
    @Override
    public String getGuideLine() {
        // 注入 prompt 的动作说明（JSON 示例 + 说明）；建议用 LanguageUtil.translatable 支持多语言
        return "{\"action\":\"hug\",\"target\":\"目标名\"} 表示抱住目标；";
    }
});
```

`NekoAction` 是 record：`NekoAction(String type, String item, int count, String target, String text)`。

### 4.7 提示词片段（🟡）

`PromptRegistry.register(key, PromptFactory)`；`PromptFactory` 是函数式接口 `String getPrompt(NekoEntity neko, INeko other)`。`%key%` 占位符在生成提示词时被单次扫描替换（参考 `Prompts` 类中内置 22 个片段的注册范例）。

## 5. AI 对话集成（🟢）

### 5.1 发送消息

**唯一入口是 `AIUtil`**（不要绕过它直接调 provider——冷却、历史、上下文注入都由它处理）：

```java
// 非流式
AIUtil.sendMessage(
        neko.getAIStorageId(),        // 猫娘存储 id（用 getAIStorageId()，不要用实体 UUID）
        player.getUUID(),             // 说话玩家
        prompt,                       // 附加提示词（可为 null）
        "你好呀~",                    // 消息正文
        (AIResponse response) -> {    // MessageCallback：在 AI 工作线程回调
            String text = response.getContent();
            ModMeta.INSTANCE.getServer().execute(() -> {   // MC 操作必须切回主线程
                Messaging.sendNekoChat(player, neko, text); // 气泡/聊天栏显示（按客户端配置）
            });
        });

// 流式（SSE 打字机）
AIUtil.sendMessageStream(
        neko.getAIStorageId(), player.getUUID(), prompt, "你好呀~",
        new AIUtil.StreamCallback() {
            @Override public void onChunk(String chunk) {
                // 干净文本增量（动作 JSON 已剥离，可直接展示；可能为空串），逐块推给客户端即可
            }
            @Override public void onFinished(AIResponse full) {
                // 完整响应（含 think）；动作解析与历史保存基于此
            }
            @Override public void onError(AIResponse error) {
                // 冷却/连接/流中断/超时等，error 与错误 AIResponse 同构
            }
        });
```

重载参数：`(nekoStorageId, userUuid, prompt, message, callback[, ignoreCooldown, historyPrefix])`——`ignoreCooldown=true` 跳过玩家级冷却（配置 `ai.cooldown`）；`historyPrefix` 自定义历史前缀，null 用默认 `[说话人]` 前缀。

### 5.2 回调线程纪律（重要）

- 回调在 **AI 工作线程**执行；一切世界/实体/网络操作必须 `ModMeta.INSTANCE.getServer().execute(...)`（或 `player.getServer().execute(...)`）切回主线程。
- 发网络包必须 try-catch（玩家可能已断线）。
- 非流式 provider 调用 `sendMessageStream` 会自动回退为"单元素流"（一个 chunk + 立即完成），行为与一次性显示一致。

### 5.3 显示与 TTS

```java
// 猫娘气泡/聊天栏（客户端按 config 选择模式；在范围内玩家可见）
Messaging.sendNekoChat(player, neko, text);                    // 单玩家
Messaging.sendNekoChatInRange(entity, neko, text, 64.0);       // 范围内广播

// 套用口癖/前缀的纯文本变换（不带显示）
String formatted = Messaging.formatMessage(text, neko);
Component hover = Messaging.createComponentWithHover(formatted, neko);

// 猫娘主动说话（INeko 默认方法，等价 modifyAndSendMessageToAll）
neko.sendMessageToAll("喵~");

// TTS（仅 Player2 服务商支持，见 AI 设置文档）
AIUtil.playTTS(text, voice);

// 清理模型回复里的角色前缀（如 "[猫娘]你好"）
AIUtil.cleanReplyPrefixes(text);
```

## 6. 事件与交互钩子（🟢）

### 6.1 聊天事件

```java
// 为猫娘聊天添加前缀
ChatEvents.CREATE_CHAT_PREFIXES.register((INeko sender, List<String> prefixes) -> {
    prefixes.add("[联动]");
});

// 修改聊天最终文本（返回值替换消息）
ChatEvents.ON_CHAT_FORMAT.register((message, sender, prefixes, chatFormat) ->
        message.replace("喵", "喵~"));
```

### 6.2 天气事件

```java
WorldEvents.ON_WEATHER_CHANGE.register((ServerLevel world, int clearTime, int weatherTime,
                                        boolean isRaining, boolean isThundering) -> { /* ... */ });
```

> 已知缺陷：当前实现里 `isThundering` 参数实际传入的是 `isRaining`（实现 bug），使用前请自行 `world.isThundering()` 兜底。

### 6.3 玩家-猫娘互动

**互动扩展的正规途径是注册怪癖（§4.2）**——互动触发逻辑集中在 toNeko 内部事件类（🔴），不建议直接 hook。怪癖钩子覆盖：互动（`onNekoInteraction`/`onInteractionOther`）、伤害、攻击、睡觉、天气、进服。

### 6.4 实体交互屏幕（🟡，客户端）

```java
// 在客户端为指定实体注册自定义交互屏（构造器用 NekoScreenBuilder，可 clone）
NekoScreenRegistry.register(ToNekoEntities.ADVENTURER_NEKO_ID,
        new NekoScreenBuilder()
                .setStartY(20)
                .addButton(new MyButtonFactory())
                .addTooltip(new MyTooltipFactory()));
```

屏幕构建器链式 API：`setStartY` / `addButton` / `addTooltip` / `addWidget`（支持插入位置）/ `clone`。绑定实体的屏幕实现 `INekoScreen`（`NekoEntity getNeko()`）。

### 6.5 延迟任务队列

```java
// 服务端延迟任务（tick 为单位，主线程执行）
TickTasks.add(new ITickable() {
    int ticksLeft = 100;
    @Override public void tick(int tickCount) {
        if (--ticksLeft <= 0) TickTasks.remove(this);
    }
});
// 客户端用 TickTasks.addClient(...)；也可用 mod/util/TickTaskQueue（addTask(delayTicks, runnable)）
```

## 7. 平台边界

- **Fabric 与 NeoForge 完全一致**：以上 API 全部位于 common 模块，经 FFAPI 桥接，两端用法相同；你的联动代码写一份即可。
- **客户端类**（`common.mod.client.*`：屏幕、渲染器、`NekoScreenRegistry` 等）只能在客户端入口/`@Environment(CLIENT)` 代码中引用，服务端专用代码不得触碰。
- **Trinkets 联动仅 Fabric 端**存在（slot `legs/socks`）；NeoForge 端丝袜为普通 ArmorItem。
- **Bukkit 插件版不提供本 API**（不共享这些 MC 绑定类型）。
- 线程纪律与主线程约束同 toNeko 内部规范：`Level` 查询只允许主线程；AI 回调在后台线程（§5.2）。

## 8. 稳定性分级汇总

| API | 级别 | 说明 |
|---|---|---|
| `INeko`、`NekoEntity`、`Owner`/`BlockedWord` | 🟢 | 接口以 default 方法扩展，兼容性优先 |
| `NekoNameRegistry` / `NekoSkinRegistry` / `NekoLevelRegistry` / `QuirkRegister` / `ModQuirk` | 🟢 | 静态注册表，注册即生效 |
| `AIUtil` / `Messaging` / `ToNekoAttributes` / `ChatEvents` / `WorldEvents` | 🟢 | 核心调用路径 |
| `mod/util/*`、`common/api/Permissions`、`TickTasks` | 🟢 | 纯工具，无副作用 |
| `genetics.api.*` | 🟡 | 功能稳定，签名仍可能微调；文档以 genetics_api.md 为准 |
| `AIServiceProvider(Registry)` / `NekoActionExecutor` / `PromptRegistry` | 🟡 | AI 层快速迭代中 |
| `NekoScreenRegistry` / `NekoScreenBuilder` | 🟡 | 客户端 UI 构建器 |
| `Common*Event`、`packets/*`、`mixin/*`、`quirks` 内部实现 | 🔴 | 内部实现，随时可能重构；依赖它们 = 自行承担破损风险 |

## 9. 给 toNeko 维护者的改进建议

现状问题（按优先级）：

1. **NickName 加载 bug**：`INeko.loadNekoNBTData` 中 `setNickName(this.getNickName())` 应为 `setNickName(nbt.getString("NickName"))`，否则昵称读取恒等于自身。
2. **`WorldEvents.ON_WEATHER_CHANGE` 的 `isThundering` 参数**实际传入 `isRaining`（实现 bug，见 §6.2）。
3. **皮肤命名空间限制**：`NekoRenderer.getTextureResource` 硬编码 `toneko` 命名空间，第三方皮肤必须注入 `assets/toneko/`；建议支持任意命名空间（`NekoSkinRegistry` 增加 `ResourceLocation` 键）。
4. **缺少官方门面**：建议新增 `ToNekoAPI` 门面类，聚合 `isNeko(Entity)` 静态助手、`getNeko(Entity)` 等高频操作，避免第三方在 `mod.api` 与 `mod.entities` 之间迷路。
5. **稳定性标注**：对公开类补 `@ApiStatus.Internal` / `@ApiStatus.Experimental` 注解与 Javadoc（`INeko` 与 genetics 已有部分 Javadoc，其余注册表没有）。
6. **`AIServiceProviderRegistry` 注册时序**：第三方在 toNeko 初始化前注册会被内置 `init()` 之后的重复检查拒绝，或需文档化"必须在 SERVER_STARTED 后注册"（目前靠 §2.3 约定）。
7. **客户端事件面**：`ChatEvents` 仅有服务端语义，若未来需要客户端事件（如气泡渲染），建议同样走 `mod.api.events` 而不是暴露内部类。

## 10. 完整示例：一个联动模组的入口骨架

```java
public class MyAddon {
    public static void init() {
        // 1) 探测 toNeko（软依赖）
        if (!FabricLoader.getInstance().isModLoaded("toneko")) return;   // fabric 模块
        // if (!ModList.get().isLoaded("toneko")) return;                // neoforge 模块

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // 2) 注册自己的内容
            QuirkRegister.register(new MyQuirk());
            NekoLevelRegistry.register(new MyLevelFactor());
            NekoSkinRegistry.register(ToNekoEntities.ADVENTURER_NEKO, "my_skin");
            if (!AIServiceProviderRegistry.hasProvider("my_llm")) {
                AIServiceProviderRegistry.register(new MyProvider());
            }
            NekoActionExecutor.register("hug", new MyHugAction());
        });
    }

    /** 任意位置：让猫娘回话 */
    public static void askNeko(ServerPlayer player, NekoEntity neko, String msg) {
        AIUtil.sendMessageStream(neko.getAIStorageId(), player.getUUID(), null, msg,
                new AIUtil.StreamCallback() {
                    @Override public void onChunk(String chunk) { /* 推打字机增量 */ }
                    @Override public void onFinished(AIResponse full) {
                        ModMeta.INSTANCE.getServer().execute(() ->
                                Messaging.sendNekoChat(player, neko, full.getContent()));
                    }
                    @Override public void onError(AIResponse error) { }
                });
    }
}
```
