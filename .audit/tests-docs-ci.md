# toNeko 审计报告 —— 测试、文档与可维护性基础设施

## 审计范围与方法

实际读取/核查的内容：

| 范围 | 明细 |
|---|---|
| 测试代码 | 全仓库 `find *Test*.java`、`src/test` 目录遍历：仅命中 `common/src/main/java/org/cneko/gal/common/client/TestGalCommand.java`（游戏内调试命令，非单元测试）；`common/src/test`、`fabric/src/test` 存在但为空且未被 git 跟踪 |
| 构建脚本 | `build.gradle`(129 行)、`common/build.gradle`、`fabric/build.gradle`、`settings.gradle`、`gradle.properties`、`build.sh`；grep 全部 gradle 文件确认无 junit/testImplementation/useJUnitPlatform/spotless/checkstyle/pmd/spotbugs |
| 开发规范 | 《开发规范.md》全文 365 行逐节阅读（19 个章节），并抽查其声明与代码的一致性 |
| 文档 | `docs/` 全目录（AI.md 131 行、AI_en.md 138 行、API.md 473 行、API_en.md 477 行、genetics_api.md 732 行、README.md/README_en.md 各 7 行、TONEKO_ONLINE_API.md 23 行，共 1,988 行）+ 根 README.md(121)/README_en.md(155)/README_Bukkit.md(289)；docs/deprecated 下 16 个归档 java 文件清单 |
| 文档准确性抽查 | ①AI 服务商列表对照 `provider/impl/` 12 个实现类；②占位符表对照 `ConfigUtil.java:65`；③配置文件路径对照 `ConfigUtil.java:12`；④基因位点数对照 `ToNekoLocus.java`/`MoeGenetics.java`；⑤API.md §3.2 `Owner` 类型对照 `INeko.java:411`；⑥API.md §5.1 方法签名对照 `AIUtil.java:215-233,583-591`；⑦genetics_api.md 接口对照 `IGeneticEntity.java:7-23` |
| CI | `.github/workflows/build.yml`(56 行)、`releases.yml`(37 行) 全文 |
| 仓库卫生 | `.gitignore`、`git ls-files` 检查被跟踪的 .class/jar/result 产物及体积 |
| 风格漂移抽样 | tab 缩进、CRLF、行尾空白、通配符 import、import 排序抽样、超长文件统计 |
| 关联代码 | genetics api（Locus/Allele/Genome/GeneticsRegistry）、NekoActionParser、ZettaiRyouiki、QuirkRegister、gal 子系统（12 文件/1,302 行）的可测试性评估 |

## 总体评价

**4 / 10**

文档侧明显高于同类项目平均水平：《开发规范.md》是一份少见的、以"现状即规范"为原则且经抽查基本属实的工程宪法，API.md/genetics_api.md 抽查的关键签名与代码一致。但测试侧是绝对零分：约 5 万行主代码没有任何一个单元测试、没有测试依赖、没有测试任务配置，连 `test` 任务本身都被官方文档定性为"绕过"；CI 只做编译打包、无任何验证门禁；无任何格式化/静态分析工具；仓库里还躺着约 14MB 违反自家规范的构建产物。"文档强、验证弱"的失衡使该领域整体不及格。

## 优点

