# toNeko Integration API Specification

> An integration manual for **third-party mod authors**: how to declare/detect a toNeko dependency, read and write neko data, register your own content (genes / quirks / AI providers), trigger AI conversations, and listen to events.
>
> Stability levels: 🟢 Stable (compatibility guaranteed, use freely) | 🟡 Experimental (usable, signatures may change) | 🔴 Internal (read-only; not recommended to call directly).
> See also: [开发规范](../开发规范.md) (development conventions), [genetics_api.md](genetics_api.md) (full genetics API).
> Version: toNeko 1.9.5 / Minecraft 1.21.1, works identically on Fabric and NeoForge (see §7).

## 1. Entry Point Overview

| Package | Content | Level |
|---|---|---|
| `org.cneko.toneko.common.mod.api` | Extension points: name/skin/level-factor registries, `ChatEvents`/`WorldEvents` | 🟢 |
| `org.cneko.toneko.common.mod.entities.INeko` | Core neko-data interface (all default methods) | 🟢 |
| `org.cneko.toneko.common.mod.entities.NekoEntity` | NPC neko entity base class | 🟢 |
| `org.cneko.toneko.common.mod.quirks` | Quirk interface and registry | 🟢 |
| `org.cneko.toneko.common.mod.genetics.api` | Genetics system API | 🟡 (see genetics_api.md) |
| `org.cneko.toneko.common.mod.ai.provider` | AI provider interface and registry | 🟡 |
| `org.cneko.toneko.common.mod.ai.actions` / `PromptRegistry` | AI actions / prompt extensions | 🟡 |
| `org.cneko.toneko.common.util.AIUtil` | The only AI-conversation entry point | 🟢 |
| `org.cneko.toneko.common.mod.misc.Messaging` / `ToNekoAttributes` | Message display / custom attributes | 🟢 |
| `org.cneko.toneko.common.mod.util` / `common.api` | Utilities / permission constants | 🟢 |
| `org.cneko.toneko.common.mod.client.*` | Client-only classes (**must not be referenced from server code**) | 🟡 |
| `org.cneko.toneko.common.mod.events.Common*`, `packets`, `mixin` | Internal implementation | 🔴 |

AI-related types (`AIResponse`, `AIRequest`, etc.) come from the built-in **NekoAI** library (`org.cneko.ai.*`, bundled in the jar) — use them directly.

## 2. Dependency Declaration and Detection

### 2.1 Hard dependency

`fabric.mod.json`:

```json
{
  "depends": { "toneko": ">=1.9.5" }
}
```

`neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
modId = "toneko"
type = "required"
versionRange = "[1.9.5,)"
ordering = "NONE"
side = "BOTH"
```

On NeoForge, toNeko itself depends on FFAPI (`fabric_api`); the installer handles that automatically — you don't need to declare it.

### 2.2 Soft-dependency detection

```java
// In your fabric module
if (FabricLoader.getInstance().isModLoaded("toneko")) { /* ... */ }

// In your neoforge module
if (ModList.get().isLoaded("toneko")) { /* ... */ }

// Platform-agnostic fallback (inside your own mod's code):
try {
    Class.forName("org.cneko.toneko.common.mod.entities.INeko");
    // toNeko is present — run integration logic
} catch (ClassNotFoundException ignored) {
    // toNeko is absent — run fallback logic
}
```

> Note: the "no FabricLoader in common" rule is toNeko's own internal constraint; as a third party you may use `FabricLoader` normally in your own fabric module.

### 2.3 Call timing

- Read-only APIs (`INeko`, `AIUtil`, `Messaging`) are available at any time.
- **Registration APIs** (providers / quirks / genes / names / skins / level factors) should be called **after server startup** (Fabric: `ServerLifecycleEvents.SERVER_STARTED`; the equivalent on NeoForge), by which point toNeko's `ModBootstrap.bootstrap()` has finished.
- `AIServiceProviderRegistry.register` **throws `IllegalStateException` for duplicate provider ids** (case-insensitive); check `hasProvider(id)` first or catch the exception.

## 3. Reading and Writing Neko Data (🟢 core)

### 3.1 Checking "is this a neko?"

