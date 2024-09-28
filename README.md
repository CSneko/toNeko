# toNeko
**[简体中文](README.md)** | [English](README_en.md)

![break with optifine](https://wsrv.nl/?url=https%3A%2F%2Fimages.teamresourceful.com%2Fu%2F8vCLgK.svg&n=-1)
## 简单的介绍
喵喵喵~
### 下载:
[插件版](https://modrinth.com/plugin/toneko/)
[模组版](https://modrinth.com/mod/tonekomod/)
## 使用方法
你只需要放入服务器/客户端的`plugins`或`mods`文件夹就可以了喵

模组版需要的前置mod(一定要加哦): [ctlib](https://modrinth.com/mod/ctlibmod)，[geckolib](https://modrinth.com/mod/geckolib)

插件版需要的前置插件: [ctlib](https://modrinth.com/plugin/ctlib)

**目前插件版暂停开发了哦**

## 小提示
如果你遇到了部分语言与你的实际语言不同,只需要修改[配置文件](#配置)中的`language`就可以了哦

## 提供支持的版本(但不代表它不能在其它版本运行哦):
- Fabric 1.21
- Spigot/Paper/Folia 1.17~最新
## 联动
联动模组: [luckperms](https://luckperms.net/)
## 如何变猫娘
想要变成猫娘的话呢很简单，你只需要做一个猫猫收集器，然后和猫猫们贴贴，能量满了后就会掉落一瓶猫娘药水，直接喝下就好了喵

如果需要成为其它猫娘（当然你自己也可以哦）的主人的话呢，只需要执行`/toneko player <玩家名称>`就好了喵，前提是对方得是猫娘哦（Tips：一个猫娘允许拥有多个主人）
## 快捷键
toNeko的一些功能可以通过快捷键来使用喵，默认为以下：
- `K`：骑乘附近的生物（和执行`/neko ride`作用同等）
- `I`：躺下（字面意思）
- `O`：趴下（也是字面意思）
- `J`：打开XP设置界面

这些快捷键都有对应的命令实现的（我是绝对不会告诉你我是直接调用的命令）
## 猫娘NPC
toNeko为了在单人模式下有的玩，所以加了一些猫娘实体喵，你可以在野外遇见她们，当然也可以选择直接生成

通过对着猫娘shift+右键可以打开互动菜单，不同猫娘的互动菜单可能有些许不同，有效距离为16格喵，如果走远了再点按钮就无效哦

每只猫娘刷新出来后皮肤可能不同喵，目前内置的皮肤不多喵，不过可以通过资源包来添加新的皮肤，至于怎么添加呢，这个等皮肤功能完善后我会专门为它写文档的喵，所以，敬请期待吧喵~^_^~

猫娘会有不同的变种喵，其刷新地点也可能不同喵。如果你想要把你自己添加进去的话呢，可以直接告诉我哦，我尽量会做的喵。当然还有个特殊变种，它只会刷新在toNeko生日（9月26日）这一天喵，至于是什么呢，你可以自己去看看喵
## 饰品
toNeko加了几件饰品，你可以在创造模式物品栏看到它们，当然啦，这些都是可以合成的喵。饰品都可以直接穿在盔甲栏上喵，如果你加了[trinkets](https://modrinth.com/mod/trinkets)的话呢，放在饰品栏也可以喵。
## 命令&权限（默认全部拥有）
```yaml
# 如果想要知道怎么用呢,请在每个命令后加help就可以了哦,例如/toneko help
# 给主人用的命令 (权限为command.toneko.xxx)
/toneko
# 给猫娘用的命令(权限为command.neko.xxx)
/neko
# 给管理员用的命令(权限为command.tonekoadmin.xxx)
/tonekoadmin
# 设置你的XP(权限为command.quirk.xxx)
/quirk
```
## 配置
虽然大部分情况呢你可能不需要配置文件,但是有时候可以用它来自定义体验喵.
### 主要配置文件:`config.yml`
#### 路径:
Fabric/Quilt: `config/toneko.yml`
Spigot/Paper: `plugins/toNeko/config.yml`

怎么编辑我就不详细说了哦,注释是有的呢.


就这些啦.
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
