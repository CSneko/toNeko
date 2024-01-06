# 自定义语言
插件默认支持`简体中文(zh_cn)`和`English(en_us)`两种语言，该文档会教你如何添加自定义语言
## 配置
首先你需要打开插件目录，如果你使用mod加载器运行，那么它应该在`{你的游戏目录}/ctlib/toneko`，插件加载器则在`{服务器根目录}/plugins/toneko`

打开它，你应该会看到如下文件/文件夹:
```
config.yml  language
```
确保`language`文件夹里有`zh_cn.yml`或`en_us.yml`，如果没有则重启游戏

使用文本编辑器（推荐Visual Studio Code，Gedit，Vim）打开`config.yml`，编辑以下配置项:
```yaml
#语言选项（支持 zh_cn,en_us）,可自定义语言
language: custom
#使用客户端语言，启用后语言选项无效，且要求玩家必须在客户端安装mod,否则无法正常显示消息（仅在Fabric生效）
client-language: false
```
确保`language`为`custom`，`client-language`为`false`
## 创建语言文件
进入`language`文件夹，将`zh_cn.yml`或`en_us.yml`的名称改成`custom.yml`（如果两个都有只改一个就行）

接下来使用文本编辑器打开`custom.yml`,修改配置项后重启即可

例如我要修改`§c此命令只能由玩家执行`,那么我只需要将以下配置项进行修改即可:
```yaml
command:
  only-player: "§c这个命令只能有玩家执行哦，不支持命令方块哦"
```
那么现在使用命令方块或控制台执行时会提示`§c这个命令只能有玩家执行哦，不支持命令方块哦`

一点补充:

`§`符号为特殊字体符号，可以对文本的显示效果进行修改，例如：`§a`会使后面的字符为绿色,`§c`为红色`