1. **《开发规范.md》质量很高**（365 行、19 章）：条条落到具体文件与行为，含根因分析型陷阱清单（如 `开发规范.md:340` 解释 `:common:test` 任务报错是 Gradle 8.11.1 与新 JDK 的反射问题而非代码问题），并附新功能检查清单（`:348-365`）。抽查其声明基本属实：如 §8.4 的显隐性语义（5/10/15/20）与 `Allele.java:26` 的 `dominance` 字段及 `ToNekoAlleles.java:17` 的 `WILD_TYPE = new Allele(..., 10, ...)` 吻合。
2. **API 文档关键内容准确**：`docs/API.md:292-315` 的 `AIUtil.sendMessage/sendMessageStream` 示例与方法签名、重载参数、回调三阶段语义与 `AIUtil.java:215/223/233/568-574/583/591` 逐一吻合；`docs/genetics_api.md` 的 `IGeneticEntity` 用法与 `IGeneticEntity.java:7-23`（getGenome/getGeneticData/getActiveTraits/expressTraits 等）完全一致。
3. **中英成对机制总体在运转**：AI/AI API/docs README 均有 `_en.md` 对应文件，README 顶部互链并加粗当前语言（`README.md:2`、`README_en.md:2`），符合自定规范 §16。
4. **CI 存在且本地/CI 同一条构建路径**：`build.yml:31` 与 `releases.yml:26` 都执行 `sh build.sh`，JDK 版本钉死 temurin 21（`build.yml:20-23`），release 触发自动构建并把 `result/*` 上传为 release 资产（`releases.yml:33-39`），发布流程自动化程度不错。
5. **废弃代码归档制度落实了**：`docs/deprecated/{common,gal}` 共 16 个 java 文件按原包路径归档（如 `docs/deprecated/gal/client/TestGalCommand.java`），与规范 §16"按原包结构归档"一致，为降级端口复用保留了参照。
6. **代码风格自律性比"无工具"预期的好**：common 379 个 java 文件中 0 个使用 tab 缩进、0 个 CRLF；抽样 import 基本按字典序排列（如 `AIUtil.java:3-14`、`NekoEntity.java:3-14`）；TODO/FIXME 仅 6 处。
7. **LICENSE 明确（GPL-3.0）** 且与《开发规范.md:15》声明一致；`.gitignore` 覆盖 `.gradle/`、`run/`、`.idea/`、`.claude`，含作者本机路径的 `.idea/runConfigurations` 未被跟踪（§18.9 的风险在仓库层面已缓解）。
8. **测试意图有痕迹**：`common/src/test/java`、`fabric/src/test/java` 目录骨架已建好（2025-01 起），说明曾计划补测试。

## 问题

### [严重] 无任何单元测试，核心纯逻辑零回归防护
- **位置**：全仓库（证据：`find -name "*Test*.java"` 仅命中生产代码里的调试命令；所有 build.gradle 无 junit/test 依赖；`common/src/test`、`fabric/src/test` 为空目录）
- **描述**：约 50,700 行主代码（不含未参与构建的 bukkit）测试覆盖率精确为 0%。最讽刺的是历史 bug 自己都写进了规范——《开发规范.md:187/345》记载 `INeko` 的 NickName load 曾写成 `setNickName(getNickName())`（恒等于自身）——但同样的 NBT 序列化代码至今没有一行测试防回归。当前最高危的裸奔代码：
  - **遗传学引擎**：`Genome.resolveDominance`（`Genome.java:150-157`，等位基因胜负裁决）、`Genome.save()/load()`（`Genome.java:159-185`，存档往返）、减数分裂/配子生成——这是玩家存档数据的核心，坏了就是数据丢失；
  - **AI 动作解析器**：`NekoActionParser`（200 行，`NekoActionParser.java:27-79` 是区间删除+括号配对的精细字符串手术）——它只依赖 Gson+JDK，是全仓库最容易测的类，却也是 AI 功能正确性的咽喉；
  - **流式动作剥离**：`StreamingActionCleaner.java`（167 行状态机）；
  - **纯计算函数**：`ZettaiRyouiki.compute(float,float)`（`ZettaiRyouiki.java:23-31`）；
  - **数据包驱动的注册表**：`GeneticsRegistry.clearDynamicData()`（`GeneticsRegistry.java:70-96`）的三阶段 `/reload` 清理逻辑。
- **建议**：先给 loom 模块接通 JUnit 5（architectury-loom 会把 minecraft 依赖带进 test sourceSet），第一批测试只覆盖上述 4 个低耦合点（NekoActionParser 甚至无需 MC 类，把 `Bootstrap.LOGGER` 静态依赖换成注入即可秒测）；同时修复 §18.7 的 test 任务故障或升级 Gradle，让 `./gradlew check` 回到默认流程而不是被"绕过"（`开发规范.md:22`、`:365` 把跳过 test 写成了标准操作，等于制度化放弃验证）。

