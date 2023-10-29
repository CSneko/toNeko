# toNeko
[English](README.md) | [简体中文](README_zh.md)
## 简单的介绍
这是一个可以在服务器中把玩家变成猫娘的插件，为你的服务器增添一点乐趣,Fabric版本目前尚完善
## 使用方法
放入服务器的`plugins`文件夹（Spigot） 或放入`mods`文件夹(Fabric1.20.2)

暂不支持Forge,Fabric版本暂不完善

如果你发现没有成功启动的话，可以尝试重启服务器/客户端，或检查服务器/客户端是否安装了[ctLib插件（如果你运行在插件端,正常情况下会自动为你安装好）](https://github.com/csneko/ctlib)
## 命令&权限（默认全部拥有）
```yaml
#获取帮助
/toneko help
#将玩家变成猫娘(这会献祭自己(kill)，请谨慎使用):
/toneko player <玩家名称>        #toneko.command.player
#获取厥猫棍(该物品在击败猫娘时会有特殊死亡提示,并且能够增加或减少好感经验):
/toneko item                   #toneko.command.item
#删除猫娘（危险操作，需要二次确认）
/toneko remove <猫娘名称>       #toneko.command.remove
#查看好感经验
/toneko xp <猫娘名称>           #toneko.command.xp
#设置别名（别名会被替换成主人）
/toneko aliases <猫娘名称> add或remove <别名>
#添加屏蔽词和替换词
/toneko block <猫娘名称> add或remove <屏蔽词> <替换词> all或word
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
这是对该插件做的一个简易的网站，搭建在cloudflare pages上，用于查询统计信息

网站链接: https://w.csk.asia/toneko
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
