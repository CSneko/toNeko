# 安全与工程卫生横切扫描审计报告（toNeko）

> 审计人：ox-alpha（安全与工程卫生领域）
> 范围：common / fabric / neoforge / bukkit 四源码模块 + 构建脚本 + git 索引 + 开发规范.md

## 审计范围与方法

- **源码规模**：4 个源码模块共 **445 个 .java 文件 / 54,006 行**（common 379 / fabric 25 / neoforge 16 / bukkit 25）；另有 `docs/deprecated/` 下 16 个已废弃源码副本。
- **git 索引**：`git ls-files` 共追踪 1,083 个文件，逐一核对构建产物与二进制。
- **全仓库 grep 扫描**：`System.out/printStackTrace`、空 catch 块、`apiKey/getAIKey` 全部打印点、HTTP 客户端（JDK HttpClient / HttpURLConnection）、`new File/Paths.get/FileUtil.WriteFile` 的路径拼接、lang 键数统计（python 解析 JSON 计数）。
- **精读抽查**：AIUtil.java(804行)、ConfigUtil.java(848行)、HttpClient.java、HttpBase.java、HttpGet.java、Stats.java、FileUtil.java×2、JsonConfiguration.java、LanguageUtil.java、FabricLanguageImpl.java、PermissionUtil.java、NekoActionExecutor.java(giveItem/parseItem)、ToNekoNetworkEvents.java(C2S 校验)、bukkit ChatEvent/PayloadSender/NetworkingEvents、Player2Auth/TTSUtil、gal 模块(GalParser/GalSoundInstance/TestGalCommand)、Metrics.java(bStats)、build.sh/.gitignore/.github/workflows。

## 总体评价

**6 / 10。**
没有发现"远程可利用、可致崩溃/数据丢失"级别的安全漏洞：存储路径全部 UUID 化、无 SQL/命令注入面、聊天格式化用 `replace` 不用 `String.format`、C2S 包有权限+距离校验链。但仓库存在三类实质问题：① AI 动作系统对模型输出的物品数量/种类完全不设防；② 语言回退逻辑有一个真 bug，叠加 ko_kr 仅 18% 覆盖率导致非中英用户大面积看到原始键名；③ 工程卫生与《开发规范.md》自我声明严重脱节——规范 §17 明令"禁止提交构建产物"，而 git 索引里躺着 48 个 bukkit/build 文件和约 16.3MB 二进制。

## 优点

1. **API key 日志脱敏有意识**：debug 模式打印请求时 key 只显示前 8 字符 + `***`（`AIUtil.java:314-315`、`:673-675`），未发现完整明文 key 进入日志的路径。
2. **C2S 安全校验链真实落地**：基因编辑包先查权限（手持 GeneEditor 或 op/luckperms），再校验实体存在且距离 <64 格（`ToNekoNetworkEvents.java:80-95`）；交互类包统一走 `processNekoInteractive` 的距离 ≤8 格校验（`:623-634`），与开发规范 §10.2 一致。
3. **路径穿越面基本封死**：AI 历史按随机 UUID 落盘（`NekoEntity.getAIStorageId()` `NekoEntity.java:1788-1796` 首次生成 UUID.randomUUID 并 NBT 持久化）、猫娘数据按玩家 UUID 命名（`NekoQuery.java:125`），玩家可控字符串均不进入文件路径。
4. **无格式化字符串注入**：聊天格式化用链式 `.replace("%name%",…)` 而非 `String.format`（`Messaging.java:96-110`），昵称含 `%s%n` 不会破坏输出。
5. **配置解析容错性好**：`JsonConfiguration(Path)` 强制 UTF-8、坏 JSON 回退为 `{}` 不崩启动（`JsonConfiguration.java:43-67`），读方法全部 try-catch 给默认值并加锁（`:189-234`）。
6. **外部调用超时意识**：Player2 设备授权流程每个阻塞点都带 `.get(5s/30s)` 上限（`Player2Auth.java:70-73,107-124`），device flow 有总 deadline 和 `compareAndSet` 防重入（`:104-106`）。
7. **权限层 fail-closed**：`PermissionUtil.has()` 异常时返回 false 而非 true（`PermissionUtil.java:66-76,84-96`），luckperms 缺失时按前缀映射 op 等级兜底（`:78-83`）。
8. **规范文档自我认知清晰**：《开发规范.md》§17 明确禁止提交构建产物、§10.2 定义 C2S 校验五步法、§18 记录 13 条已知坑——问题在于执行没跟上（见下）。

## 问题

