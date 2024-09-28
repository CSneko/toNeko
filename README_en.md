# toNeko
[简体中文](README.md) | **[English](README_en.md)**

![break with optifine](https://wsrv.nl/?url=https%3A%2F%2Fimages.teamresourceful.com%2Fu%2F8vCLgK.svg&n=-1)
## Brief introduction
Meow meow meow~
### Download:
[Plugin version](https://modrinth.com/plugin/toneko/)
[Module version](https://modrinth.com/mod/tonekomod/)
## How to use
You only need to put it in the `plugins` or `mods` folder of the server/client.

Pre-mods required for the mod version (must be added): [ctlib](https://modrinth.com/mod/ctlibmod), [geckolib](https://modrinth.com/mod/geckolib)

Pre-plugin required for the plugin version: [ctlib](https://modrinth.com/plugin/ctlib)

**Currently the plugin version is temporarily suspended**
## Tips
If you found that some text isn't your language, just modify [the config file](#configuration) to your language.

## Supported versions (but it does not mean that it cannot run on other versions):
- Fabric 1.21
- Spigot/Paper/Folia 1.17~latest
## How to become a neko
If you want to become a neko, it's very simple. You just need to make a neko collector and then get close to cats. When the energy is full, a bottle of neko potion will drop. Just drink it.

If you need to become the owner of another neko (of course you can do it yourself), just execute `/toneko player <player name>`. The prerequisite is that the other party must be a neko (Tips: a neko can have multiple owners)
## Linkage
Linkage mods: [luckperms](https://luckperms.net/)
## Shortcuts
Some functions of toNeko can be used by shortcuts, the default is as follows:
- `K`: Ride nearby creatures (same as executing `/neko ride`)
- `I`: Lie down (literally)
- `O`: Lie down (also literally)
- `J`: Open quirks settings interface

These shortcuts have corresponding commands (I will never tell you that I call the command directly)
## Neko NPC
toNeko has added some neko to make it more playable in single-player mode. You can meet them in the wild, or you can choose to spawn them directly

You can open the interactive menu by shift+right-clicking on the neko. The interactive menus of different neko may be slightly different. The effective distance is 16 grids. If you walk far away and click the button, it will be invalid.

Each neko may have a different skin after refreshing. There are not many built-in skins at present, but new skins can be added through resource packs. As for how to add them, I will write a document specifically for it after the skin function is perfected, so please stay tuned~^_^~

Neko will have different variants, and their refresh locations may also be different. If you want to add your own, you can tell me directly and I will try my best to do it. Of course, there is also a special variant that will only refresh on toNeko’s birthday (September 26). As for what it is, you can go and see it yourself.
## Accessories
toNeko has added several accessories, which you can see in the creative inventory. Of course, these are all craftable. Accessories can be worn directly on the armor slot, and if you have added [trinkets](https://modrinth.com/mod/trinkets), they can also be placed in the accessories slot.
## Commands & permissions (all owned by default)
```yaml
# If you want to know how to use it, please add help after each command, for example /toneko help
# Commands for the owner (permission is command.toneko.xxx)
/toneko
# Commands for neko (permission is command.neko.xxx)
/neko
# Commands for administrators (permission is command.tonekoadmin.xxx)
/tonekoadmin
  # Set your quirks (permissions are command.quirk.xxx)
/quirk
```
## Configuration
Although you may not need a configuration file in most cases, sometimes you can use it to customize your experience.
### Main configuration file: `config.yml`
#### Path:
Fabric/Quilt: `config/toneko.yml`
Spigot/Paper: `plugins/toNeko/config.yml`

I won't go into details about how to edit it, there are comments.

That's all.
## bStats:
![bStats](https://bstats.org/signatures/bukkit/toneko.svg)
