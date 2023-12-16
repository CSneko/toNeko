# toNeko
[简体中文](README.md) | [English](README_en.md)
## 简单的介绍
这是一个可以在服务器中把玩家变成猫娘的插件，为服务器增添一点乐趣,Fabric版本目前尚完善,并且支持Folia
### 聊天
- 添加前缀，修改格式
- 别名，屏蔽词与替换词
### 撅猫娘
- 一级&二级撅猫棍
- 被撅音效
- 被撅效果
- 好感经验
### 提升
- 跳跃提升与夜视
### 下载稳定版 : [Modrinth](https://modrinth.com/plugin/toneko/)
## 使用方法
放入服务器的`plugins`文件夹或`mods`文件夹(Fabric1.20.2)。如果你没有安装[ctLib](https://modrinth.com/plugin/ctlib)前置插件，请安装它

提供支持的版本:
- Fabric >= 1.20
- Spigot/Paper/Folia 1.16~1.20.2

如果你发现没有成功启动的话，可以尝试重启服务器/客户端，或检查服务器/客户端是否安装了ctLib插件（如果你运行在插件端,正常情况下会自动为你安装好)
### 联动
联动模组: [luckperms](https://luckperms.net/)
## 命令&权限（默认全部拥有）
```yaml
#获取帮助
/toneko help
#将玩家变成猫娘:
/toneko player <玩家名称>        #toneko.command.player
#获取厥猫棍(该物品在击败猫娘时会有特殊死亡提示,并且能够增加或减少好感经验):
/toneko item                   #toneko.command.item
#删除猫娘（危险操作，需要二次确认）
/toneko remove <猫娘名称>       #toneko.command.remove
#查看好感经验
/toneko xp <猫娘名称>           #toneko.command.xp
#设置别名（别名会被替换成主人）
/toneko aliases <猫娘名称> add或remove <别名>    #toneko.command.aliases
#添加屏蔽词和替换词  
/toneko block <猫娘名称> add或remove <屏蔽词> <替换词> all或word  #toneko.command.block
#----------------------以下命令仅猫娘可用-----------------------
#获取跳跃提升（根据好感经验来设置时长）
/neko jump                    #toneko.command.jump
#获取夜视（根据好感经验来设置时长）
/neko vision                  #toneko.command.vision
```
## 配置
### 主要配置文件:`config.yml`
```yaml
#语言选项（支持 zh_cn,en_us）
language: zh_cn
#是否开启自动更新
automatic-updates: false
#是否接入在线网站(统计信息会被上传，详情见 https://w.csk.asia/toneko)
online: true
```
## 插件网站
这是对该插件做的一个简易的网站，搭建在cloudflare pages上，用于查询统计信息(已停用)

网站链接: https://w.csk.asia/toneko
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
