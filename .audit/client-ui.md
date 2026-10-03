# 审计报告：客户端 UI 与渲染（mod/client + gal）

## 审计范围与方法

- **toneko 客户端包** `common/src/main/java/org/cneko/toneko/common/mod/client/**`：55 个 Java 文件，**8309 行**。
  - screens/ 20 个文件（重点精读 AIConfigScreen.java 757 行、ToNekoManagementScreen.java 557 行、ConfigScreen.java 547 行、ChatWithNekoScreen.java 409 行、GeneticsScreen.java 331 行、RouletteScreen.java 315 行、LegwearWorkbenchScreen.java 300 行等）；
  - renderers/ + renderers/layers/（NekoRenderer、NekoBossRenderer、GhostNekoRenderer、ClotheslineBlockEntityRenderer、ShengDengBewlr、FlySwordRenderer、AmmunitionRenderer、SpoiledWaterProjectileRenderer、SeatRenderer、NekoArmorLayer）；
  - items/ 渲染器（LegwearRenderer、LegwearItemRenderer、NekoArmorRenderer）；events/（ClientTickEvent、ClientNetworkEvents、HudRenderEvent、NekoBubbleRenderer、LegwearRustleHandler、LegwearWetDripHandler、ClientPlayerJoinEvent）；music/、emi/、api/、util/、ToNekoClient、ToNekoKeyBindings。
- **gal 子模块** `common/src/main/java/org/cneko/gal/**`：12 个文件，**1302 行**（DialogueScreen.java 626 行精读；GalParser/PlotParser/GalPictureReader/GalInfo/GalSoundInstance/GalSoundPlayer/TextureUtil/FileUtil/TestGalCommand/Gal/GalClient 全读）。
- 交叉验证：`开发规范.md` §3.1（Fabric Loader 禁用条款）、§3.3（初始化时序）、平台入口（fabric/neoforge 的 client 入口对 common client 类的引用方式）、语言文件 zh_cn/en_us/ja_jp/ko_kr 键数统计、mixins json（client/mixins 分组）。
- 方法：全文阅读 + 定向 grep（硬编码中文、`new Random()`、`System.out`、TODO、未判空调用点、`@Environment` 覆盖率）+ 与 lang 文件数据比对。

## 总体评价

**7 / 10。** 该领域整体质量高于常见 mod 水准：网络回调统一 `context.client().execute()` 主线程调度、多数 Screen 用翻译键而非硬编码文本、若干文件（NekoBubbleRenderer、LegwearRenderer、RouletteScreen、ClientConfig、ShengDengBewlr）注释与设计俱佳。扣分点集中在：ToNekoManagementScreen 的布局算术错误与"init/render 双份布局逻辑"、ChatWithNekoScreen 用 § 格式码字符串当数据协议且每帧 O(n²) 重排、gal 子模块的日志风格/死代码/资源泄漏、以及个别违反项目自身规范（§3.1 Fabric Loader）的用法。

## 优点

