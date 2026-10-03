# 实体系统 (entities) 审计报告

## 审计范围与方法

- 实际通读（逐行 read）：`common/src/main/java/org/cneko/toneko/common/mod/entities/` 下全部 41 个 Java 文件，共 **11,496 行**。重点精读：
  - `NekoEntity.java`（1,942 行，全读）
  - `NoelleMaidNekoEntity.java`（1,559 行，全读）
  - `boss/mouflet/MoufletNekoBoss.java`（934 行，全读）
  - `FlySwordEntity.java`（781 行）、`NekoInventory.java`（642 行）全读
  - `INeko.java`（416 行）、`AmmunitionEntity.java`、`CrystalNekoEntity.java`、`FightingNekoEntity.java`、`GhostNekoEntity.java`、`RavennEntity.java`、`SpoiledWaterProjectile.java`、`ToNekoEntities.java` 全读
  - `ai/NekoBrain.java`、`SeatEntity.java`、`AdventurerNeko.java` 部分精读
- 用 grep 做了横切统计：直接 `navigation.moveTo` 调用、`EntityDataSerializers.ITEM_STACK` 使用、`new Random()`、遗留调试日志、死字段引用链。
- 对两个关键结论做了**字节码级验证**（反编译 gradle cache 中 1.21.1 mojmap patched jar）：
  - `Projectile.onHit(HitResult)` 原版已按类型派发 `onHitEntity/onHitBlock`；
  - `LivingEntity.eat(Level,ItemStack)` 内部调用 `ItemStack.consume(1,this)`，非创造模式**必定 shrink(1)**。

## 总体评价

**6 / 10**

实体系统功能密度很高且有不少亮点设计（NekoBrain 中央移动仲裁、幽灵实体的 NBT 快照转生、Noelle 的枚举状态机+旧档迁移），核心猫娘类的同步数据也严格遵守《开发规范》§8 的 STRING/INT/FLOAT 约束。但 `INeko` 接口里藏着两个影响所有实现方的数据丢失级 bug（fixQuirks 逻辑反转导致玩家每次加入清空性癖、NickName 永不回读），加上进食双重消耗、弹药双重命中、Boss 的 `die()` 误用等问题，说明"接口默认方法 + 继承复用"这条主干路径缺乏基本回归验证；同时 `NekoEntity` 以 ~61 字段/~150 方法承担了至少 10 类职责，是典型的上帝类。

## 优点

1. **NekoBrain 移动仲裁架构出色**：`ai/NekoBrain.java:14-31` 的 Javadoc 明确写出仲裁规则（高优先级替换、同优先级 ≥10 tick 防抖、同 source 去重、切换目标不 stop），并有卡住检测常量组（`STUCK_CHECK_INTERVAL/STUCK_DISTANCE/STUCK_CANCEL_TICKS`, :60-63）。这是把"决策与执行分离"落到了实处。
2. **注册模式与开发规范 §3.2/§6.5 完全一致**：`ToNekoEntities.java` 每个实体一对「`XXX_ID`(ResourceLocation) + 可空 `XXX` 字段」，构建方法全部 `@ApiStatus.Internal` 的 `Supplier` 且带 `sized()/clientTrackingRange()`（如 :96-102, :139-145），common 不碰平台注册。
3. **核心类同步数据合规且有性能意识**：`NekoEntity.java:168-173` 只用 STRING/INT/FLOAT 序列化器；萌属性用分隔符字符串 + 缓存（:120-121, :365-373）；服务端 slowTick(20t) 强制重写易失值兜底（:940-941）正是 §8 要求的做法；液体状态在 tick 里缓存避免 AI/动画高频查方块（:1272-1277）。
4. **NoelleMaidNekoEntity 的工程质量是包内最佳**：所有数值全部命名常量（:148-192 共 30+ 个）；Stage/Branch 枚举状态机语义清晰（:53-143）；`readAdditionalSaveData` 有旧档迁移——无 `NoellePeakTrauma` 时按阶段反推峰值（:1408-1425），旧 WITHERED 实体补初始化死亡倒计时（:1451-1453），`Stage.valueOf` 均有 try/catch 兜底。
5. **幽灵转生的 NBT 快照方案稳健**：`GhostNekoEntity.createGhostFrom`（:110-135）用 `saveWithoutId` 复制完整灵魂数据，并逐一防御性移除 `UUID/id/Passengers/Vehicle/Leash/Motion` 且注释解释原因；末尾显式 `expressTraits()` 保证基因一致。
6. **AI 存储与 UUID 解耦**：`getAIStorageId()` 首次生成持久 ID 并迁移旧聊天记录（`NekoEntity.java:1787-1795`），NBT 持久化（:232-234），死亡转幽灵后记忆可延续；聊过天的猫娘免自然消失（:701-707）。
7. **子类对仇恨系统的扩展边界处理细致**：FightingNeko 把战斗交给 Goal 时只在"goal 追踪的正是仇恨目标"时才清除（`FightingNekoEntity.clearHatred`）；Mouflet 重写 `tickHatred` 并注释说明为何不向 Brain 重复提交 COMBAT 移动（`MoufletNekoBoss.java:260-278`）。
8. **INeko 用 default 方法统一玩家/实体数据源**：`saveNekoNBTData/loadNekoNBTData` 单一实现供 PlayerEntityMixin 与 NekoEntity 共用（符合 §8 "不得各写一套"），等级修饰符统一走 `applyModifier`（:332-346）。

