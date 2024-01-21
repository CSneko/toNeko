# toNeko
[简体中文](README.md) | [English](README_en.md)
## 简单的介绍
喵喵喵~
### 下载:
[插件版](https://modrinth.com/plugin/toneko/)
[模组版](https://modrinth.com/mod/tonekomod/)
## 使用方法
放入服务器/客户端的`plugins`文件夹或`mods`文件夹。

模组版需要的前置mod: [ctlib](https://modrinth.com/mod/ctlibmod),[geocklib](https://modrinth.com/mod/geckolib)

插件版需要的前置插件: [ctlib](https://modrinth.com/plugin/ctlib)

提供支持的版本(不代表它不能在其它版本运行):
- Fabric >= 1.20
- Spigot/Paper/Folia 1.16~最新
### 联动
联动模组: [luckperms](https://luckperms.net/)
## 命令&权限（默认全部拥有）
```yaml
#toneko部分，权限:toneko.command.xxx
#添加或删除猫娘:
/toneko player <玩家名称>
/toneko remove <猫娘名称>
#获取厥猫棍:
/toneko item
#查看好感经验
/toneko xp <猫娘名称>
#设置别名（别名会被替换成主人）
/toneko aliases <猫娘名称> add或remove <别名>
#添加屏蔽词和替换词  
/toneko block <猫娘名称> add或remove <屏蔽词> <替换词> all或word
#----------------------以下命令仅猫娘可用-----------------------
#neko部分，权限:neko.command.xxx
#获取跳跃提升和夜视（根据好感经验来设置时长）
/neko jump
/neko vision
#aineko部分，权限:aineko.command.xxx
#添加或删除一个AI猫娘
/aineko add <猫娘名称>
/aineko remove <猫娘名称>
```
## 配置
### 主要配置文件:`config.yml`
#### 路径:
Fabric/Quilt: `ctlib/toneko/config.yml`
Spigot/Paper: `plugins/toNeko/config.yml`
```yaml
#语言选项（支持 zh_cn,en_us）,可自定义语言，详细查看 https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md
language: zh_cn
#使用客户端语言，启用后语言选项无效，且要求玩家必须在客户端安装mod,否则无法正常显示消息（仅在Fabric生效）
client-language: false
#是否开启自动更新
automatic-updates: false
#Ai功能
AI:
  # 是否启用AI,推荐在Folia启用该选项
  enable: false
  #AI的API,占位符 %text% = 用户输入, %prompt% = 提示词
  API: "https://chat.ai.crystalneko.online?t=%text%&&p=%prompt%"
  #提示词，请勿输入&&符号 占位符 %name% = 猫娘名称, %owner% = 主人
  prompt: "你是一只可爱的猫娘，你的名字是%name%，你的主人是%owner%"
```
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