1. **线程纪律良好**：`ClientNetworkEvents.init()` 中全部 14 个 S2C 接收器都包在 `context.client().execute(...)` 里（如 ClientNetworkEvents.java:34、46、98），新代码还带断线竞态防护（:132、:144、:162 `mc.level == null || mc.player == null` 注释"断线竞态防护"）。`VoiceEntry`（AIConfigScreen.java:668-686）用 `pending.isDone()` 在渲染线程轮询异步结果，避免跨线程写 UI 状态，注释明确说明该设计。
2. **资源管理到位**：DialogueScreen 对立绘/大图纹理做了缓存（含失败记忆 `cachedTextures.put(path, null)`，:266）、try-with-resources 关流（:273、:315）、`onClose` 统一 `release(texture)`（:614-618）。gal 加载失败不崩屏而是降级为空显示并允许刷新重试。
3. **i18n 覆盖面广**：AIConfigScreen 全部 40+ 个配置项均走 `Component.translatable("screen.toneko.config.key.ai.*")`（AIConfigScreen.java:81-215），en_us.json 2049 键 / zh_cn.json 2043 键基本对齐；RouletteScreen 连图标路径都用数据化 record 定义（RouletteScreen.java:71-105）。
4. **多个高质量渲染实现**：`LegwearRenderer.actuallyRender` 双 pass 左右腿分色，并用长注释解释 GeckoLib 内部机制（"isReRender 必须传 true……否则 AnimationProcessor 第二次 tick 会把骨骼 lerp 回初始快照"，LegwearRenderer.java:76-79）；`NekoBubbleRenderer` 有完整的数据流 javadoc、已知限制说明、排版一次性缓存、维度切换清理（NekoBubbleRenderer.java:16-33、88-96）；`ShengDengBewlr` 把 BEWLR 触发链路和顶点格式要求写得清清楚楚。
5. **可扩展的 Screen 组合体系**：`NekoScreenBuilder`（widget 工厂列表 + 插入位置 API + clone）配合 `ButtonFactories`/`TooltipFactories`/`ScreenBuilders`/`NekoScreenRegistry` 实现"每种猫娘一个界面定义"，`InteractionScreen` 还能从按钮消息键自动推导 tooltip 键是否存在（InteractionScreen.java:74-80，用 `TranslatableContents.getFallback()` 判断）——是不错的控件复用方案。
6. **容器屏规范**：LegwearWorkbenchScreen 采用"拖动仅本地预览、松手发一次 C2S"的正确网络模式，并在 `containerTick` 里按槽位 ItemStack 实例变化重置滑杆（LegwearWorkbenchScreen.java:63-70）；对自定义 512×512 贴图必须显式传纹理尺寸的坑有注释。
7. **客户端配置类范本**：`ClientConfig` 双检锁 + volatile、入参校验（颜色正则、时长下限）、非法值回退默认（ClientConfig.java:44-100），并注明"专用服务器不会加载本类"。
8. **隔离基本成立**：渲染器注册只出现在平台 client 入口（fabric/src/.../client/ToNekoClient.java:50-67、neoforge ToNekoNeoForgeClient.java:73+），common 侧 mixins json 把 6 个客户端 mixin 单独放 `"client"` 数组（toneko.mixins.json），5 个事件类带 `@Environment(EnvType.CLIENT)`。

## 问题

### [较高]

1. **ClientTickEvent.processKeyInput 多处不判空即解引用 `client.player.connection`**
   - 位置：`events/ClientTickEvent.java:42-80`（9 处，LIE/GET_DOWN/RIDE/QUIRK/SPEED/JUMP/VISION/RIDE_HEAD/TONEKO_MANAGEMENT 键）。
   - 描述：同一方法里 DISMOUNT/MULTI_TOOL/STEALTH/PULL_UP 分支都有 `if (client.player != null)` 防护，而这 9 个分支直接 `client.player.connection.sendUnsignedCommand(...)`。玩家为 null 且无屏幕打开的窗口期（断线瞬间、维度切换黑屏间隙）按下这些键会 NPE 并在主线程炸出崩溃。
   - 建议：方法入口统一 `if (client.player == null && client.level == null) return;` 或每个 consumeClick 分支补齐判空，消除同方法内防护不一致。

2. **ToNekoManagementScreen 布局算术自相矛盾 + 无界内容溢出**
   - 位置：`screens/ToNekoManagementScreen.java:269`（`maxVisible = (height-CONTENT_START_Y-60)/75`）vs :275（行高 `* 85`）vs :506（滚动又用 `/85`）；别名区 :284-299 每个别名占 18px 但行预算固定 85px；屏蔽词列表 :371-385 同样无上限。
   - 描述：三处行高常量不一致导致滚动边界计算错误；一只猫娘有多个别名/屏蔽词时，控件会越过 85px 行高与下一只猫娘的控件重叠、甚至压到底部"返回/刷新"按钮之下。布局 Y 坐标还在 initRequestsTab（:175-257）和 render()（:462-473）各算一遍，改一处必然漏另一处。
   - 建议：抽出行高/间距常量统一引用；把"每只猫娘块"改为按实际子项数量动态测高（或像 AIConfigScreen 一样做成滚动 Entry 列表）；布局坐标只在 init 计算，render 只消费。

