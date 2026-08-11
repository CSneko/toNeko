# toNeko
**[简体中文](README.md)** | [English](README_en.md)

![break with optifine](https://wsrv.nl/?url=https%3A%2F%2Fimages.teamresourceful.com%2Fu%2F8vCLgK.svg&n=-1)

![Modrinth Downloads](https://img.shields.io/modrinth/dt/tonekomod)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/CSneko/toNeko)
![Discord](https://img.shields.io/discord/1263358377606516867)

## 简单的介绍
喵喵喵~

toNeko 是一个将你变成猫娘的 Minecraft 模组喵!除了变成猫娘,还有 AI 聊天、遗传学、特质系统、猫薄荷种植、Boss 战斗等丰富的内容等你来探索喵~

喜欢这个模组记得点个 star 哦~

### 下载:
[模组版](https://modrinth.com/mod/tonekomod/)

## 支持的版本:
- Fabric 1.21.1
- NeoForge 1.21.1

## 前置mod
- [geckolib](https://modrinth.com/mod/geckolib)（必装）
- [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api)（仅 NeoForge 需要）

可选前置：
- [trinkets](https://modrinth.com/mod/trinkets)（提供饰品栏位支持）
- [luckperms](https://luckperms.net/)（权限管理）
- [patchouli](https://modrinth.com/mod/patchouli)（提供游戏内指南书）

## 特色功能
- 🧬 **遗传学系统** — 孟德尔遗传机制，猫娘拥有可遗传的基因喵！速度、体型、攻击、抗性……共 11 个基因位点，连 15 种萌属性（傲娇、病娇、天然呆、雌小鬼……）都被编码成了基因，可以通过基因编辑器修改，猫娘之间交配还会遗传给后代喵
- 🤖 **AI 猫娘** — 支持 12 家 AI 服务商（Google、OpenAI、DeepSeek、Groq、SiliconFlow、本地 Ollama 等）。AI 猫娘不只会聊天：她们会主动找你说话、猫娘之间互相聊天、写日记、给你物品，还支持 TTS 语音喵~
- 🎀 **LoliHead** — 当玩家体型变小时自动调整头部大小，让萝莉猫娘更可爱喵
- ⚔️ **战斗系统** — 哈气能量爆发（AoE 伤害 + 连击 HUD）、火箭筒（爆炸弹/闪电弹）、御剑飞行、九命护符，猫娘也能打架喵！
- 🐾 **猫爪攀墙** — 猫娘玩家可以像猫一样贴着墙壁爬上爬下喵
- 🌿 **猫薄荷** — 可以种植、收获的猫薄荷作物，还能做成猫薄荷三明治喵
- 🎭 **特质系统** — 摸摸、水晶猫娘、杂鱼等特质，通过 `/quirk` 命令设置喵
- 🏆 **成就系统** — 变身、驯服、攀墙、哈气连击、等级挑战……共 17 个成就等你达成喵
- 🔧 **猫猫聚合台** — 专属的合成工作台，用来制作模组中的各种物品喵
- 📦 **Moe 资源包** — 可爱の猫娘翻译包~
- 🔌 **EMI 集成** — 在 EMI 中查看猫猫聚合台的合成配方喵

## AI 设置
AI 是 toNeko 的重头戏喵！详细的图文教程请看 [AI 设置文档](docs/AI.md)，这里简单介绍一下：

### AI 动作
AI 猫娘不只会聊天，她们还会在对话中执行动作喵（一共有 29 种）：
- 走向你、跟随你、拥抱你、求摸头、呼噜、蹭蹭、梳毛、陪你玩、分享食物……
- 给你物品（背包里没有时会消耗猫能虚拟生成）、把东西存进附近的箱子、装备物品……
- 写日记（会记录当时的天气、心情、群系喵），还能把日记做成书送给你
- 认你为主、解除认主、增减好感度（都有冷却，防止刷好感喵）
- 主动发起交配（要你同意才行！）

### 主动发言
在配置中开启后，猫娘会在空闲时主动找你说喵：主人来了主动打招呼、夜晚关心你早点睡、无聊了找你聊天、和其他猫娘闲聊、死亡时留下遗言、看到你死亡时会担心你……

也可以直接使用管理员命令管理服务商：`/tonekoadmin ai list`、`/tonekoadmin ai switch`、`/tonekoadmin ai test` 喵


## 小提示
如果你遇到了部分语言与你的实际语言不同，可以在游戏内通过 Mod Menu 的配置界面修改语言设置哦

## 如何变猫娘
想要变成猫娘的话呢很简单，你只需要做一个猫猫收集器，然后和猫猫们贴贴，能量满了后就会掉落一瓶猫娘药水，直接喝下就好了喵

如果需要成为其它猫娘（当然你自己也可以哦）的主人的话呢，只需要执行 `/toneko player <玩家名称>` 就好了喵，前提是对方得是猫娘哦（Tips：一个猫娘允许拥有多个主人）

## 快捷键
toNeko 的一些功能可以通过快捷键来使用喵，默认为以下：
- `K`：骑乘附近的生物（和执行 `/neko ride` 作用同等）
- `I`：躺下（字面意思）
- `O`：趴下（也是字面意思）
- `J`：打开特质设置界面

这些快捷键都有对应的命令实现的（我是绝对不会告诉你我是直接调用的命令）

## 猫娘NPC
toNeko 为了在单人模式下有的玩，所以加了一些猫娘实体喵，你可以在野外遇见她们，当然也可以选择直接生成

通过对着猫娘 shift+右键可以打开互动菜单，不同猫娘的互动菜单可能有些许不同，有效距离为 16 格喵，如果走远了再点按钮就无效哦

每只猫娘刷新出来后皮肤可能不同喵，目前内置的皮肤不多喵，不过可以通过资源包来添加新的皮肤，至于怎么添加呢，这个等皮肤功能完善后我会专门为它写文档的喵，所以，敬请期待吧喵~^_^~

猫娘会有不同的变种喵，包括冒险猫娘、幽灵猫娘、水晶猫娘、战斗猫娘、女仆猫娘等，它们的刷新地点也有所不同喵。如果你想要把你自己添加进去的话呢，可以直接告诉我哦，我尽量会做的喵。当然还有个特殊变种——水晶猫娘，它只会刷新在 toNeko 生日（9 月 26 日）这一天喵~

每只猫娘都有等级与好感度，等级会随着互动、战斗、探索等提升，让她们越来越强喵。聊过 AI 或有主人的猫娘，死亡后不会就这样消失，而是会化作幽灵猫娘继续陪伴在你身边喵……（会说话的那种）

除此之外，还有强大的 **Mouflet Boss** 等你来挑战喵！它会偷走你的物品、飞行攻击，还会撒娇、魅惑、抱着你飞上天，可不是那么容易对付的喵！不过如果实力足够，也可以使用契约收服它，然后骑着它飞行喵~

## 饰品
toNeko 加了几件饰品（猫耳、猫尾、猫爪等），你可以在创造模式物品栏看到它们，当然啦，这些都是可以合成的喵。饰品都可以直接穿在盔甲栏上喵，如果你加了 [trinkets](https://modrinth.com/mod/trinkets) 的话呢，放在饰品栏也可以喵。

## 命令&权限（默认全部拥有）
```yaml
# 如果想要知道怎么用呢，请在每个命令后加help就可以了哦，例如/toneko help
# 给主人用的命令 (权限为 command.toneko.xxx)
/toneko
# 给猫娘用的命令(权限为 command.neko.xxx)
/neko
# 给管理员用的命令(权限为 command.tonekoadmin.xxx)
/tonekoadmin
# 设置你的特质(权限为 command.quirk.xxx)
/quirk
# 查看遗传信息(权限为 command.genetics.xxx)
/genetics
```

## 文档
- [AI 设置](docs/AI.md) — AI 服务商、模型、Key 获取教程
- [遗传学 API](docs/genetics_api.md)
- [toNeko 在线 API](docs/TONEKO_ONLINE_API.md)

## 交流&支持渠道
- [Discord](https://discord.gg/hQ6Mm7wtt4)

## 联动
联动模组: [luckperms](https://luckperms.net/)

就这些啦.