### [严重] 约 14MB 构建产物被提交进 git，直接违反自家规范 §17
- **位置**：`git ls-files` 证实：38 个 `.class`（`bukkit/build/classes/**`）、`bukkit/build/libs/toneko-bukkit-1.9.3.jar`、`bukkit/libs/paper-1.21.jar`（10.5MB）、`result/toneko-fabric-1.9.5.jar`（3.5MB）
- **描述**：《开发规范.md:328》（§17）明文"禁止提交构建产物：`*.class`、`build/`……（历史上出现过）"，但仓库当前仍跟踪着这些文件（`.gitignore` 有 `build/*` 却拦不住已被跟踪的历史文件）。后果：clone 体积虚增、每次 checkout 出现假 diff、`result/` 里躺着一个过期版本的 jar 误导使用者（规范 §2.3 还专门强调 result/ 由脚本维护）。
- **建议**：`git rm -r --cached bukkit/build bukkit/libs result` 后提交；`.gitignore` 补 `libs/*.jar`（保留 `gradle/wrapper/gradle-wrapper.jar` 例外）与 `result/`。

### [较高] `.gitignore` 与"datagen 产物必须提交"的规范正面冲突
- **位置**：`.gitignore` 第 6-10 行忽略 `/fabric/src/main/generated/`（及 common/neoforge 同名目录）vs《开发规范.md:46》（§2.3）"datagen 产物在 `fabric/src/main/generated/`（**提交进 git**）"及 §15 同义表述
- **描述**：实测 `fabric/src/main/generated` 有 25 个文件但 `git ls-files` 计数为 0——规范要求的提交根本不可能发生。这意味着 neoforge 侧"手工复制同步"（§2.3）失去了唯一可信来源，两端数据 JSON 漂移无版本记录。
- **建议**：二选一并改文档或改 ignore：要么移除该 ignore 条目并首次提交 generated 目录（推荐，与规范一致），要么修改 §2.3/§15 承认 generated 不入库。

### [较高] 生产包内置调试命令 `/testgal`
- **位置**：`ToNekoClient.java:21` → `GalClient.java:5-7`（`init(){ TestGalCommand.init(); }`）→ `TestGalCommand.java`（注册 `/testgal playMusic/playVoice/...` 客户端命令）
- **描述**：名字就叫 Test 的命令经由正式客户端初始化链进入每个玩家的游戏，可任意播放任意路径音频。同模块 `org.cneko.gal`（12 文件/1,302 行）在 `docs/deprecated/gal/` 已有归档副本，说明 gal 子系统本身处于半弃用状态，但活跃副本仍留在 common 主源码集并被入口调用。
- **建议**：`GalClient.init()` 置空或加 `if (FabricLoader.getInstance().isDevelopmentEnvironment())` 门控；长期看把整个 gal 包移入 `docs/deprecated/gal` 与既有归档会合。