## 问题

### [严重] S1 — `INeko.fixQuirks` 条件反转：玩家每次加入被清空全部有效性癖（数据丢失）

- 位置：`entities/INeko.java:182-185`
```java
default void fixQuirks(){
    // 修复quirks
    this.getQuirks().removeIf(quirk -> QuirkRegister.hasQuirk(quirk.getId()));
}
```
- `QuirkRegister.hasQuirk(id)` 为 true 表示该性癖**存在且有效**（`quirks/QuirkRegister.java:62-64`）。此逻辑把有效性癖全部删除、只留下失效的。对照同仓库正确写法 `api/NekoQuery.java:118-121`（`removeIf(q -> getById(q) == null)`）即可确认反转。
- 该方法在玩家加入时被调用：`events/ToNekoEvents.java:202` `player.fixQuirks();`，而 `PlayerEntityMixin` 只覆盖了 `getQuirks` 未覆盖 `fixQuirks` → 玩家性癖列表当场被清空，随后随存档持久化即永久丢失。
- 建议：改为 `!QuirkRegister.hasQuirk(...)`，并补一条"加入前后 quirks 集合不变（当全部有效时）"的单测。

### [严重] S2 — NickName 从 NBT 永远加载不回来（存档回读失效）

- 位置：`entities/INeko.java:263-265`
```java
if (nbt.contains("NickName")){
    this.setNickName(this.getNickName());   // 应为 nbt.getString("NickName")
}
```
- 把自己赋值给自己，保存的昵称永远读不回来。由于 `PlayerEntityMixin.java:299` 与 `NekoEntity.readAdditionalSaveData`（:272）都走这个 default 方法，**玩家和实体的昵称均无法跨重启保留**（命令 `NekoCommand.setNickName` 设置的值重启即丢）。
- 建议：一行修复 `this.setNickName(nbt.getString("NickName"))`；顺带排查同类 save/load 字段是否成对。

### [较高] P1 — 自动进食双重消耗：`eat()` 已 shrink(1) 又手动 `shrink(1)`

- 位置：`entities/NekoEntity.java:1377-1384`（hurt 中寻找食物回血）
```java
this.heal(food.nutrition());
this.eat(this.level(), stack);   // LivingEntity.eat 内部 ItemStack.consume(1,this)=shrink(1)
stack.shrink(1);                 // 再扣 1 —— 每餐吃掉 2 个
```
- 已反编译验证 `LivingEntity.eat(Level,ItemStack)` 调用 `ItemStack.consume(1,this)`，非无限材料必 shrink。同一错误模式还出现在 `MoufletNekoBoss.doLittleDevilInteraction`（:773-779）：`eatOrStoreFood(stack)`（内部 eat 扣玩家背包 1 个）之后又 `player.getInventory().removeItem(i, 1)` 再扣 1 个——偷食物一次偷走 2 个。
- 注意 `addItem()`（:551-565）与 `eatOrStoreFood` 的饥饿分支没有手动 shrink，是对的；三处行为互不一致恰说明作者对 `eat()` 是否消耗认知混乱。
- 建议：统一封装一个 `eatFood(ItemStack)` 工具方法，明确"eat 负责消耗"；删除多余 shrink/removeItem。

### [较高] P2 — AmmunitionEntity.onHit 双重派发，弹药效果触发两次

- 位置：`entities/AmmunitionEntity.java` `onHit`（约 :196-215）
```java
protected void onHit(@NotNull HitResult hitResult) {
    super.onHit(hitResult);          // 原版已按类型调用 onHitEntity/onHitBlock
    if (!this.level().isClientSide) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) hitResult);   // 第二次！
        } ...
```
- 反编译确认原版 `Projectile.onHit` 已经按 `HitResult.Type` 派发到（被子类覆盖的）`onHitEntity/onHitBlock`，其中各自会执行 `ammo.hitOnEntity/hitOnBlock` 并 `discard()`。super 返回后再手动再派发一次 → 爆炸弹双倍爆炸、闪电弹双倍落雷、仇恨设置重复。`discard()` 不能阻止第二次执行，因为代码未检查 `isRemoved()`。
- 建议：删掉 onHit 中的手工派发（原版 super 已足够），或在 onHitEntity/onHitBlock 开头加 `if (this.isRemoved()) return;`。