```java
// NPC neko (all variants — adventurer/crystal/ghost/fighting/maid/boss — extend NekoEntity)
if (entity instanceof NekoEntity neko) { /* ... */ }

// Player neko (PlayerEntityMixin makes players implement INeko; isNeko() is false for unchanged players)
if (player instanceof INeko neko && neko.isNeko()) { /* ... */ }

// Any entity implementing the neko-data interface (including non-neko players)
if (entity instanceof INeko neko) { /* ... */ }
```

### 3.2 The INeko interface (core entry point, all default methods)

Located in `org.cneko.toneko.common.mod.entities.INeko`. Every method has a default implementation and is safe to call:

**Identity**: `isNeko()` / `setNeko(boolean)` / `isPlayer()` / `getEntity()` (returns `LivingEntity`) / `getAIStorageId()` (stable AI storage id — **use this for AI calls, not the entity UUID**).

**Level & ability**: `getNekoLevel()` (= `NekoLevelRegistry.computeTotal(this)`) / `getNekoAbility()` (= level × `neko.degree` attribute) / `getNekoLevelFactorRaw(String)` / `setNekoLevelFactorRaw(String, double)` / `getNekoLevelFactorData()`.

**Energy**: `getNekoEnergy()` / `setNekoEnergy(float)` / `getMaxNekoEnergy()` (attribute value) / `increaseEnergy()`.

**Owner system**: `getOwners()` (`Map<UUID, Owner>`) / `getOwner(uuid)` / `addOwner(uuid, Owner)` / `addOwnerIfNotExist(uuid)` / `removeOwner(uuid)` / `hasOwner(uuid)` / `getXpWithOwner(uuid)` / `setXpWithOwner(uuid, xp)`. `Owner` is a nested record: `Owner(List<String> aliases, int xp)`.

**Nickname & age**: `getNickName()` / `setNickName(String)` / `getNekoAge()` / `setNekoAge(int)` / `isNekoBaby()` / `setNekoBaby(boolean)` / `getMaxAge()`.

**Quirks**: `getQuirks()` (`List<Quirk>`) / `addQuirk(Quirk)` / `hasQuirk(Quirk)` / `removeQuirk(Quirk)` / `fixQuirks()` (prunes unregistered quirks).

**Blocked words**: `getBlockedWords()` (`List<BlockedWord>`) / `addBlockedWord(BlockedWord)` / `removeBlockedWord(String)`. `BlockedWord` is a nested record: `BlockedWord(String block, String replace, BlockMethod method)`.

**Visited biomes**: `getVisitedBiomes()` (`Set<String>` of biome keys) / `addVisitedBiome(String)`.

**Stealth**: `isStealthActive()` / `setStealthActive(boolean)` / `breakStealth()`.

**Sending messages** (automatically applies pet phrases/prefixes, see §5.3): `sendMessageToTarget(String, Entity)` / `sendMessageToAll(String)`.