### [中] 用户文档与代码事实多处漂移（抽查 7 处命中 6 处）
- **AI_en.md 把 player2 写成不存在的 "elefant"**：`AI_en.md:6,25,29` 教用户执行 `/tonekoadmin config set ai.service elefant`，而代码 `Player2Provider.java:24` 返回 `"player2"`——照文档操作必然失败。[中]
- **服务商列表过时**：`AI.md:5-11`/`AI_en.md:5-11` 只列 7 项，实际 `provider/impl/` 有 12 个实现（缺 deepseek、claude、mistral、ollama、openrouter）；根 `README.md:35` 写的"12 家"反而是对的。《开发规范.md:322》自己都点名了这条，半年未修。
- **配置文件扩展名错误**：`AI.md:95`、`AI_en.md:85` 写 `config/toneko.yml`，代码 `ConfigUtil.java:12` 为 `config/toneko.json`（同样是规范 §16 点名项）。
- **占位符表缺 12 项**：`AI.md` 占位符表列 10 个，代码 `ConfigUtil.java:65` 实际支持 22 个（缺 `%neko_level%`、`%world_biome%`、`%neko_surroundings%` 等）。
- **基因位点数错误**：`README.md:34`/`README_en.md:34` 称"11 个基因位点"，实际注册 15 个（`ToNekoLocus.java:13-28` 12 个 + `MoeGenetics.java:17-19` 3 个萌属性位点）。
- **Owner 类型描述错误**：`docs/API.md` §3.2 称 `Owner` 是嵌套 record，`INeko.java:410-414` 实为 Lombok `@Data @AllArgsConstructor class`（可变对象，语义不同）。
- **README_Bukkit.md 时效性**：`settings.gradle:9` 已注释 `//include 'bukkit'`（bukkit 停建），但 `README_Bukkit.md:13-19` 仍引导用户去 Modrinth/Releases 下载 `toneko-bukkit-x.x.x.jar` 并给出安装步骤。
- **建议**：把 §16 的"文档口径与代码事实一致"从口号变成流程：每处漂移单开 issue；在 CI 加一个廉价检查脚本（grep `toneko.yml`、`elefant`、"11 个基因位点"等已知错误串即失败）先止血。

### [中] 《开发规范.md》自身存在失效交叉引用
- **位置**：`开发规范.md:22`"编译验证（绕过 test 任务，见 16.5）"——全文不存在 §16.5（16 章无子节），实际内容在 §18 第 7 条（`:340`）
- **描述**：作为新人的第一入口文档，引用悬空会让人找不到"为什么绕过 test"的解释；同类小问题还有 `gradle.properties` 的 `enabled_platforms = fabric,neoforge,bukkit` 仍含已停用的 bukkit（靠 `common/build.gradle` 的 `findAll { it != 'bukkit' }` 兜底）。
- **建议**：改为"见 §18.7"；清理 enabled_platforms 或加注释。

### [中] 零格式化/静态分析基础设施，风格漂移已有实据
- **位置**：全仓库无 `.editorconfig`、checkstyle/spotless/pmd/spotbugs 任何配置（grep 全部 gradle/properties 为空）
- **描述**：目前靠自觉维持的风格已在退化：通配符 import 50 处（如 `GeneticsRegistry.java:6` `import java.util.*;`、`JsonConfiguration.java:3` `import com.google.gson.*;`）；行尾空白污染 28 个文件（tab 结尾 24 + 空格结尾 4）。另外巨型文件持续膨胀：`NekoEntity.java` 1,942 行、`NoelleMaidNekoEntity.java` 1,559 行、`NekoActionExecutor.java` 1,081 行——没有任何工具会在超阈值时报警。
- **建议**：最低成本起步：加 `.editorconfig`（indent_size=4、trim_trailing_whitespace）+ Spotless（importOrder + removeUnusedImports）跑在 CI；不必一步到位上全套检查。

### [中] 可测试性与全局可变静态状态
- **位置**：`GeneticsRegistry.java:11-30` —— `ALLELES/LOCI/WILD_POOLS/KARYOTYPES_BY_ID/DYNAMIC_*` 全部是 `public static final HashMap/HashSet`（非并发容器），跨测试用例无法隔离复位；`QuirkRegister.java:11` 同款 `static LinkedHashMap`
- **描述**：即使未来补了测试，这类"全局单例注册表"也会导致用例顺序耦合、需要反射清场。`/reload` 时 `clearDynamicData()` 在遍历 `WILD_POOLS` 的同时 remove（`:75-84`），若与其他线程读并发存在竞态（虽然当前主要在服务端线程触发）。另 `Genome.load()`（`Genome.java:184`）`catch (NumberFormatException ignored) {}` 静默吞损坏数据——正是需要测试钉住的行为。
- **建议**：为注册表补 `resetForTesting()` 或将可变 Map 收敛为 private + 已有 register/clear 门面；长期按 §8.3 的思路考虑实例化。