### [中]

3. **ChatWithNekoScreen 用格式码字符串当数据协议 + 每帧 O(n²) 重排**
   - 位置：`screens/ChatWithNekoScreen.java:49-56`（HISTORY/STREAMS 静态表）、:81/:105/:149（写入 `"§6> §f"` 前缀）、:282-293（`roleOf/textOf` 按前缀反解角色）。
   - 描述：消息角色靠 § 颜色码前缀编码再解析，用户消息若以相同序列开头会被误判角色；历史/流状态是 static Map，跨服务器不清理（仅 LRU 50 条兜底）。同时 `blockHeight`/`renderBlock` 每帧对全部 ≤200 条历史逐字符调 `font.width(current.toString())` 重排（:370-403），AI 长回复时是可感知的帧开销。
   - 建议：历史存 `record ChatLine(MsgRole role, String text)`；换行结果按 (text, width) 缓存（或直接用 `ComponentRenderUtils.wrapComponents`，NekoBubbleRenderer 已示范）。

4. **NekoRenderer 缺失皮肤时回退随机皮肤 → 每帧随机贴图闪烁**
   - 位置：`renderers/NekoRenderer.java`（`getTextureResource` 中 `checkResource(id)` 失败则 `animatable.getRandomSkin()+".png"`）；`entities/NekoEntity.java:358-360` + `misc/NekoSkinRegistry.java:34-40`（`(int)(Math.random()*skins.size())`）。
   - 描述：GeckoLib 每帧调用 `getTextureResource`；一旦某皮肤的贴图缺失而模型存在，实体会每帧在不同皮肤贴图间跳变。另外 `getRandomSkin` 可能返回 `null`，拼出 `textures/neko/null.png`。
   - 建议：随机回退结果按实体持久化（首次失败时 `setSkin` 固定下来），或直接回退 `common.png`；对 null 值做保护。

5. **网络驱动的屏幕构造器缺防御： GeneticsScreen 构造器强转实体**
   - 位置：`screens/GeneticsScreen.java:49-52`（`(LivingEntity) Minecraft.getInstance().level.getEntity(entityId)` 后立即 `GeneticsRegistry.getKaryotype(targetEntity)`）；`ClientNetworkEvents.java:61-62、117、124、146`（多处 `UUID.fromString` 无 try-catch）。
   - 描述：entityId 对应实体尚未同步/已卸载时 getEntity 返回 null，构造器内继续调用会 NPE——异常发生在 `client().execute` 任务里会导致游戏崩溃。UUID 解析同理，被篡改服务端发坏包可直接崩客户端。对比之下 NekoBubbleRenderer.show（NekoBubbleRenderer.java:57-62）就做了 try-catch，标准不统一。
   - 建议：构造器判空降级（找不到实体就关屏/提示）；所有 `UUID.fromString` 收敛到一个 `parseUuidOrNull` 工具。

6. **gal 子模块整体工程质量明显低于主体**
   - 位置与证据：
     - 日志风格：DialogueScreen.java:73、118、127、168、285 等 15+ 处 `"哎呀呀！…QAQ~"` 式卖萌日志，且正常流程打 INFO（:105、114）；GalSoundInstance.java:40-145 同风格。难以 grep、无法按级别过滤噪音。
     - 死代码含潜在 NPE：PlotParser.java:56-60、86-90 `getNextPlotIfShouldBeSwitch()` 全仓库零调用，且 `next.startsWith("plot:")` 未判 next 为 null（choices-only 节点 next==null 即 NPE）。
     - 资源泄漏：GalParser 构造器（GalParser.java:27-30）把 `FileUtil.getFileReader(...)` 的 Reader 直接交给 Gson，全程无人 close。
     - PNG 探测依赖 mark/reset：GalPictureReader.java:19-28 先在同一条流上读 16 字节量尺寸再 reset 交给 `NativeImage.read`，`file.toURI().toURL().openStream()` 的流不保证 markSupported，不支持时纹理解码拿到的是掐头的流。
     - 杂项：GalInfo.java:57 残留 `System.out.println();`；`GalInfo.parse` 失败返回 null 后 GalParser 构造器直接 `galInfo.getPlots()` NPE（GalParser.java:23→25）；`AuthorInfo.getRole()` 对 null role `split` 会 NPE（GalInfo.java:41-45）；`TestGalCommand` 开发用 `/testgal playDialogue <任意磁盘路径>` 随正式版发布；`Gal.getInstance()` 懒汉单例无同步（GalSoundInstance.java:20-26）；`GAL_MODID` 常量无人使用。
   - 建议：日志改为常规中文/英文 + 正确级别；删除或修好死代码；Reader 用 try-with-resources；尺寸探测改为先读字节自己解码（或复制一份流）；测试命令移到 dev 环境或加 `FabricLoader.isDevelopmentEnvironment()` 守卫。

