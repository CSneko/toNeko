# toNeko
## 简单的介绍
这是一个可以在服务器中把玩家变成猫娘的插件，为你的服务器增添一点乐趣,Fabric版本目前尚完善
## 食用方法
丢进服务器的`plugins`文件夹即可（Spigot）

或放入`mods`文件夹(Fabric1.20.2)

暂不支持Forge

如果你不会开服，你可以[点击这里下载](https://w.csk.asia/res/scripts/)自动开服脚本(支持Windows和Linux)，脚本会自动为你下载插件，你什么都不需要做，只需要双击即可

如果你发现插件没有成功启动的话，可以尝试重启服务器，或检查服务器是否安装了[ctLib插件（正常情况下会自动为你安装好）](https://github.com/csneko/ctlib)
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
#是否开启自动更新
automatic-updates: true
```
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
