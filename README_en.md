# toNeko
[简体中文](README.md) | [English](README_en.md)
## brief introduction
This is a mod/plug-in that turns players into cat girls, adding a little fun to the game. Supports Folia

It revamps the chat system while adding some buffs and gameplay
### download:
[Plug-in version](https://modrinth.com/plugin/toneko/)
[Mod version](https://modrinth.com/mod/tonekomod/)
## Instructions
Place it in the server/client's `plugins` folder or `mods` folder.

Pre-mods required for the modding version: [ctlib](https://modrinth.com/mod/ctlibmod), [geocklib](https://modrinth.com/mod/geckolib)

Pre-plugin required for the plug-in version: [ctlib](https://modrinth.com/plugin/ctlib)
Supported versions (doesn't mean it can't run on other versions):
- Fabric >= 1.20
- Spigot/Paper/Folia 1.16~Latest
### Linkage
Linkage mods: [luckperms](https://luckperms.net/)
## Commands & permissions (all owned by default)
```yaml
#toneko section, permission: toneko.command.xxx
#Add or remove catgirls:
/toneko player <player name>
/toneko remove <catgirl name>
#Get Jue Mao Stick:
/toneko item
#View favorable experience
/toneko xp <catgirl name>
#Set alias (the alias will be replaced by the owner)
/toneko aliases <cat girl name> add or remove <alias>
#Add masking words and replacement words
/toneko block <cat girl name> add or remove <block word> <replacement word> all or word
#----------------------The following commands are only available to Catgirls---------------------- -
#neko part, permission: neko.command.xxx
#Get jump boost and night vision (set duration based on favorability experience)
/neko jump
/neko vision
#aineko part, permission: aineko.command.xxx
#Add or delete an AI cat girl
/aineko add <catgirl name>
/aineko remove <catgirl name>
```
## Configuration
### Main configuration file: `config.yml`
#### Path:
Fabric/Quilt: `ctlib/toneko/config.yml`
Spigot/Paper: `plugins/toNeko/config.yml`
```yaml
#Language option (supports zh_cn, en_us), you can customize the language, see https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md for details
language: zh_cn
#Use the client language. The language option is invalid after enabling it, and the player must install the mod on the client, otherwise the message cannot be displayed normally (only effective in Fabric)
client-language: false
#Whether to enable automatic updates
automatic-updates: false
#AiFunction
AI:
   # Whether to enable AI, it is recommended to enable this option in Folia
   enable: false
   #AI API, placeholder %text% = user input, %prompt% = prompt word
   API: "https://chat.ai.crystalneko.online?t=%text%&&p=%prompt%"
   #Prompt word, please do not enter the && symbol. Placeholder %name% = cat girl name, %owner% = owner
   prompt: "You are a cute cat girl, your name is %name%, and your owner is %owner%"
```
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)