### [较高] P3 — Noelle 残花死亡流程误用 `die()`：告别分支永不执行、尸体成为"已死亡空壳"

- 位置：`entities/NoelleMaidNekoEntity.java:692-715`（performWitheredDeath）
```java
this.die(this.damageSources().generic());   // 此时 health > 0
if (this.isAlive()) {                        // die() 不改血量 → isAlive() 恒真
    witheredDeathTimer = WITHERED_DEATH_MAX;
    return;
}
// “真正死亡 —— 遗物与告别” 分支永不到达
```
- 原版契约是先扣血至 0 再 `die()`；直接调用时 `NekoEntity.die`→`super.die` 会完整执行 `dropAllDeathLoot + inventory.dropAll + 掉日记 + 化幽灵 + broadcastEntityEvent(3)`（客户端播放死亡动画），但实体血量仍 >0、不会被移除。于是：物品/装备全部掉地、幽灵被复制生成、而本体顶着 dead 标记继续存活（后续自伤打到 0 血时第二次 `die()` 因 dead 标志被跳过，无声消失）。注释声称"经过 die() 管线以支持不死图腾"，但图腾分支判断的是 `tryUseTotem` 提前 return，与"存活"判定完全不是一回事。
- 建议：改为 `setHealth(0)` 后走标准伤害管线（如 `hurt(damageSources().generic(), Float.MAX_VALUE)`），或自行拆分"图腾检查"为独立方法供此处复用，不要在血量 >0 时调用 `die()`。

### [较高] P4 — Boss 无伤回复失控：挂机后每 tick 一次瞬间治疗（≈无敌）

- 位置：`entities/boss/mouflet/MoufletNekoBoss.java:592-601`
```java
unhurtTime++;
if (unhurtTime > 1280) {
    this.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0)); // 每 tick 瞬间治疗 I
}
```
- `unhurtTime` 达标后从不重置，从第 1281 tick 起每 tick 施加一次即时治疗直到再次受伤——64 秒脱战后等于每秒回 40+ 血且伴随效果包刷屏。对照 `FightingNekoEntity.tick`（同型代码）用的是 `REGENERATION, 1, 0`——1 tick 再生 I 实际回复量≈0，等于没写。两处明显是同一段代码复制后朝两个相反方向出错。
- 建议：触发后 `unhurtTime = 0`；Mouflet 改为一次性 HEAL 或短周期 REGENERATION。

### [较高] P5 — `giftItem` 等处 `Objects.requireNonNull(getCustomName())` 存在 NPE 崩服路径

- 位置：`entities/NekoEntity.java:476、503、509`
- 自定义名为 null 时右键送礼直接抛 NPE 打断交互/网络线程。代码自己在 `trySendHatredMessage`（:1493）就承认 customName 可能为 null 并现场补名字（`this.getCustomName() == null || getName().getString().equals("null")`），说明空名状态真实存在（旧存档、外部 mod 生成的实例等）。
- 建议：统一 `getName().getString()` 或提供 `getDisplayNameSafe()`；顺带清理 `"null"` 字符串比较这种魔法判断。

### [中] M1 — NekoEntity 上帝类：~10 个职责域挤在一个 1942 行类里

- 位置：`entities/NekoEntity.java` 全文
- 量化：1,942 行、~61 个字段声明、~154 个成员方法签名；最长方法 `hurt()` 71 行、`giftItem()` 57 行、`equipArmors()`/`readAdditionalSaveData()` 各 51 行。按关键词粗估职责域出现频次：hatred×49、diary×44、inventory×34、animation×33、genome/breeding×32、totem×16、gift×9——外加皮肤/萌属性/能量/采集动力/等级因子/骑乘=坐下/交互菜单/AI prompt 等。字段组织也乱：`NEKO_LEVEL_ID` 定义在 :1745、`NICKNAME_ID` 在 :1835、`HUG_RANGE_SQ` 在使用点之后 :1160。
- 建议（方向）：至少拆出 `NekoHatredSystem`（仇恨/寻仇消息/武器切换）、`NekoGiftService`（送礼/好感）、`NekoEquipmentHelper`（盔甲槽位/防御计算）、`NekoDiaryStore`（日记+NBT）、繁殖逻辑下沉到 genetics 包；EntityDataAccessor 与常量集中到类首。