### [较高] 1. AI give_item 动作：物品数量不限 + 物品种类不限 + 能量成本与数量无关
- **位置**：`common/src/main/java/org/cneko/toneko/common/mod/ai/actions/NekoActionExecutor.java:483`（调用）、`:916-943`（giveItem）、`:971-976`（parseItem）
- **描述**：`giveItem(neko, target, action.item(), action.count())` 直接把 LLM 输出的 count 传入 `new ItemStack(item, count)`（`:939`），无上限/下限钳制（对比 `take_item` 至少做了 `Math.max(1,…)`，`:205`）。虚拟生成分支只扣固定能量 `cost = Math.max(1, getAIActionsEnergyCost())`（默认 10），与 count 无关。`parseItem` 解析**任意注册物品**（`:971-976`），包括命令方块、屏障等创造专属物品，无白名单。
- **影响**：玩家通过话术诱导猫娘输出 `{"action":"give_item","item":"minecraft:diamond_block","count":576}` 即可以一次动作 10 点能量的代价批量获取任意物品（需 `ai.actions.virtual_items=true` 且 LLM 配合，但 prompt 本身就鼓励输出动作 JSON）。这是本仓库最接近"真实可利用"的问题——游戏经济作弊向量而非服务器 RCE。
- **建议**：count 钳制到 `[1, min(64, maxStackSize)]`；能量成本按 `ceil(count/stackSize)` 缩放；parseItem 增加 allowlist 或排除 `ItemCreativity`/creative-only 物品。

### [较高] 2. 语言回退条件写错：缺失键直接给玩家看原始翻译键
- **位置**：`common/src/main/java/org/cneko/toneko/common/util/LanguageUtil.java:24-29`
```java
if(LANG.contains(key)){ return LANG.getString(key); }
else if(EN_US_LANG != null && !language.equals("en_us") && LANG.contains(key)){  // ← 应为 EN_US_LANG.contains(key)
    return EN_US_LANG.getString(key);
}
return key;
```
- **描述**：else-if 分支重新检查的是已经为 false 的 `LANG.contains(key)`，英文回退永远走不到，直接返回 key 本身。同文件接口默认方法（`:41`）写的是正确的 `EN_US_LANG.contains(key)`，两条路径行为不一致。
- **影响**：叠加问题 3 的覆盖率数据——ko_kr 缺 1,681 键、ja_jp 缺 643 键——这些用户会在成就/界面/AI 提示里看到 `advancements.toneko.catnip.title` 这类裸键名。静态入口被 58 处显式调用（含 bukkit 的 `Language.get`），是主路径之一。
- **建议**：改为 `EN_US_LANG.contains(key)`；补一个"key 必须命中 en_us"的单测即可锁死。

### [较高] 3. ja_jp 语言包发布但从未加载；ko_kr 覆盖率仅 18.2%，违反自家规范
- **位置**：`common/src/main/java/org/cneko/toneko/common/mod/impl/FabricLanguageImpl.java:17`、`common/src/main/resources/assets/toneko/lang/*.json`
- **描述**：`List.of("en_us","zh_cn","zh_tw","ko_kr")` 不含 ja_jp，但 jar 里打包了 ja_jp.json（1,411 键）；用户把 language 配成 ja_jp 后 `LANG = fromFile(不存在的文件)` 得到空对象，配合问题 2 → 全部文本变裸键。实测各语言键数（python 解析）：en_us **2049** / zh_cn **2043**（缺 7）/ zh_tw **1565**（76.4%）/ ja_jp **1411**（68.9%）/ ko_kr **373**（**18.2%**）。开发规范 §14 要求"en_us 与 zh_cn 必须全量、新增文本一次性同步 5 个文件"，zh_cn 也缺 7 键。
- **建议**：要么把 ja_jp 加入复制列表并修覆盖率，要么从 resources 删除 ja_jp.json 并在配置注释声明不支持；给 CI 加 lang 键数 diff 检查。

### [较高→工程] 4. 构建产物与第三方 jar 大量入库，.gitignore 存在系统性漏洞，违反自家规范 §17
- **证据**：
```bash
$ git ls-files | grep -c "^bukkit/build/"        # → 48
$ git ls-files | grep -cE "\.class$"             # → 38（bukkit/build/classes/...）
$ git ls-files | grep -E "\.jar$"
# bukkit/build/libs/toneko-bukkit-1.9.3.jar   (2,150,265 B)
# bukkit/libs/paper-1.21.jar                  (10,563,408 B)
# result/toneko-fabric-1.9.5.jar              (3,586,055 B)
```
  连 Gradle 增量编译的事务残留都进了库：`bukkit/build/tmp/compileJava/compileTransaction/stash-dir/*.class.uniqueId0..3`、`previous-compilation-data.bin`。