**Level modifiers**: `updateNekoLevelModifiers()` (server-side; routine maintenance is handled by toNeko's slowTick).

### 3.3 NekoEntity-specific accessors

`getSkin()` / `setSkin(String)`, `getMoeTags()` / `setMoeTags(List<String>)` / `getMoeTagsString()` (moe tags). For player moe tags, rely on the `INeko` interface.

### 3.4 Custom attributes

`ToNekoAttributes.NEKO_DEGREE` (neko degree), `ToNekoAttributes.MAX_NEKO_ENERGY` (both `Holder<Attribute>`):

```java
double degree = entity.getAttributeValue(ToNekoAttributes.NEKO_DEGREE);
```

### 3.5 Data synchronization notes

- Server-side writes to `INeko` data are synced by toNeko internally (`NekoInfoSyncPayload` every 20 ticks / SynchedEntityData).
- **You are only responsible for server-side data**; when the client needs to see something immediately, use `Messaging.sendNekoChat` (§5.3) or your own packets — do not touch 🔴 internal payloads.
- After modifying a NekoEntity's genome you must call `expressTraits()` (server-side only).

## 4. Registry Extensions (registering your own content with toNeko)

### 4.1 Neko names / skins (🟢)

```java
// Register random names (NPC neko name pool)
NekoNameRegistry.register("小白");
NekoNameRegistry.register(List.of("咪咪", "团子"));

// Register skins: skin PNGs must live in the toneko namespace at
// assets/toneko/textures/neko/<skin>.png (can be provided via a resource pack;
// the filename is passed without the .png extension)
NekoSkinRegistry.register(ToNekoEntities.ADVENTURER_NEKO, "my_custom_skin");
NekoSkinRegistry.register(ToNekoEntities.ADVENTURER_NEKO, List.of("skin_a", "skin_b"));
```

Entity type constants live in `ToNekoEntities` (`ADVENTURER_NEKO`, `CRYSTAL_NEKO`, `GHOST_NEKO`, `FIGHTING_NEKO`, `MOUFLET_NEKO_BOSS`, `NOELLE_MAID_NEKO`).

### 4.2 Quirks (🟢)

Extend the `Quirk` base class and implement the `ModQuirk` interface (all hooks are default methods — override only what you need):

```java
public class MyQuirk extends Quirk implements ModQuirk {
    public MyQuirk() { super("my_quirk"); }  // id: lowercase snake_case

    @Override
    public InteractionResult onNekoInteraction(Player owner, Level world, InteractionHand hand,
                                               INeko neko, EntityHitResult hitResult) {
        // Fired when the owner interacts with the neko; return SUCCESS to intercept and grant XP
        return InteractionResult.PASS;
    }

    @Override
    public void onDamage(INeko neko, DamageSource source, float amount) { }
    @Override
    public void onJoin(INeko neko) { }
}
```

Available hooks (`ModQuirk`): `onNekoInteraction` / `onInteractionOther` / `onDamage` / `onJoin` / `onNekoAttack` / `onWeatherChange` / `startSleep` / `stopSleep` / `getTooltip` / `getInteractionValue`.

```java
// Register (recommended after SERVER_STARTED, see §2.3)
QuirkRegister.register(new MyQuirk());
```

Retrieve registered quirks with `QuirkRegister.getById("my_quirk")`; `INeko.fixQuirks()` prunes quirks that are no longer registered.

### 4.3 Level factors (🟢)

Implement `NekoLevelFactor` (`getId()` / `getLevel(double raw)`; optionally `getDefaultRawValue()`, `addRaw`, `setRaw`):

```java
public class MyLevelFactor implements NekoLevelFactor {
    @Override public String getId() { return "my_factor"; }
    @Override public double getLevel(double raw) { return raw; }
}
// Register
NekoLevelRegistry.register(new MyLevelFactor());
```

Any neko can then use `neko.setNekoLevelFactorRaw("my_factor", 10)`; `neko.getNekoLevel()` (= `computeTotal`) aggregates all factors automatically.

### 4.4 Genetics (🟡; full documentation in genetics_api.md)

**Via code** (three parts):

```java
// 1) Allele: dominance semantics 5 recessive / 10 wild / 15 codominant / 20 dominant
Allele myAllele = new Allele(ResourceLocation.fromNamespaceAndPath("yourmod", "my_allele"),
        10,                                    // dominance
        (entity, geneticData) -> { /* express callback: can write moe tags / NBT */ },
        (entity, geneticData) -> { /* remove callback */ })
        .addAttributeModifier(Attributes.MOVEMENT_SPEED, "my_modifier", 0.1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        .addAIGoal(3, mob -> new MyGoal(mob)); // injects an AI Goal (auto attach/cleanup)

// 2) Register the allele and its locus
GeneticsRegistry.registerAllele(myAllele);
GeneticsRegistry.registerLocus(new Locus(ResourceLocation.fromNamespaceAndPath("yourmod", "my_locus")));

// 3) Wild pool weight (roulette-weighted sampling on natural spawn; without this, wild mobs never carry it)
GeneticsRegistry.addWildAllele(ResourceLocation.fromNamespaceAndPath("yourmod", "my_locus"),
        ResourceLocation.fromNamespaceAndPath("yourmod", "my_allele"), 10);
```

Karyotypes: `registerKaryotype(ResourceLocation id, Class<? extends LivingEntity>, SpeciesKaryotype)` — toNeko already registers a base karyotype for `Mob.class`, so **vanilla mobs have gene slots too**.

**Via data pack (no code)**: ship `data/<your namespace>/toneko_genetics/` with the three JSON files (alleles / loci / karyotypes; structure in genetics_api.md) in your resource/data pack — loaded automatically on `/reload`, no Java code needed.

**Manipulating an entity's genome**: `entity instanceof IGeneticEntity g` then `g.getGenome()` / `g.setGenome(...)` / `g.expressTraits()` (**required after genome changes, server-side only**).

### 4.5 AI providers (🟡)

Implement the `AIServiceProvider` interface and register it. For **OpenAI-compatible** services, reuse the built-in helper `OpenAIProvider.applyCommon(OpenAIConfig, AIServiceConfig, String defaultHost)` (see `provider/impl/OpenAIProvider.java`):

```java
public class MyProvider implements AIServiceProvider {
    @Override public String getProviderId() { return "my_llm"; }   // lowercase; used as ai.service config value
    @Override public String getDisplayName() { return "My LLM"; }
    @Override public boolean isOpenAICompatible() { return true; }
    @Override public boolean requiresApiKey() { return true; }
    @Override public String getDefaultHost() { return "api.example.com"; }
    @Override public int getDefaultPort() { return 443; }
    @Override public String getDefaultEndpoint() { return "/v1/chat/completions"; }
    @Override public boolean isDefaultTls() { return true; }
    @Override public String getDefaultModel() { return "my-model"; }
    @Override public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        /* Use config's apiKey/model/host/port/endpoint/tls/proxy to send the request; return an AIResponse */
    }
    // Optional: override supportsStream() and processStream() for streaming;
    // without them, streaming calls fall back to a "single-element stream" automatically.
}
```

```java
// Register (duplicate ids throw IllegalStateException)
if (!AIServiceProviderRegistry.hasProvider("my_llm")) {
    AIServiceProviderRegistry.register(new MyProvider());
}
```

After registration, users must set `ai.service` to `my_llm` in the config for it to be used; per-provider config sections (`ai.providers.<id>.*`) are supported.

### 4.6 AI actions (🟡)

`NekoActionExecutor.register(type, NekoActionHandler)` registers model-callable actions (function-calling style):

```java
NekoActionExecutor.register("hug", new NekoActionExecutor.NekoActionHandler() {
    @Override
    public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
        // Runs on the server main thread; return whether the action succeeded
        neko.sendMessageToTarget("（抱住了%s）".formatted(target.getName().getString()), speaker);
        return true;
    }
    @Override
    public String getGuideLine() {
        // Action guide line injected into the prompt (JSON example + explanation);
        // use LanguageUtil.translatable for localization
        return "{\"action\":\"hug\",\"target\":\"目标名\"} 表示抱住目标；";
    }
});
```

`NekoAction` is a record: `NekoAction(String type, String item, int count, String target, String text)`.

### 4.7 Prompt snippets (🟡)

`PromptRegistry.register(key, PromptFactory)`; `PromptFactory` is the functional interface `String getPrompt(NekoEntity neko, INeko other)`. `%key%` placeholders are replaced in a single scan pass when the prompt is generated (see the 22 built-in snippets in the `Prompts` class for registration examples).

## 5. AI Conversation Integration (🟢)

### 5.1 Sending messages

**`AIUtil` is the only entry point** (do not bypass it to call providers directly — cooldowns, history, and context injection are all handled there):

```java
// Non-streaming
AIUtil.sendMessage(
        neko.getAIStorageId(),        // neko storage id (use getAIStorageId(), never the entity UUID)
        player.getUUID(),             // speaking player
        prompt,                       // extra prompt (nullable)
        "你好呀~",                    // message body
        (AIResponse response) -> {    // MessageCallback: invoked on the AI worker thread
            String text = response.getContent();
            ModMeta.INSTANCE.getServer().execute(() -> {   // MC operations must hop to the main thread
                Messaging.sendNekoChat(player, neko, text); // bubble/chat display (per client config)
            });
        });

// Streaming (SSE typewriter)
AIUtil.sendMessageStream(
        neko.getAIStorageId(), player.getUUID(), prompt, "你好呀~",
        new AIUtil.StreamCallback() {
            @Override public void onChunk(String chunk) {
                // Clean text increment (action JSON already stripped; may be empty) — push to the client
            }
            @Override public void onFinished(AIResponse full) {
                // Full response (including think); action parsing and history saving are based on this
            }
            @Override public void onError(AIResponse error) {
                // Cooldown/connection/stream-interruption/timeout; error is shaped like the error AIResponse
            }
        });
```

Overload parameters: `(nekoStorageId, userUuid, prompt, message, callback[, ignoreCooldown, historyPrefix])` — `ignoreCooldown=true` skips the per-player cooldown (config `ai.cooldown`); `historyPrefix` overrides the default `[speaker]` history prefix (null = default).

### 5.2 Callback thread discipline (important)

- Callbacks run on the **AI worker thread**; all world/entity/network operations must hop back to the main thread via `ModMeta.INSTANCE.getServer().execute(...)` (or `player.getServer().execute(...)`).
- Wrap network sends in try-catch (the player may have disconnected).
- Calling `sendMessageStream` on a non-streaming provider automatically falls back to a "single-element stream" (one chunk + immediate completion), visually identical to one-shot display.

### 5.3 Display and TTS

```java
// Neko bubble/chat (client picks the mode in its config; visible to players in range)
Messaging.sendNekoChat(player, neko, text);                    // single player
Messaging.sendNekoChatInRange(entity, neko, text, 64.0);       // broadcast in range

// Plain-text transformation with pet phrases/prefixes applied (no display)
String formatted = Messaging.formatMessage(text, neko);
Component hover = Messaging.createComponentWithHover(formatted, neko);

// Neko-initiated speech (INeko default method, equivalent to modifyAndSendMessageToAll)
neko.sendMessageToAll("喵~");

// TTS (Player2 provider only; see the AI settings docs)
AIUtil.playTTS(text, voice);

// Strip role prefixes from model replies (e.g. "[猫娘]你好")
AIUtil.cleanReplyPrefixes(text);
```

## 6. Events and Interaction Hooks (🟢)

### 6.1 Chat events

```java
// Add prefixes to neko chat
ChatEvents.CREATE_CHAT_PREFIXES.register((INeko sender, List<String> prefixes) -> {
    prefixes.add("[联动]");
});

// Modify the final chat text (return value replaces the message)
ChatEvents.ON_CHAT_FORMAT.register((message, sender, prefixes, chatFormat) ->
        message.replace("喵", "喵~"));
```

### 6.2 Weather events

```java
WorldEvents.ON_WEATHER_CHANGE.register((ServerLevel world, int clearTime, int weatherTime,
                                        boolean isRaining, boolean isThundering) -> { /* ... */ });
```

> Known defect: the current implementation passes `isRaining` into the `isThundering` parameter (an implementation bug) — fall back to `world.isThundering()` yourself.

### 6.3 Player–neko interactions

**The sanctioned way to extend interactions is registering a quirk (§4.2)** — interaction triggers are centralized in toNeko's internal event classes (🔴) and should not be hooked directly. Quirk hooks cover: interaction (`onNekoInteraction`/`onInteractionOther`), damage, attack, sleeping, weather, and join.

### 6.4 Entity interaction screens (🟡, client)

```java
// Client-side: register a custom interaction screen for an entity type
// (builder is NekoScreenBuilder, supports clone())
NekoScreenRegistry.register(ToNekoEntities.ADVENTURER_NEKO_ID,
        new NekoScreenBuilder()
                .setStartY(20)
                .addButton(new MyButtonFactory())
                .addTooltip(new MyTooltipFactory()));
```

Builder fluent API: `setStartY` / `addButton` / `addTooltip` / `addWidget` (with insertion positions) / `clone`. Entity-bound screens implement `INekoScreen` (`NekoEntity getNeko()`).

### 6.5 Delayed task queues

```java
// Server delayed task (tick-based, runs on the main thread)
TickTasks.add(new ITickable() {
    int ticksLeft = 100;
    @Override public void tick(int tickCount) {
        if (--ticksLeft <= 0) TickTasks.remove(this);
    }
});
// Client-side: TickTasks.addClient(...); or use mod/util/TickTaskQueue (addTask(delayTicks, runnable))
```

## 7. Platform Boundaries

- **Fabric and NeoForge behave identically**: every API above lives in the common module, bridged through FFAPI; your integration code is written once.
- **Client-only classes** (`common.mod.client.*`: screens, renderers, `NekoScreenRegistry`, etc.) may only be referenced from client entrypoints / `@Environment(CLIENT)` code — never from server-dedicated code.
- **Trinkets integration exists only on Fabric** (slot `legs/socks`); on NeoForge, legwear is a plain ArmorItem.
- **The Bukkit plugin edition does not provide this API** (it does not share these MC-bound types).
- Thread discipline matches toNeko's internal conventions: `Level` queries only on the main thread; AI callbacks run on background threads (§5.2).

## 8. Stability Level Summary

| API | Level | Notes |
|---|---|---|
| `INeko`, `NekoEntity`, `Owner`/`BlockedWord` | 🟢 | Interface grows via default methods; compatibility-first |
| `NekoNameRegistry` / `NekoSkinRegistry` / `NekoLevelRegistry` / `QuirkRegister` / `ModQuirk` | 🟢 | Static registries; register-and-go |
| `AIUtil` / `Messaging` / `ToNekoAttributes` / `ChatEvents` / `WorldEvents` | 🟢 | Core call paths |
| `mod/util/*`, `common.api/Permissions`, `TickTasks` | 🟢 | Pure utilities, no side effects |
| `genetics.api.*` | 🟡 | Functionality stable, signatures may still shift; genetics_api.md is authoritative |
| `AIServiceProvider(Registry)` / `NekoActionExecutor` / `PromptRegistry` | 🟡 | AI layer iterates quickly |
| `NekoScreenRegistry` / `NekoScreenBuilder` | 🟡 | Client UI builders |
| `Common*Event`, `packets/*`, `mixin/*`, internal quirk impls | 🔴 | Internal implementation, subject to refactor; depending on them = accept breakage risk |

## 9. Improvement Suggestions for toNeko Maintainers

Current issues, by priority:

1. **NickName load bug**: in `INeko.loadNekoNBTData`, `setNickName(this.getNickName())` should be `setNickName(nbt.getString("NickName"))` — otherwise the loaded nickname is always the current value.
2. **`WorldEvents.ON_WEATHER_CHANGE`'s `isThundering` parameter** actually receives `isRaining` (implementation bug, see §6.2).
3. **Skin namespace restriction**: `NekoRenderer.getTextureResource` hardcodes the `toneko` namespace, so third-party skins must be injected into `assets/toneko/`; consider supporting arbitrary namespaces (e.g. a `ResourceLocation`-keyed overload on `NekoSkinRegistry`).
4. **No official facade**: consider adding a `ToNekoAPI` facade class aggregating high-frequency operations like `isNeko(Entity)` / `getNeko(Entity)`, so third parties don't have to hunt between `mod.api` and `mod.entities`.
5. **Stability annotations**: add `@ApiStatus.Internal` / `@ApiStatus.Experimental` annotations and Javadoc to public classes (`INeko` and genetics already have partial Javadoc; the other registries have none).
6. **`AIServiceProviderRegistry` registration timing**: registering before toNeko initializes can conflict with the built-in `init()`; document or enforce "register after SERVER_STARTED" (currently only the §2.3 convention covers this).
7. **Client event surface**: `ChatEvents` is server-semantics only; if client events are ever needed (e.g. bubble rendering), expose them via `mod.api.events` rather than internal classes.

## 10. Complete Example: an addon mod's entry skeleton

```java
public class MyAddon {
    public static void init() {
        // 1) Detect toNeko (soft dependency)
        if (!FabricLoader.getInstance().isModLoaded("toneko")) return;   // fabric module
        // if (!ModList.get().isLoaded("toneko")) return;                // neoforge module

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // 2) Register your own content
            QuirkRegister.register(new MyQuirk());
            NekoLevelRegistry.register(new MyLevelFactor());
            NekoSkinRegistry.register(ToNekoEntities.ADVENTURER_NEKO, "my_skin");
            if (!AIServiceProviderRegistry.hasProvider("my_llm")) {
                AIServiceProviderRegistry.register(new MyProvider());
            }
            NekoActionExecutor.register("hug", new MyHugAction());
        });
    }

    /** From anywhere: make a neko reply */
    public static void askNeko(ServerPlayer player, NekoEntity neko, String msg) {
        AIUtil.sendMessageStream(neko.getAIStorageId(), player.getUUID(), null, msg,
                new AIUtil.StreamCallback() {
                    @Override public void onChunk(String chunk) { /* push typewriter increments */ }
                    @Override public void onFinished(AIResponse full) {
                        ModMeta.INSTANCE.getServer().execute(() ->
                                Messaging.sendNekoChat(player, neko, full.getContent()));
                    }
                    @Override public void onError(AIResponse error) { }
                });
    }
}
```