### [中] M2 — `equipArmors` 硬编码原版盔甲清单，漏海龟壳与全部模组盔甲

- 位置：`entities/NekoEntity.java:578-595`：逐一 `stack.is(Items.LEATHER_HELMET)||...` 枚举 24 个物品。
- 海龟壳不在清单里永远装不上；任何 mod 盔甲也无法通过礼物/拾取穿上。1.21 有现成 API：`LivingEntity.getEquipmentSlotForItem(stack)`（或 item 的 `getEquipmentSlot`）。
- 建议：用 API 判定槽位，特例仅保留 `LegwearItem → LEGS`。

### [中] M3 — 违反《开发规范》§8.6：entities 域内 6 处直接 `navigation.moveTo`

- 证据（grep 全量）：
  - `ai/goal/NekoAttackGoal.java:183`
  - `NoelleMaidNekoEntity.java:628`（残花自毁走向怪）、`:1131`（DEFECTIVE/BROKEN 走向攻击者）
  - `MoufletNekoBoss.java:553`（宠物跟随主人）、`:566`（叼东西）、`:734`（贴贴互动）
- 规范明确"移动一律 nekoBrain.submitMove(...)，禁止直接 navigation.moveTo"。讽刺的是 Noelle/Mouflet 自己的注释都在强调 Brain 仲裁（MoufletNekoBoss.java:261-263），却在别处绕过它——这些移动不会参与防抖/卡住检测，可能与 Brain 当前意图互相打架。
- 建议：替换为 `nekoBrain.submitMove(target, speed, BehaviorPriority.XXX, this)`；确需旁路的位置写明理由。

### [中] M4 — 违反《开发规范》§8：SynchedEntityData 使用 ITEM_STACK 序列化器

- 证据：`AmmunitionEntity.java:33-34`（BAZOOKA_STACK、AMMUNITION_STACK）、`SpoiledWaterProjectile.java:29`（ITEM），共 3 个 accessor。
- 规范："只用 STRING/INT/FLOAT 序列化器，禁止在 entityData 放复杂对象"。ItemStack 同步开销大（每次 set 全量编码）且渲染端只需 id/count 时浪费带宽。
- 建议：改同步 `STRING`（registry id）+ `INT`（count），或仅同步渲染必需的最小信息。

### [中] M5 — Mouflet.tick 每 tick 强写移速基值，覆盖收服/个体差异

- 位置：`entities/boss/mouflet/MoufletNekoBoss.java:483-485`
```java
} else {
    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.11); // 每tick执行
}
```
- 非魅惑状态下每 tick 都执行，直接踩掉 `tame()` 设置的 0.12（:869）和 randomize 的个体差异；属性基值写入每 tick 标脏。魅惑分支同样硬编码 0.143。
- 建议：只在状态切换边沿写一次（onStageChanged 式回调），或用临时 AttributeModifier 表达加速。

### [中] M6 — 消息分发长 if-else 链 + Noelle 整段复制父类方法

- `NekoEntity.sendHurtMessageToPlayer`（:1654-1694）：16 个连续 `moe.contains(x)` 分支，仅 key 与变体数不同 → 应做成 `Map<String,String[]>` 或按 tag 命名约定循环。
- `NoelleMaidNekoEntity.trySendHatredMessage`（:1191-1207）与父类版本（:1490-1508）重复度约 90%，仅冷却数值（200 vs 60）与消息键不同——应模板化（protected hook 取 cooldown/key），而不是整段抄。这也是"继承层次设计"上的信号：钩子粒度不够。

### [中] M7 — 日记成书生成代码两份 + `new Random()`

- 位置：`NekoEntity.java:802-820`（静态版）与 :826-854（实例版）结构几乎相同；两处均 `new Random()`（:805, :840）而非实体自带的 `random`，既不一致也不可注入测试。实例版完全可以委托静态版。

### [低] L1 — 遗留调试日志随每次猫娘死亡刷屏

- `GhostNekoEntity.java:127-130、147-148`：两条 `LOGGER.info("[GHOST] ...")` 带"调试：定位名字继承问题"注释，问题早已用兜底 `setCustomName` 修掉，日志应降 DEBUG 或删除。

### [低] L2 — FlySwordEntity 杂项

- `private int tickCount = 0;`（:101）遮蔽 `Entity.tickCount`，两者各自递增极易混淆；`_lx/_ly/_lz`（:100）命名违反规范；`hurt()` 忽略 `amount` 参数（任何 0 伤来源也销毁）且 discard 后仍返回 false，语义含糊（:596-613）；`buildFlySwordItem` 的 `item == null` 判断无效（默认注册表返回 AIR 而非 null，:223-224）。