- **根因**：`.gitignore` 只写了 `/build/`、`/common/build/`、`/neoforge/build/`、`/fabric/build/`，**没有 `/bukkit/build/`**，也没有 `result/`；《开发规范.md》§17 白纸黑字"禁止提交构建产物：*.class、build/、stash-dir/ 等（历史上出现过）"。另有一张 UUID 命名的截图 `textures/gui/f7648f72-….png` 被 `git add` 后又从工作区删除，仍挂在索引里（`git status` 显示 AD）。
- **影响**：仓库膨胀约 16.3MB 可移除二进制；paper-1.21.jar 入库还有许可证层面的隐患；每次 clone 全员付费。
- **建议**：`.gitignore` 追加 `/bukkit/build/`、`/bukkit/run/`、`result/`、`*.jar` 白名单例外（保留 gradle-wrapper.jar）；`git rm -r --cached bukkit/build result bukkit/libs/paper-1.21.jar`；paper 依赖改用本地 maven/repo 声明。

### [中] 5. API key 明文存 JSON 配置；debug 日志两处泄露 key 片段
- **位置**：`ConfigUtil.java:39`（`ai.key` 明文入 config/toneko.json，双轨制还复制到 `ai.providers.<id>.key`，`:592-597`）；`ConfigUtil.java:697-702`
- **描述**：MC mod 配置明文存 key 属行业惯例，不算漏洞，但要指出两点放大因素：① `ai.debug=true` 时 `buildAIServiceConfig` 会把 key **前 4 + 后 4 字符**转 hex 打进日志（`"[AI-DEBUG] Key hex sample (first 4 + last 4)"`），比 AIUtil 里"前 8 字符+***"的脱敏更激进，两个口径不一致；② `AIConfigScreen.java:96-97` 把完整 key 回显在屏幕输入框。日志文件通常被服主随意分享，后 4 字符 + 前 4 字符足以支撑社会工程攻击。
- **建议**：统一脱敏口径（建议只留前 4）；文档标注 debug 开关会降低密钥保密性。

### [中] 6. HTTP 客户端普遍缺请求级超时
- **位置**：
  - `HttpClient.java:28-30`：只有 `connectTimeout(10s)`，`HttpRequest` 未设 `.timeout(...)`（`:44-51,78-84`），慢响应可无限挂起 future；
  - `HttpBase.java:53-88`、`HttpGet.java:22-49,104-130`：HttpURLConnection 完全没有 `setConnectTimeout/setReadTimeout`；
  - 对比正面案例：Player2Auth 用 `future.get(5, TimeUnit.SECONDS)` 在调用侧补救（`Player2Auth.java:72`）。
- **影响**：Stats/AI 心跳等异步线程可能长期堆积；`HttpGet.getFile` 若被复用会占住线程。当前调用方多为 fire-and-forget，故定级[中]。
- **附带**：`new HttpClient()` 每次新建共 8 处（grep 计数），JDK HttpClient 实例内部持有连接池/线程，应静态复用单个实例。
- **建议**：HttpClient 统一加 `.timeout(Duration.ofSeconds(60))`（与 AIUtil.REQUEST_TIMEOUT=60 对齐）；legacy HttpGet/HttpBase 标记 @Deprecated 或补 timeout 后收敛到新客户端。

### [中] 7. 空 catch 吞异常 43 处（占全部 catch 块的 24%），关键 IO 失败静默
- **证据**：正则 `catch\s*\([^)]*\)\s*\{\s*\}` 全仓匹配 **43 处 / catch 总数 181**；典型分布：NekoQuery ×5、bukkit PayloadSender ×5、FileUtil ×4、fabric 渲染器 ×7、Stats ×2。
- **代表位置**：`FileUtil.java:45-57` `WriteFile` 整个方法体包在 `catch(Exception ignored)` 里且 `FileWriter` 未用 try-with-resources（write 抛异常则句柄泄漏、写入失败无任何信号——这是存玩家数据/配置的路径，属数据丢失风险面）；`PayloadSender.java` 所有网络发送失败静默（可接受但应至少 debug 级日志）。
- **建议**：IO 写路径必须记 warn；渲染器/网络发送的吞异常至少加注释说明为什么安全。

