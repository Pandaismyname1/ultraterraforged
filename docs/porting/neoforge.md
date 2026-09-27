# Dropping Forge, adding NeoForge (from 1.21.1)

Roadmap decision: the 1.20.1 line keeps its Forge jar (which also runs on NeoForge 47.1). From 1.21.1 on, RTF ships **Fabric + NeoForge only**. This page maps every piece of the current `forge/` module to its NeoForge 21.1 equivalent, then lists what changes again on later NeoForge versions.

Sources: NeoForge 20.2/20.5/21.0/21.4 release notes, docs.neoforged.net (1.21.1), the NeoForge and FancyModLoader sources on their `1.21.1` branches, and the Architectury docs (`loom/neoforge_migration.mdx`, `loom/access_transformer.mdx`). The source-code facts were checked against NeoForge 21.1.252 / FML 4.0.44.

## What's in `forge/` today

The whole module is 16 Java files (from the [inventory](inventory.md#3-expectplatform-methods)):

| File | What it does | NeoForge 21.1 replacement |
|---|---|---|
| `RTFForge` (`@Mod`) | `RTFCommon.bootstrap()`, registers `DeferredRegister`s on `FMLJavaModLoadingContext.get().getModEventBus()`, `GatherDataEvent` → lang + pack metadata | `@Mod("reterraforged") public RTFNeoForge(IEventBus modBus, ModContainer container, Dist dist)`. `FMLJavaModLoadingContext` is removed; the mod bus is injected. |
| `RTFForgeClient` | `RegisterPresetEditorsEvent` → `PresetConfigScreen` | Same event, now `net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent` (mod bus): `event.register(RTFWorldPresets.RETERRAFORGED, (screen, ctx) -> new PresetConfigScreen(screen))`. Use a separate `@Mod(value = "reterraforged", dist = Dist.CLIENT)` class, so client classes never load on a dedicated server. |
| `platform/forge/ConfigUtilImpl` | `FMLPaths.CONFIGDIR` | `net.neoforged.fml.loading.FMLPaths.CONFIGDIR` (same shape). |
| `platform/forge/ModLoaderUtilImpl` | `FMLLoader.getLoadingModList().getModFileById(id)`; `FMLLoader.getDist()` | Same class under `net.neoforged.fml.loading`. The `MixinPlugin` calls `isLoaded` during mixin bootstrap, so keep using the *loading* mod list (`ModList` is not ready yet). From FML 21.9: `FMLEnvironment.getDist()` and `FMLLoader.getCurrent()`. |
| `platform/forge/RegistryUtilImpl` + `DeferredRegistry` (a hand-written 35-method `Registry`/`WritableRegistry` delegate over `GameData.getWrapper`) | vanilla-registry writes, custom static registries, datapack registries | **Delete `DeferredRegistry`.** NeoForge registries *are* vanilla `Registry` objects, and `GameData.getWrapper`, `IForgeRegistry`, `RegistryObject` and `RegistryBuilder.hasTags()` don't exist. See "Registries" below. |
| `platform/forge/DataGenUtilImpl` | `new RegistriesDatapackGenerator(output, lookup)` (Forge patched it to include modded datapack registries) | NeoForge's `DatapackBuiltinEntriesProvider`, or keep a common implementation. The Fabric impl is already a loader-neutral port of Forge's patch that only needs the list of datapack registries: on NeoForge, get it from `DataPackRegistriesHooks.getDataPackRegistries()` (**verify** the name on 21.1). |
| `server/commands/forge/RTFCommandsImpl` | `@EventBusSubscriber` + `RegisterCommandsEvent` | `NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> ...)`. `getDispatcher()` and `getBuildContext()` are unchanged. |
| `world/worldgen/biome/modifier/forge/*` | RTF `add`/`replace` modifiers implementing Forge's `BiomeModifier`, applied by a mixin into `ServerLifecycleHooks.runModifiers` | See "Biome modifiers" below. |
| `mixin/MixinMinecraftServer` | builds `FeatureTemplateManager` at `<init>` (full Forge ctor descriptor) and reloads it in `lambda$reloadResources$26` | Retarget. The ctor descriptor changes (1.21.1 adds `Services`/`LevelLoadListener` changes by 21.9). The lambda index is compiler-assigned and **must be read with `javap` from the NeoForge-patched jar** (`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/neoforge-21.1.*-minecraft-merged-mojang/...`). Better: drop both injections and use loader events (`AddReloadListenerEvent` on NeoForge, `ResourceManagerHelper`/`ResourceLoader` on Fabric) to register the template manager as a reload listener. That also removes the Fabric `method_29440` target. |
| `mixin/MixinServerLifecycleHooks` | appends RTF biome modifiers to Forge's list | Delete (see "Biome modifiers"). |
| `mixin/MixinTagsProvider` | null-guards `ExistingFileHelper.trackGenerated` in a Forge-patched lambda, because RTF runs tag providers at runtime | Check whether NeoForge 21.1 still calls `trackGenerated` from `TagsProvider`. If it does, retarget with a `javap`-verified lambda name. Otherwise delete it. |
| `mixin/MixinBiomeGenerationSettingsPlainsBuilder` | `@Accessor features` | Only needed by the Forge biome modifiers; delete along with them. |
| `META-INF/mods.toml`, `pack.mcmeta` | metadata | `META-INF/neoforge.mods.toml` (below). Delete `pack.mcmeta`: NeoForge generates one, and a stale `pack_format` makes NeoForge flag the mod's resources as incompatible (EMI hit this, decision D9). |

## Build (`neoforge/`)

```groovy
// settings.gradle
include("common", "fabric", "neoforge")
// common/build.gradle
architectury { common("fabric", "neoforge") }
// neoforge/gradle.properties
loom.platform=neoforge
// neoforge/build.gradle
architectury { platformSetupLoomIde(); neoForge() }
repositories { maven { url = "https://maven.neoforged.net/releases/" } }
configurations { developmentNeoForge.extendsFrom common }
dependencies {
    neoForge "net.neoforged:neoforge:${rootProject.neoforge_version}"        // 21.1.252
    common(project(path: ":common", configuration: "namedElements")) { transitive = false }
    shadowBundle(project(path: ":common", configuration: "transformProductionNeoForge")) { transitive = false }
}
```

- **Mixin configs** are declared in `neoforge.mods.toml` (`[[mixins]] config=...`). Loom's `forge { mixinConfig ... }` has no NeoForge equivalent.
- **Access widener → access transformer.** NeoForge reads `META-INF/accesstransformer.cfg` with Mojang names. There is no `forge { convertAccessWideners }` for NeoForge. On 1.21.1 (remapping Loom), use either `remapJar { atAccessWideners.add("reterraforged.accesswidener") }` or the experimental `loom.neoForge.convertAccessWideners(tasks.remapJar, "reterraforged.accesswidener")`. **From 26.1 (loom-no-remap) the automatic conversion is broken** ([architectury-loom#337](https://github.com/architectury/architectury-loom/issues/337)), so ship a hand-written `accesstransformer.cfg` next to the widener. That's what EMI does on every 26.x branch. Generate it from the widener with a small Gradle task instead of maintaining two files by hand: the formats map 1:1 (`accessible field a/B c` → `public a.B c`, and so on).
- **Datagen:** Loom's `forge { dataGen { mod ... } }` has no NeoForge shortcut. Define a `data` run (`runs { data { data(); programArgs "--mod", "reterraforged", "--all", "--output", file("src/generated/resources").absolutePath } }`) and check the argument names against the 21.1 `DataMain`. Remember that RTF's datagen JVM never exits by itself (see the dev notes).
- **Dev runs:** EMI found that client-only dev mods break `neoforge:runServer`. Keep the NeoForge run classpath minimal.

## `META-INF/neoforge.mods.toml`

```toml
modLoader="javafml"
loaderVersion="[1,)"            # FML major; 1.20.1 Forge used [47,)
license="MIT"

[[mods]]
modId="reterraforged"
version="${version}"
displayName="ReTerraForged"
logoFile="logo.png"             # 26.2+: iconFile + bannerFile (logoFile deprecated)
authors="dags, Won-Ton, raccoonman, Steveplays28, EdoEquin0x"
description='''...'''

[[mixins]]
config="reterraforged-common.mixins.json"
[[mixins]]
config="reterraforged-neoforge.mixins.json"

[[dependencies.reterraforged]]
modId="neoforge"
type="required"                 # replaces mandatory=true
versionRange="[21.1.0,)"
ordering="NONE"
side="BOTH"

[[dependencies.reterraforged]]
modId="minecraft"
type="required"
versionRange="[1.21.1,1.21.2)"
ordering="NONE"
side="BOTH"

[[dependencies.reterraforged]]
modId="terrablender"
type="optional"
versionRange="[4.1.0.0,)"
ordering="AFTER"
side="BOTH"
```

On 26.x: `loaderVersion="[4,)"`, `neoforge [26.x,27)`, `minecraft [26.x,26.(x+1))` (EMI's values).

## Events and buses

| Forge 1.20.1 | NeoForge 21.1 |
|---|---|
| `net.minecraftforge.eventbus.api.*` | `net.neoforged.bus.api.*` |
| `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` |
| `@Mod.EventBusSubscriber(bus = MOD)` | top-level `@EventBusSubscriber`. The `bus` parameter is deprecated in FML 4.0.44 and **removed** in current FML, because mod-bus events are routed automatically. Don't write `bus =` at all. |
| `@Cancelable` events | events implement `ICancellableEvent` |
| `FMLJavaModLoadingContext` | constructor injection (`IEventBus`, `ModContainer`, `Dist`) |
| `ForgeConfigSpec` / `ModLoadingContext.registerConfig` | `ModConfigSpec` / `container.registerConfig(...)`. RTF doesn't use loader configs, so nothing to do. |

From NeoForge 21.5, `@Mod` classes are constructed **before** `Minecraft` exists on the client. Don't touch `Minecraft.getInstance()` in constructors.

## Registries

RTF's `RegistryUtil` has three jobs. On NeoForge 21.1:

1. **Writing into vanilla static registries** (`DENSITY_FUNCTION_TYPE`, `MATERIAL_RULE`, …). `getWritable(registry).register(...)` fails on NeoForge because registries are frozen outside `RegisterEvent`. Use `DeferredRegister.create(registryKey, MOD_ID)` per registry, or buffer the entries and write them in `RegisterEvent` (`event.register(key, id, () -> value)`). The buffered design keeps the common `RegistryUtil.register(registry, name, value)` API intact: `RegistryUtilImpl` queues `(key, name, value)` and flushes each queue in `RegisterEvent` for that key.
2. **Custom static registries** (8 of them: `noise_type`, `domain_type`, …). Use `new RegistryBuilder<>(key).create()` and register it in `NewRegistryEvent#register(registry)`. The returned object is a vanilla `Registry`, so `byNameCodec().dispatch(...)` works as today. There's no `hasTags()`; every registry supports tags. `RegistryBuilder#sync(false)` matches today's behaviour (not synced).
3. **Datapack registries** (5 of them: `worldgen/noise`, `biome_modifier`, `structure_rule`, `surface_layers`, `preset`). Use `DataPackRegistryEvent.NewRegistry#dataPackRegistry(key, codec)`: same as Forge, new package. **26.3 renames it to `NewDatapackRegistryEvent`**, with `worldRegistry(...)`/`reloadableRegistry(...)`.

**`TestBootstrap` impact:** the headless tests mock `RegistryUtil` statically, so the NeoForge change doesn't affect them.

## Biome modifiers

Today, both loaders bypass their public biome-modifier APIs. Fabric mixes into `fabric.impl.biome.modification.BiomeModificationImpl` (an internal class) and builds `ModifierRecord`s by reflection. Forge mixes into `ServerLifecycleHooks.runModifiers`. Both read RTF's own `reterraforged:worldgen/biome_modifier` datapack registry.

**Recommended on NeoForge: use the public API.** Register a single serializer `reterraforged:preset` in `NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS` (values are `MapCodec<? extends BiomeModifier>`). Its `modify(Holder<Biome>, Phase.AFTER_EVERYTHING, builder)` applies every entry of RTF's registry. Make the preset datapack contain one file, `data/reterraforged/neoforge/biome_modifier/preset.json` (`{"type":"reterraforged:preset"}`). NeoForge then loads and orders it like any other modifier, and `MixinServerLifecycleHooks` and the `PlainBuilder` accessor go away.

NeoForge biome modifier API changes to track:
- 21.1: `codec()` returns `MapCodec`; folder `data/<ns>/neoforge/biome_modifier/`.
- 26.1: spawn modifiers take `WeightedList`.
- 26.3: `ModifiableBiomeInfo.BiomeInfo` loses `MobSpawnSettings` (mob spawns become the environment attribute `minecraft:gameplay/natural_mob_spawns`), and carver modifiers take `HolderSet<WorldCarver>`. RTF only touches features, so only the `features()` list API matters.

The Fabric side has the same option: the public `BiomeModifications.create(id).add(ModificationPhase.POST_PROCESSING, selector, (sel, ctx) -> ...)` API. The catch is that RTF's modifiers are data-driven per world, while Fabric's API is registered once at init. Keep the mixin there, but re-verify `BiomeModificationImpl.finalizeWorldGen`/`getSortedModifiers` and the `ModifierRecord` constructor on every Fabric API bump. They still exist in 1.21.1 (`fabric-biome-api-v1`); in 26.3, `addCarver` takes `ResourceKey<WorldCarver>`.

## Preset editor button

Fabric has no preset-editor API, so RTF redirects `Map.of` in `PresetEditor.<clinit>`. On NeoForge, `PresetEditorManager.init()` copies the vanilla `PresetEditor.EDITORS` and then posts `RegisterPresetEditorsEvent`. Use the event on NeoForge and keep the Fabric-only mixin in `fabric/`. Don't move it to common: it would double-register on NeoForge. The event still exists in NeoForge 26.3.

## Things that stop being true

- *"NeoForge 1.20.1 (47.1.x) still reports as forge"*: from 20.2 on, the loader id is `neoforge`. The `forge_min_version` property and its comment go away.
- `pack_format 15` in `forge/src/main/resources/pack.mcmeta`: deleted (see the table above).
- The CI artifact glob `forge/build/libs/*-forge.jar` becomes `neoforge/build/libs/*-neoforge.jar`.
