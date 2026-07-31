# toNeko Bukkit

**[简体中文](#简体中文)** | **[English](#english)**

![bStats](https://bstats.org/signatures/bukkit/toneko.svg)

---

## 简体中文

### 简介
toNeko Bukkit 是 [toNeko](https://github.com/CSNeko/toNeko) 的 Paper/Folia 插件版喵~ 无需客户端模组即可体验猫娘变身、AI 聊天、特质系统、猫薄荷种植等核心玩法。

### 下载
[Modrinth](https://modrinth.com/mod/tonekomod/) | [GitHub Releases](https://github.com/CSNeko/toNeko/releases)

### 支持平台
Paper / Purpur / Folia 1.21.x

### 前置
- 无必装前置
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)（可选）— PAPI 变量支持
- [LuckPerms](https://luckperms.net/)（可选）— 权限管理

### 与模组版的区别

| 特性 | 模组版 | Bukkit |
|------|:-----:|:-----:|
| 猫娘变身 / 能量系统 / AI 聊天 | ✅ | ✅ |
| 特质系统 / 猫薄荷种植 | ✅ | ✅ |
| 猫猫收集器 / 猫娘药水 / 主猫关系 | ✅ | ✅ |
| 聊天屏蔽词 & 别名 / 区域&全局聊天 / 猫娘语 | ✅ | ✅ |
| 猫娘 NPC / Boss / 遗传学 / 饰品 / 皮肤 | ✅ | ❌ |

### 安装
1. 下载 `toneko-bukkit-x.x.x.jar` → 放入 `plugins/`
2. 重启服务器，配置自动生成于 `plugins/toNeko/`

### 变成猫娘
1. 制作**猫猫收集器**（或 `/tonekoadmin give collector <玩家>`）
2. 靠近猫娘玩家（3格内），收集器充能至 5000 后掉落**猫娘药水**
3. 喝下变身！已是猫娘则恢复 100 能量

**主猫关系**：`/toneko player <玩家>` 发送请求，对方 `/toneko accept <玩家>` 接受。一个猫娘可有多个主人。

### 猫娘能量
- 最大能量：1000 + 等级 × 10
- 每秒恢复：等级 × (0.5 + 等级×0.02) × 0.1
- 附近猫娘加成：3格内每只猫娘额外恢复
- 技能消耗 100 能量/次

```
/neko jump     # 跳跃强化
/neko speed    # 速度提升
/neko vision   # 夜视
/neko ride     # 骑乘附近生物
```

### 物品

| 物品 | 材质 | 用途 |
|------|------|------|
| 猫猫收集器 | 末影之眼 | 近猫娘充能至 5000 → 掉落药水 |
| 猫娘药水 | 药水 | 变身 / 恢复 100 能量 |
| 猫薄荷 | 绿色染料 | 食用恢复 30 能量（带副作用） |
| 猫薄荷种子 | 小麦种子 | 种耕地，成熟收猫薄荷+种子 |

管理员给予：`/tonekoadmin give <collector|potion|catnip|catnip_seed> <玩家>`

### AI 聊天
支持 Google / OpenAI / SiliconFlow / Groq 等。
- `/neko chat <消息>` — 主动发起聊天
- `/tonekoadmin ai list|switch|config|test` — AI 管理

自动触发：区域模式每条消息触发，全局模式以配置前缀开头触发。支持 TTS（需模组客户端）。

### 聊天系统
- 猫娘发言自动插入 "nya" 等猫娘语
- 主人名称 → "主人" 自动替换
- `/toneko block <猫娘> add|remove ...` — 屏蔽词管理
- `/toneko aliases <猫娘> add|remove ...` — 别名管理
- 自定义聊天格式（`chatFormat`）
- 全局 / 区域（64格）聊天模式

### 命令

**权限：** 所有玩家命令默认 `true`，管理员命令默认 OP。

#### /toneko（主人命令）
```yaml
help / player <玩家> / accept <玩家> / deny <玩家> / remove <玩家>
block <猫娘> add|remove ...       # 屏蔽词
aliases <猫娘> add|remove ...     # 别名
xp <猫娘>                          # 好感度
gui                               # 管理界面（需模组客户端）
```

#### /neko（猫娘命令）
```yaml
help / chat <消息> / level / gui
ride / jump / speed / vision       # 技能（消耗100能量）
nickname <昵称> / removeNickname
lore <描述>
```

#### /tonekoadmin（管理员命令）
```yaml
help / reload [config|data]
set <玩家>                                         # 切换猫娘状态
neko <玩家> [setBaby|setEnergy|setNickName|setAge] ...
give <collector|potion|catnip|catnip_seed> <玩家>
config get|set <key> <value>
data loadedCount|allCount
ai list|switch|config|test ...
```

#### /quirk（特质命令）
```yaml
help / list / add <ID> / remove <ID> / gui
```

### PlaceholderAPI 变量
```
%toneko_is_neko%       # 是否猫娘
%toneko_level%         # 等级
%toneko_energy%        # 当前能量
%toneko_max_energy%    # 最大能量
%toneko_nickname%      # 昵称
%toneko_age%           # 年龄
%toneko_owners%        # 主人数量
```

### 模组客户端增强
安装 Fabric/NeoForge 版模组的玩家额外获得：GUI 界面、TTS 语音、聊天历史同步、客户端侧翻译。（请确保与服务端版本号一致）

### 配置文件
```
plugins/toNeko/
├── config.json    # AI、聊天格式、欢迎消息
└── data/          # 猫娘数据
```

### 交流
- [Discord](https://discord.gg/hQ6Mm7wtt4)
- [GitHub Issues](https://github.com/CSNeko/toNeko/issues)

---

## English

### Introduction
toNeko Bukkit is the Paper/Folia plugin edition of [toNeko](https://github.com/CSNeko/toNeko). Experience neko transformation, AI chat, quirk system, catnip farming, and more — no modded client required.

### Download
[Modrinth](https://modrinth.com/mod/tonekomod/) | [GitHub Releases](https://github.com/CSNeko/toNeko/releases)

### Supported Platforms
Paper / Purpur / Folia 1.21.x

### Dependencies
- No required dependencies
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional) — PAPI placeholders
- [LuckPerms](https://luckperms.net/) (optional) — permission management

### Differences from Mod Version

| Feature | Mod | Bukkit |
|---------|:--:|:-----:|
| Neko transformation / Energy / AI chat | ✅ | ✅ |
| Quirk system / Catnip farming | ✅ | ✅ |
| Collector / Potion / Owner-Neko relationship | ✅ | ✅ |
| Word blocking & aliases / Area & global chat / Pet phrases | ✅ | ✅ |
| Neko NPCs / Boss / Genetics / Accessories / Skins | ✅ | ❌ |

### Installation
1. Download `toneko-bukkit-x.x.x.jar` → put into `plugins/`
2. Restart server. Config auto-generated at `plugins/toNeko/`

### Becoming a Neko
1. Craft a **Neko Collector** (or `/tonekoadmin give collector <player>`)
2. Stay near neko players (within 3 blocks). Collector charges to 5000 → drops **Neko Potion**
3. Drink it! If already a neko, restores 100 energy.

**Owner-Neko relationship**: `/toneko player <player>` sends a request. Target uses `/toneko accept <player>`. A neko can have multiple owners.

### Neko Energy
- Max energy: 1000 + level × 10
- Regen per second: level × (0.5 + level×0.02) × 0.1
- Nearby neko bonus: each neko within 3 blocks adds extra regen
- Skill cost: 100 energy per use

```
/neko jump     # Jump boost
/neko speed    # Speed boost
/neko vision   # Night vision
/neko ride     # Ride nearby entity
```

### Items

| Item | Material | Purpose |
|------|----------|---------|
| Neko Collector | Eye of Ender | Charges near nekos to 5000 → drops potion |
| Neko Potion | Potion | Transform / restore 100 energy |
| Catnip | Green Dye | Eat to restore 30 energy (with side effects) |
| Catnip Seeds | Wheat Seeds | Plant on farmland, harvest for catnip + seeds |

Admin give: `/tonekoadmin give <collector|potion|catnip|catnip_seed> <player>`

### AI Chat
Supports Google / OpenAI / SiliconFlow / Groq and more.
- `/neko chat <message>` — initiate AI chat
- `/tonekoadmin ai list|switch|config|test` — AI management

Auto-trigger: every message in area mode, or prefixed messages in global mode. TTS supported (modded client required).

### Chat System
- Auto "nya" insertion for neko players
- Owner names → "Master" replacement
- `/toneko block <neko> add|remove ...` — word blocking
- `/toneko aliases <neko> add|remove ...` — alias management
- Customizable chat format (`chatFormat`)
- Global / Area (64 blocks) chat modes

### Commands

**Permissions:** All player commands default `true`, admin commands default OP.

#### /toneko (Owner Commands)
```yaml
help / player <player> / accept <player> / deny <player> / remove <player>
block <neko> add|remove ...       # Word blocking
aliases <neko> add|remove ...     # Alias management
xp <neko>                          # Favor level
gui                               # Management screen (modded client required)
```

#### /neko (Neko Commands)
```yaml
help / chat <message> / level / gui
ride / jump / speed / vision       # Skills (cost 100 energy)
nickname <name> / removeNickname
lore <description>
```

#### /tonekoadmin (Admin Commands)
```yaml
help / reload [config|data]
set <player>                                        # Toggle neko status
neko <player> [setBaby|setEnergy|setNickName|setAge] ...
give <collector|potion|catnip|catnip_seed> <player>
config get|set <key> <value>
data loadedCount|allCount
ai list|switch|config|test ...
```

#### /quirk (Quirk Commands)
```yaml
help / list / add <ID> / remove <ID> / gui
```

### PlaceholderAPI Variables
```
%toneko_is_neko%       # Is neko
%toneko_level%         # Level
%toneko_energy%        # Current energy
%toneko_max_energy%    # Max energy
%toneko_nickname%      # Nickname
%toneko_age%           # Age
%toneko_owners%        # Owner count
```

### Modded Client Perks
Players with the Fabric/NeoForge mod get: GUI screens, TTS voice, chat history sync, client-side translations. （Make sure than your mod version is same to the plugin version）

### Configuration
```
plugins/toNeko/
├── config.json    # AI, chat format, welcome message
└── data/          # Neko data
```

### Community
- [Discord](https://discord.gg/hQ6Mm7wtt4)
- [GitHub Issues](https://github.com/CSNeko/toNeko/issues)

---

<p align="center"><sub>可爱即是正义! | Cuteness is justice! 🐾</sub></p>