7. **common 客户端入口违反《开发规范》§3.1 使用 Fabric Loader 类**
   - 位置：`mod/client/ToNekoClient.java:5、25`（`FabricLoader.getInstance().getModContainer(MODID)` 注册内置资源包），该方法被 neoforge client 入口直接调用（neoforge/src/main/java/org/cneko/toneko/neoforge/client/ToNekoNeoForgeClient.java:58）。
   - 描述：规范明文"common 禁止使用 Fabric Loader 类（唯一允许 Environment 注解）"，并把 PermissionUtil 的 Class.forName 保护称为"例外而非范例"。此处既无保护也无例外说明；NeoForge 运行时若无 loader shim 即 `NoClassDefFoundError`，属于埋雷式跨平台耦合。
   - 建议：把内置资源包注册下沉到两个平台的 client 入口各自实现（fabric 用 FabricLoader，neoforge 用自身 API），或在调用点做 try/catch + 日志。

8. **ClientTickEvent.onTick 每 tick 白扫一遍实体表**
   - 位置：`events/ClientTickEvent.java:240-242`：`var entities = EntityUtil.getLivingEntitiesInRange(p, p.level(), 16);` 结果从未使用，仅为让 `tick++` 到 100 时清理 poseMap。
   - 描述：20 次/秒 × 每次 AABB 扫描全部 LivingEntity 的无用功；清理逻辑本身只需要每 100 tick 执行一次计时。
   - 建议：删掉扫描，直接 `if (++tick % 100 == 0) { ... }`。

### [低]

