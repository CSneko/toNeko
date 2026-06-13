# toNeko
**[简体中文](README.md)** | [English](README_en.md)

![break with optifine](https://wsrv.nl/?url=https%3A%2F%2Fimages.teamresourceful.com%2Fu%2F8vCLgK.svg&n=-1)

![Modrinth Downloads](https://img.shields.io/modrinth/dt/tonekomod)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/CSneko/toNeko)
![Discord](https://img.shields.io/discord/1263358377606516867)

## 简单的介绍
喵喵喵~

toNeko 是一个将你变成猫娘的 Minecraft 模组喵！除了变成猫娘，还有 AI 聊天、遗传学、特质系统、猫薄荷种植、Boss 战斗等丰富的内容等你来探索喵~

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

## 特色功能
- 🧬 **遗传学系统** — 孟德尔遗传机制，猫娘拥有可遗传的基因（如萌属性基因），可以通过基因编辑器修改喵
- 🤖 **AI 聊天** — 支持多种 AI 服务商（Google、OpenAI、SiliconFlow、Groq 等），让猫娘 NPC 可以和你聊天，还支持 TTS 语音喵
- 🎀 **LoliHead** — 当玩家体型变小时自动调整头部大小，让萝莉猫娘更可爱喵
- 🌿 **猫薄荷** — 可以种植、收获的猫薄荷作物，还能做成猫薄荷三明治喵
- 🔧 **猫猫聚合台** — 专属的合成工作台，用来制作模组中的各种物品喵
- 📦 **Moe 资源包** — 可爱の猫娘翻译包~
- 🔌 **EMI 集成** — 在 EMI 中查看猫猫聚合台的合成配方喵

## 使用方法
你只需要将模组放入服务器/客户端的 `mods` 文件夹就可以了喵

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

猫娘会有不同的变种喵，包括冒险猫娘、幽灵猫娘、水晶猫娘、战斗猫娘等，它们的刷新地点也有所不同喵。如果你想要把你自己添加进去的话呢，可以直接告诉我哦，我尽量会做的喵。当然还有个特殊变种——水晶猫娘，它只会刷新在 toNeko 生日（9 月 26 日）这一天喵~

除此之外，还有强大的 **Mouflet Boss** 等你来挑战喵！它会偷走你的物品、飞行攻击，可不是那么容易对付的喵！

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
```

## 交流&支持渠道
- [Discord](https://discord.gg/hQ6Mm7wtt4)

## 联动
联动模组: [luckperms](https://luckperms.net/)

就这些啦.

## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
