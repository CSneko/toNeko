# toNeko
[简体中文](README.md) | [English](README_en.md)
## brief introduction
This is a plug-in that can turn players into catgirls in the server, adding a little fun to the server.Supports Folia.
### chat
- Add prefix and modify format
- Aliases, masking words and replacement words
### Pussy cat girl
- Level 1 & Level 2 Cat Stick
- Being poked sound effects
- Effect of being poked
- Favorable experience
### promote
- Jump boost and night vision
### Download stable version: [Modrinth](https://modrinth.com/plugin/toneko/)
## Instructions
Put it into the server's `plugins` folder or `mods` folder (Fabric1.20.2). If you do not have the [ctLib](https://modrinth.com/plugin/ctlib) pre-plugin installed, please install it

Supported versions:
- Fabric >=1.20
- Spigot/Paper/Folia 1.16~1.20.2

If you find that it does not start successfully, you can try to restart the server/client, or check whether the server/client has the ctLib plug-in installed (if you run it on the plug-in side, it will be installed automatically for you under normal circumstances)
### Linkage
Linkage module: [luckperms](https://luckperms.net/)
## Commands & permissions (all owned by default)
```yaml
#GetHelp
/toneko help
#Turn players into catgirls:
/toneko player <player name> #toneko.command.player
#Get the Jue Mao Stick (this item will have a special death prompt when defeating the cat lady, and can increase or decrease the favorability experience):
/toneko item #toneko.command.item
#Delete Cat Girl (dangerous operation, requires secondary confirmation)
/toneko remove <catgirl name> #toneko.command.remove
#View favorable experience
/toneko xp <catgirl name> #toneko.command.xp
#Set alias (the alias will be replaced by the owner)
/toneko aliases <cat girl name> add or remove <alias> #toneko.command.aliases
#Add masking words and replacement words
/toneko block <cat girl name> add or remove <block word> <replacement word> all or word
#----------------------The following commands are only available to Catgirls---------------------- -
#Get jump boost (set duration based on favorability experience)
/neko jump #toneko.command.jump
#Get night vision (set duration based on favorability experience)
/neko vision #toneko.command.vision
```
## Configuration
### Main configuration file: `config.yml`
#### Path:
Fabric: `ctlib/toneko/config.yml`
Bukkit: `plugins/toNeko/config.yml`
```yaml
#Language options (support zh_cn, en_us)
language: zh_cn
#use client language（only Fabric）
client-language: false
#Whether to enable automatic updates
automatic-updates: false
#Whether to access the online website (statistics information will be uploaded, see https://w.csk.asia/toneko for details)
online: true
```
## Plug-in website
This is a simple website made for the plug-in, built on cloudflare pages, used to query statistical information (disabled)

Website link: https://w.csk.asia/toneko
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
