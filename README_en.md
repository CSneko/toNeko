# toNeko
[简体中文](README.md) | **[English](README_en.md)**

![break with optifine](https://wsrv.nl/?url=https%3A%2F%2Fimages.teamresourceful.com%2Fu%2F8vCLgK.svg&n=-1)

![Modrinth Downloads](https://img.shields.io/modrinth/dt/tonekomod)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/CSneko/toNeko)
![Discord](https://img.shields.io/discord/1263358377606516867)

## Brief Introduction
Meow meow meow~

toNeko is a Minecraft mod that turns you into a neko! Beyond becoming a catgirl, there's also AI chat, genetics, a quirk system, catnip farming, boss battles, and more waiting for you to explore nya~

If you like this mod, please give it a star~

### Download:
[Mod Version](https://modrinth.com/mod/tonekomod/)

## Supported Versions:
- Fabric 1.21.1
- NeoForge 1.21.1

## Required Mods
- [geckolib](https://modrinth.com/mod/geckolib) (required)
- [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api) (NeoForge only)

Optional:
- [trinkets](https://modrinth.com/mod/trinkets) (adds accessory slot support)
- [luckperms](https://luckperms.net/) (permission management)
- [patchouli](https://modrinth.com/mod/patchouli) (adds an in-game guide book)

## Features
- 🧬 **Genetics System** — Mendelian inheritance mechanics! Nekos have heritable genes: speed, body size, attack, resistance... 11 gene loci in total, and even 15 moe traits (tsundere, yandere, airhead, brat... ) are encoded as genes. You can edit them with the Gene Editor, and breeding passes them on to offspring nya~
- 🤖 **AI Nekos** — 12 AI providers supported (Google, OpenAI, DeepSeek, Groq, SiliconFlow, local Ollama, etc.). AI nekos don't just chat: they proactively talk to you, chat among themselves, write diaries, give you items, and even support TTS voice nya~
- 🎀 **LoliHead** — Automatically adjusts head size when the player's body shrinks, making loli nekos even cuter nya~
- ⚔️ **Combat System** — Neko Energy Burst (AoE damage + combo HUD), bazooka (explosive/lightning bombs), flying sword, Nine Lives Charm — nekos can fight too nya!
- 🐾 **Claw Climb** — Neko players can climb walls like a cat nya~
- 🌿 **Catnip** — Growable catnip crop that you can farm, harvest, and turn into catnip sandwiches nya~
- 🎭 **Quirk System** — Quirks like Caress, Crystal Neko, and Zako, settable via `/quirk` nya~
- 🏆 **Advancements** — Transform, tame, climb, hiss combos, level challenges... 17 advancements to unlock nya~
- 🔧 **Neko Aggregator** — A dedicated crafting workbench for making various items from the mod nya~
- 📦 **Moe Resource Pack** — Built-in Chinese translation fix pack, fixing translation issues for 25+ mods nya~
- 🔌 **EMI Integration** — View Neko Aggregator recipes directly in EMI nya~

## AI Setup
AI is the star of toNeko nya! For a full illustrated guide, see the [AI Setup docs](docs/AI_en.md). Here's a quick overview:

### Supported AI Providers
- `player2` — the simplest, recommended for beginners (just download the [Player2 client](https://player2.game/))
- `neko` — built-in Google proxy, easiest to configure
- `google` — free to use
- `openai`
- `deepseek`
- `claude` — Anthropic
- `groq` — free to use
- `siliconflow` — has free quotas
- `ollama` — local models, no API key required
- `openrouter` — multi-model gateway
- `mistral`
- `custom` — any OpenAI-compatible endpoint

If you're in mainland China without a proxy, use `player2`, `neko`, or `siliconflow` nya~

### AI Actions
AI nekos don't just chat — they perform actions during conversation nya (29 actions in total):
- Walk to you, follow you, hug you, ask for head pats, purr, nuzzle, groom, play with you, share food...
- Give you items (virtually generated with neko energy if they don't have them), store things in nearby chests, equip items...
- Write diaries (recording the weather, mood, and biome at the time nya), and gift you their diary as a written book
- Accept you as their owner, remove ownership, change affection (with cooldowns to prevent affection farming nya)
- Initiate mating on their own (but they need your consent!)

### Proactive Messages
Once enabled in config, nekos will proactively talk to you nya: greet you when their owner arrives, worry about you staying up late, chat with you when bored, chat with other nekos, leave last words when dying, and worry about you when you die...

### Neko-to-Neko Chat
When a neko mentions another neko's name in conversation, the named neko replies, forming a chat chain nya~ You can also tell a neko to go talk to another neko face to face, and watch from the sidelines nya

### TTS Voice
Optional feature — when enabled, neko speech is spoken aloud nya (currently via player2 voice)

### Show Thinking Process
When enabled, the AI's thinking process is displayed in-game. Disabling it does not affect AI output nya

### How to Configure
In the in-game toNeko config screen (Mod Menu), or via commands:
```
/tonekoadmin config set ai.enable true
/tonekoadmin config set ai.service neko
/tonekoadmin config reload
```
You can also manage providers with admin commands: `/tonekoadmin ai list`, `/tonekoadmin ai switch`, `/tonekoadmin ai test` nya

## How to Use
Just put the mod into the `mods` folder of your server/client, meow~

## Tips
If you find that some text isn't in your language, you can change the language setting through the Mod Menu config screen in-game~

## How to Become a Neko
If you want to become a neko, it's very simple. Just craft a Neko Collector and get close to cats. When the energy is full, a Neko Potion will drop. Just drink it, meow~

If you want to become the owner of another neko (yes, you can be one yourself too), just run `/toneko player <player name>`. The prerequisite is that the other player must already be a neko (Tips: a neko can have multiple owners)

## Keybinds
Some toNeko features can be used with keybinds. The defaults are:
- `K`: Ride nearby creatures (same as `/neko ride`)
- `I`: Lie down (literally)
- `O`: Get down (also literally)
- `J`: Open the quirks settings screen

These keybinds all have corresponding commands (I'll never tell you that they just call the commands directly)

## Neko NPCs
toNeko adds neko entities so there's stuff to do in single-player mode. You can encounter them in the wild, or just spawn them directly~

Shift+right-click a neko to open the interaction menu. Different nekos may have slightly different menus. The effective range is 16 blocks — if you walk too far away, the buttons won't work~

Each neko spawns with a different skin. There aren't many built-in skins yet, but you can add new ones through resource packs. As for how, I'll write a dedicated guide once the skin system is more complete, so stay tuned~ ^_^~

Nekos come in different variants, including Adventurer Neko, Ghost Neko, Crystal Neko, Fighting Neko, Maid Neko, and more, each spawning in different places. If you want to add yourself as one, just tell me and I'll try my best to do it! There's also a special variant — the Crystal Neko — that only spawns on toNeko's birthday (September 26th)~

Every neko has a level and affection. Their level grows through interaction, combat, exploration, and more, making them stronger and stronger nya~ A neko who has talked with AI or has an owner doesn't just disappear when she dies — she becomes a Ghost Neko and keeps accompanying you nya... (the kind that talks)

There's also a powerful **Mouflet Boss** waiting for you to challenge! It steals your items, flies around while attacking, acts cute, charms you, and carries you up into the sky — not easy to deal with! But if you're strong enough, you can tame it with a Contract and ride it through the skies nya~

## Accessories
toNeko adds several accessories (neko ears, tail, paws, etc.) that you can find in the creative inventory. Of course, they're all craftable. Accessories can be worn directly in armor slots, and if you have [trinkets](https://modrinth.com/mod/trinkets) installed, they can go in accessory slots too~

## Commands & Permissions (all granted by default)
```yaml
# To learn how to use each command, add "help" after it, e.g. /toneko help
# Commands for owners (permission: command.toneko.xxx)
/toneko
# Commands for nekos (permission: command.neko.xxx)
/neko
# Commands for admins (permission: command.tonekoadmin.xxx)
/tonekoadmin
# Set your quirks (permission: command.quirk.xxx)
/quirk
# View genetic info (permission: command.genetics.xxx)
/genetics
```

## Docs
- [AI Setup](docs/AI_en.md) — AI providers, models, API key tutorials
- [Genetics API](docs/genetics_api.md)
- [toNeko Online API](docs/TONEKO_ONLINE_API.md)

## Community & Support
- [Discord](https://discord.gg/hQ6Mm7wtt4)

## Integrations
Integrated mods: [luckperms](https://luckperms.net/)

That's all.