### [低] L3 — 死状态字段（只写不读）

- `NoelleMaidNekoEntity.hasBeenHealedByPlayer`（:210，仅 randomize 置 false、save/load 往返，无任何读取点）；`MoufletNekoBoss.eatenCatnip`（:105，仅 StealItemGoal 自增，无读取且不入 NBT）。要么接上玩法，要么删除。

### [低] L4 — 其他小瑕疵

- `NekoEntity.getTypeName()` 无意义覆盖 `return super.getTypeName()`（:1700-1701）；`getSkin()` 是带副作用的 getter（空则随机并写回 entityData，:346-351）；`startRiding` 把 isSitting 置 true，"坐=骑"语义混淆（:1188-1192）。
- `NekoInventory.swapPaint` 计算后弃用变量、while 归一化后什么都不做（:145-153）；`add(int,ItemStack)` 是反编译粘贴的原版 Inventory 代码（label81/var10 风格，:248-344），建议重写成手写风格便于维护。
- `RavennEntity.getCustomName()` 每次调用 new 一个 Component 且违反"无自定义名返回 null"的原版契约（RavennEntity.java:37-39）。
- `INeko.loadNekoNBTData` 的 `UUID.fromString(key)`（:260）对损坏存档无防护，坏 key 会中断实体加载。
- 巨型 AI 人设文本块内嵌代码：`CrystalNekoEntity.generateAIPrompt` 约 175 行、`NoelleMaidNekoEntity.PROMPT` 约 65 行 + `getStagePrompt` 11 段拼接（:1467-1559）——应外置为资源文件。
- `ToNekoEntities.getAdventurerNeko` 的 `build("adventure_neko")` 与 `ADVENTURER_NEKO_ID("adventurer_neko")` 名字不一致（:105-110）。

### 通用维度小结

- **并发/线程安全**：实体数据仅在主线程与渲染线程按 MC 惯例分工访问，未见共享可变静态状态（`FlySwordEntity.FUEL_TABLE` 仅静态初始化写入，安全）；`hatredTarget/lastTrackedEnemy/grabbedPlayer` 等强引用均有生命周期清理，风险可控。
- **性能**：主要热点是每秒级的范围实体扫描（increaseEnergy、checkLoneliness、守护本能扫描），量级可接受；`playExpressAnim` 遍历全服玩家手算距离（NekoEntity.java:1259-1264）应改用 trackedPlayers；Mouflet 每 tick 属性写见 M5。
- **可测试性**：整个 entities 包零测试，且时间源（level().getGameTime）、随机源（random/new Random()）、世界查询全部内联，P1/P4/S1 这类回归恰好说明需要纯逻辑单测（trauma 状态机、fixQuirks、eatFood 消耗数都是可抽出的纯逻辑）。
- **与《开发规范》一致性**：注册模式、同步数据约束（核心类）、slowTick 兜底、INeko 复用等方面高度一致（好）；§8.6 移动仲裁与 §8 序列化器约束各有多处明面违规（见 M3/M4）。

## 数据速览

- entities 包共 **41 个 Java 文件 / 11,496 行**
- `NekoEntity`：**1,942 行、~61 个字段、~154 个方法签名**；最长方法 hurt() 71 行、giftItem() 57 行、equipArmors()/readAdditionalSaveData() 各 51 行；数字字面量粗计 262 处（去除坐标/数学常量后的"纯魔法值"约 30+，如 baka 0.002f、dojikko 0.005f、粒子距离 4096、日记掉率 0.30f、无伤阈值 1280）
- 职责域关键词频次（NekoEntity 内）：hatred×49、diary×44、inventory×34、animation×33、genome/breeding×32、totem×16 → ≥10 个可拆分职责域
- `NoelleMaidNeoEntity`：1,559 行；`trySendHatredMessage` 与父类重复度 ≈90%
- §8.6 违规（直接 `navigation.moveTo`）：**6 处**（NekoAttackGoal 1、Noelle 2、MoufletBoss 3）
- §8 违规（ITEM_STACK 同步序列化器）：**3 个 accessor / 2 个类**
- 死状态字段：2 个（hasBeenHealedByPlayer、eatenCatnip）；遗留 debug 日志：2 处；`new Random()` 绕过实体随机源：2 处
- 经字节码验证确认的 bug：2 个（Projectile.onHit 双重派发、eat() 内部 consume/shrink）
- 严重级别分布：严重 2、较高 5、中 7、低 4 组（合计 18 项）