9. **死/半成品 UI 直接挂在功能路径上**：`MateConfirmScreen.java:32-43` 整个类无任何调用点，且两个按钮文案硬编码中文（"忽略交配过程"/"完整交配过程"），第二个按钮回调为空（`:39-42` "// 添加完整交配过程逻辑"）；`PlotScrollScreen.java:9-13` 是零内容空屏，却被 `OpenPlotScreenPayload` 真实触发（ClientNetworkEvents.java:90-92），玩家会看到一张关不掉只能 ESC 的空屏。建议删除 MateConfirmScreen、给 PlotScrollScreen 补内容或暂时摘掉包路由。
10. **Button 当 Label 用**：ToNekoManagementScreen 11 处 `btn -> {}` 空回调按钮充当分组标题/条目文本（:181-184、191-194、218-221、362-365 等），误导可访问性（屏幕阅读器会念成"按钮"）也浪费控件；建议改 drawString。
11. **硬编码魔法数与 lang 数据漂移**：ConfigScreen.java:519 `MAX_RANDOM = 29`，但 lang 文件实际有 49 个 `screen.toneko.config.random.*` 键（0..48）——后 20 条彩蛋永远展示不出来；ToNekoManagementScreen.java:141/452 tab 宽度公式重复两遍；ChatWithNekoScreen.keyPressed（:159-160）用裸 257/335/256 而 DialogueScreen 用 GLFW 常量，风格不一。
12. **AIConfigScreen 细节瑕疵**：`StringEntry` 第 468-470 行的重载构造器接收 `height` 参数但完全忽略（ getHeight 恒 40）；`ProviderEntry.mouseClicked`（:611-613）不分左右键一律"下一个"，◀▶ 只是装饰；`SliderEntry.setFocused(false)`（:552-553）不像 StringEntry 那样转发给内部 slider，且 mouseClicked（:564-567）整行点击都会把 slider 置焦，之后行内任意拖动都会拖滑条；`VoiceEntry.writeConfig`（:752-755）即时落盘而其余条目等"应用"按钮，交互不一致（虽有注释解释）。
13. **NekoModel 大段注释掉的代码**：renderers/NekoRenderer.java getModelResource/getAnimationResource 各留 ~12 行被注释的旧逻辑及 "(bushi)" 玩笑注释，应删除交给 git 历史。
14. **KeyBinding 注册样板冗余**：ToNekoKeyBindings.java 20 个键 × 8 行重复结构 ≈170 行，可用数组循环注册；所有键共用类别 `"key.toneko.lie.category"` 名不副实。另有 ClientNetworkEvents.java:18、22 的 `import ...*` 通配符导入。
15. **本地化完成度差距大**：ko_kr.json 仅 373 键（约为 en_us 2049 键的 18%），ja_jp 1411、zh_tw 1565 也明显落后；韩语玩家大量界面回落英文/中文。
16. **NekoScreenBuilder 插入 API 可越界**：NekoScreenBuilder.java:64-66 `widgets.add(targetIndex - count, widget)` 当 count > targetIndex 时抛 IndexOutOfBoundsException；`setStartY(startY)` 内部偷偷 `+15`（:15-17）语义意外。另 EntityMixin（both-sides mixin）import 了 `client.api.ClientEntityPoseManager`（EntityMixin.java:9），目前因懒加载且该类不含客户端专属 import 而无害，但属架构卫生隐患。
17. **性能小项**：HudRenderEvent.renderNekoEnergyBar 用最多 91 次 `fill` 逐像素画渐变（HudRenderEvent.java:225-240），可用单次渐变矩形；ConfigScreen.updateText 每次 `new Random()`（ConfigScreen.java:529）；FlySwordHUD "Fuel:"/"m/s" 文案未走语言文件（HudRenderEvent.java:300-306）。

## 数据速览

- 审计文件：67 个 Java 文件（toneko client 55 个 / gal 12 个），共 **9611 行**；其中 screens/ 20 个文件约 4300 行，最大单文件 AIConfigScreen 757 行。
- `@Environment(EnvType.CLIENT)` 标注：仅 5/55 个 toneko client 文件（隔离主要靠"只被平台 client 入口引用"的约定维持）。
- S2C 接收器：14 个，全部经 `context.client().execute()` 主线程调度；其中 3 个带显式断线竞态守卫，`UUID.fromString` 无保护的调用点 4 处。
- `processKeyInput` 中 `client.player.connection...` 不判空调用 9 处（同法内有判空的分支作对照）。
- ToNekoManagementScreen：空回调 Button-as-label 11 处；行高常量 75/85 冲突 2 处；init/render 双份布局计算各 1 份。
- ConfigScreen 彩蛋池：代码 `MAX_RANDOM=29` vs lang 实际 49 键 → 20 条永不展示。
- 语言文件规模：en_us 2049 / zh_cn 2043 / zh_tw 1565 / ja_jp 1411 / ko_kr 373 键。
- gal 子模块：卖萌风格 ERROR/INFO 日志 17+ 处；零调用死方法 2 个（含未判空潜在 NPE）；未关闭 Reader 1 处/每次加载；残留 `System.out.println()` 1 处；随版发布的 `/testgal` 开发命令 1 个。
- 硬编码中文 UI 字符串：MateConfirmScreen 2 处（整个类为死代码）；toneko client 其余 Screen 文案基本全部走翻译键。
- 渲染器：11 个（实体 8 + BE 1 + 物品 3 + 层 1 中部分复用），其中 ClotheslineBlockEntityRenderer 含 ~90 行脚本生成的硬编码顶点数据（文件头已注明来源）。