### [低] build.sh 无错误处理，CI 里的 datagen 是盲跑
- **位置**：`build.sh:1-11`
- **描述**：脚本无 `set -e`：本地运行时 `./gradlew build` 失败后仍会继续 `mkdir result && cp ...`，把旧 jar 当成果（CI 因 Actions 默认 `-e` 侥幸不受影响）。datagen 采用 `sleep 60 + kill -9`（规范 §18.6 自认的 hack），成功与否无人校验，每次 CI/发版白烧至少 60 秒；且 CI 中 generated 目录被 gitignore，datagen 产物在流水线里毫无用途。
- **建议**：脚本头加 `set -euo pipefail`；CI 用独立的轻量构建入口（跳过 datagen），datagen 只在本地/专项工作流跑。

### [低] CI 无任何验证维度，仅"能编译"
- **位置**：`build.yml:31`（只有 Build Mod + Upload 两步）、`releases.yml` 同构
- **描述**：push/PR 到 main 只有编译打包，无测试（本就无测试可跑）、无 lint、无文档链接校验、无 `--warning-mode` 噪音监控。PR 质量完全依赖作者本机自觉。配合上一条的 set -e 缺失，CI 目前更像"打包按钮"而非质量门禁。
- **建议**：随测试引入逐步追加 `./gradlew check`、spotlessCheck 步骤即可，不必一步到位。

### [低] 中英 README 内容不对称
- **位置**：`README_en.md:49-92` 含 Supported AI Providers 完整列表、Neko-to-Neko Chat、TTS Voice、Show Thinking Process、How to Configure 五个小节；`README.md:46-60` 对应区域只有一句话 + 指向 docs/AI.md
- **描述**：中文用户获得的信息量少于英文用户（如 Moe 资源包：英文版注明"fixing translation issues for 25+ mods"，中文版只说"可爱の猫娘翻译包~"）。违反 §16"文档中英成对"的精神。
- **建议**：以信息量多的一方为准对齐。

## 数据速览

- 主源码规模：common **379 文件 / 47,329 行**；fabric 25 / 1,989；neoforge 16 / 1,379；bukkit 25 / 3,309（已停建）；参与构建合计约 **420 文件 / 50,700 行**
- **单元测试：0 个文件、0 行、覆盖率 0%**；JUnit/test 依赖：0；`src/test` 骨架目录：2 个（均空、未被 git 跟踪）
- 文档量：docs/ 8 个 markdown 共 **1,988 行** + 《开发规范.md》**365 行** + 根 README×3 共 565 行；中英成对文档组：3 组（AI/API/docs README）；docs/deprecated 归档 java 文件 16 个
- CI 工作流：**2 个**（build.yml 56 行、releases.yml 37 行），均为编译+打包，**测试/静态分析步骤数：0**
- 格式化/lint 配置：**0**（无 .editorconfig、checkstyle、spotless、pmd、spotbugs）
- 风格漂移抽样：通配符 import **50 处**；行尾空白文件 **28 个**（tab 24 + 空格 4）；tab 缩进文件 0；CRLF 文件 0；TODO/FIXME 6 处
- git 内构建产物：38 个 `.class` + 3 个 jar ≈ **14 MB**（paper-1.21.jar 单个 10.5MB）
- 巨型文件 Top3：NekoEntity.java **1,942 行**、NoelleMaidNekoEntity.java 1,559 行、NekoActionExecutor.java 1,081 行
- 文档准确性抽查：**7 项命中 6 项漂移**（服务商名 elefant、列表 7vs12、toneko.yml、占位符 10vs22、位点 11vs15、Owner record vs class）；API.md §5.1 签名级抽查通过
- git 提交总数 577；存在"更新喵！"式无信息提交（《开发规范.md:327》自认不鼓励）