### [中] 8. Stats 遥测 URL 手工拼接：未编码参数 + `&&` 分隔符 + 死代码
- **位置**：`common/src/main/java/org/cneko/toneko/common/Stats.java:15,29`
```java
HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/stick/add?neko="+neko+"&&player="+player,null);
```
- **描述**：参数未经 URLEncoder；`&&` 是笔误（多数服务端容忍但属脏数据）。实际风险**低**：目标 host 硬编码（无 SSRF）、玩家名受 MC `[a-zA-Z0-9_]` 限制（`TextUtil.getPlayerName` 取原版名，`TextUtil.java:18-21`），无法构造参数走私；失败静默 IOException。另 `stick(String,String)` 全仓无调用方，是死代码。
- **建议**：删除 stick()；meowInChat 改用 `HttpClient.sendGet` 的 queryParams 重载（自带 URLEncoder，`HttpClient.java:71-73`）。

### [低] 9. checkElefantOnce 探测到本地服务即静默改写配置启用 AI
- **位置**：`AIUtil.java:100-110`
- **描述**：每 60 秒探测 `127.0.0.1:<动态端口>/v1/health`，一旦通就 `CONFIG.set("ai.enable",true); CONFIG.save()`。任何本机进程占用该端口并应答即可让服主的 `ai.enable=false` 配置被翻转（本机攻击面，非远程）。至少应在日志里高亮"已自动启用 AI"，或首次征得确认。

### [低] 10. System.out/printStackTrace 与杂项卫生
- `System.out/err` 共 **4 处**：`HttpBase.java:196`、`HttpGet.java:61`、`HttpGet.java:149`、`GalInfo.java:57`（一个空的 `System.out.println()`）。
- `printStackTrace` 共 **2 处**：`FileUtil.java:116,130`（绕过 LOGGER）。
- `gal/mian.json` 文件名拼写错误 vs `GalParser.GAL_INFO_FILE_NAME="main.json"`（`GalParser.java:15`），该 gal 包的 main.json 永远解析不到；`TestGalCommand.playDialogue` 允许客户端命令打开任意本地路径（纯客户端自担风险）。
- `PermissionUtil.register()` 用 `Permissions.check(uuid, perm)` 冒充权限注册（`PermissionUtil.java:25-29`），`uuid` 来自 `Permissions.java:7` 硬编码的固定 UUID——整段 registerAll 是误导性 no-op。
- `NekoQuery.DATA_PATH` 硬编码 `"ctlib/toneko/neko_data"`（`NekoQuery.java:23`），bukkit 端其余数据在 `plugins/toNeko/`（`ToNeko.java:36-38`），同一插件数据分裂两处。
- CI 每次 `sh build.sh` 固定 sleep 60 再 kill -9 datagen（`build.sh:3-7`），白白拖慢构建。

### [低] 11. AI 共享会话文件的并发读改写无锁
- **位置**：`AIUtil.java:448-449,508-509`
- **描述**：所有玩家共享 `SESSION_ID="shared"`，同一猫娘被两名玩家同时对话时，100 线程池（`AIUtil.java:31`）里两个任务对同一 `shared.json` 读-改-写，无文件锁/串行化，可丢失对话历史；`Files.writeString` 非原子，崩溃瞬间可能截断文件（读取端 Gson 容错会退化成空历史）。触发窗口小，定级[低]，但随并发增长值得加 per-file 锁。

## 数据速览

- 扫描源码：**445 文件 / 54,006 行**（不含 docs/deprecated 16 个废弃副本）；git 追踪 1,083 文件
- 入库构建产物：**bukkit/build/ 下 48 个文件（38 个 .class）** + 3 个 jar（paper 10.6MB、toneko-bukkit 2.15MB、result 3.59MB）≈ **16.3MB 可移除**；`.gitignore` 缺 `/bukkit/build/` 与 `result/`
- System.out/err：**4 处**；printStackTrace：**2 处**；空 catch 块：**43 / 181（23.8%）**；`catch … ignored` 命名模式 50 处
- lang 键数：en_us **2049** / zh_cn **2043**(-7) / zh_tw **1565**(76.4%) / ja_jp **1411**(68.9%) / ko_kr **373**(**18.2%**)；ja_jp 打包但不在加载列表
- HTTP：JDK HttpClient 封装 1 个（connectTimeout 10s，无 request timeout）；`new HttpClient()` 逐次实例化 **8 处**；裸 `new Thread(` **9 处**；HttpURLConnection 无超时 2 类 3 文件
- 测试：`src/test` 目录 **0** 个，测试文件 **0** 个
- 超 1000 行大类：NekoEntity 1942 / NoelleMaidNekoEntity 1559 / NekoActionExecutor 1081
- API key 流向：明文存 config/toneko.json；日志脱敏 2 种口径（前8+*** 与 首4尾4hex）；未发现完整明文入日志
