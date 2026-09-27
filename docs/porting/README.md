# Porting UTF to 1.21.1 → 26.1 → 26.1.2 → 26.2 → 26.3

Exploration done 2026-09-27 against the 1.20.1 line (commit `036d949`). **1.21.1 was ported on 2026-09-28** (branch
`1.21.1`; what differed from the plan is at the end of [1.21.1.md](1.21.1.md)); the later hops are still plans.

> **Baseline and naming.** The analysis ran on `036d949`, before the UltraTerraForged rename (`7c5c17e`). The docs use the renamed identifiers: `com.pandaismyname1.ultraterraforged`, `UTF*` classes and the `ultraterraforged` mod id. File line numbers and error counts are still those of `036d949`. The raw compiler output in [census/](census) is kept verbatim, with the old names. These docs describe what each Minecraft update breaks in UTF, and how to fix it, before anyone starts. From 1.21.1 on, UTF is **Fabric + NeoForge**; Forge stays on the 1.20.1 line only.

Everything here is grounded in one of:
- the actual jars: class dumps, `javap`, and vanilla's data generator output for 1.20.1, 1.21.1, 26.1, 26.1.2, 26.2 and 26.3;
- throwaway builds of UTF's `common` module against each version (the compile census; the repo was never modified);
- the NeoForged primers, the Fabric and NeoForge blogs, loader sources, and Maven metadata;
- the EMI ports to 26.1/26.2/26.3 in `~/IdeaProjects/emi`, done with the same Architectury setup.

Anything not confirmed is marked **verify**.

## The ladder at a glance

| Hop | New compile errors | What dominates | Worldgen hooks | Size | Guide |
|---|---|---|---|---|---|
| 1.20.1 → **1.21.1** | 136 in 79 files | `BootstrapContext` typo fix, `MapCodec` dispatch, DFU 8, `ResourceLocation` factories; **Forge → NeoForge** | Unchanged except the chunk-status lambdas (→ `ChunkStatusTasks`) and the registry-loader hook | M | [1.21.1.md](1.21.1.md), [neoforge.md](neoforge.md) |
| 1.21.1 → **26.1** | +309 in 60 files | Unobfuscated toolchain, Java 25, `Identifier`, `GuiGraphicsExtractor`, input events, `ChunkPos` record | Unchanged, but with **silent semantic changes**: `getMaxY` off-by-one, `preliminary_surface_level` is now a Y level, ARGB pixels | M–L | [26.1.md](26.1.md), [toolchain.md](toolchain.md) |
| 26.1 → **26.1.2** | 0 | Nothing: one added GUI method in the whole jar | – | – | [26.1.md](26.1.md) (build once for `>=26.1 <26.2`) |
| 26.1.2 → **26.2** | +124 in 29 files | Surface-rule `MapCodec`s, `isBiome(HolderGetter…)`, `noiseCondition2d`, tag providers, `MenuTabBar`, `gui.setScreen` | Minor; vanilla now does what two UTF mixins do | S–M | [26.2.md](26.2.md) |
| 26.2 → **26.3** | +873 in 74 files | **Vanilla worldgen rewrite**: compiled density samplers, material rules, feature/carver configs merged, new chunk pipeline | **About half the common mixins lose their targets**; the terrain-injection design has to change | XL | [26.3.md](26.3.md) |

Per-file error lists: [census/](census). Mixin and access-widener status per version: [census/mixin-targets.txt](census/mixin-targets.txt) and [census/aw-check.txt](census/aw-check.txt).

## The findings that matter most

1. **26.3 is the real port; everything before it is mechanical.** Mojang replaced `DensityFunction` evaluation with a compile-to-sampler pipeline, surface rules with datapack-registered *material rules*, `ConfiguredFeature`/`ConfiguredWorldCarver` with self-configured features and carvers, and the noise/surface/carver chunk statuses with one `terrain` step. UTF injects its terrain through exactly those internals. [26.3.md](26.3.md) has a concrete replacement for each UTF subsystem. The central one (§A) is to bind UTF's per-chunk terrain cache the way vanilla binds the beardifier: a `ContextBoundSampler` plus a `ContextKey` in `NoiseChunk`'s sampler context. That's *less* invasive than today's approach.
2. **26.1 needs a new toolchain.** Use `dev.architectury.loom-no-remap`, no mappings, access-widener namespace `official`, and Java 25; the Gradle daemon JVM pin (`gradle-daemon-jvm.properties`) must go to 25 too. Architectury's AW → AT conversion is broken on no-remap, so NeoForge needs a hand-written `accesstransformer.cfg`. `@ExpectPlatform` under no-remap is untested (EMI doesn't use it), so **spike it first**. See [toolchain.md](toolchain.md).
3. **Some changes compile but behave differently.** These need tests, not the compiler:
   - `getMaxY()` is one lower than `getMaxBuildHeight()`, which shifts strata.
   - The noise router's `preliminary_surface_level` is a Y level, not a density.
   - `NativeImage` is ARGB, so the preview's red and blue would swap.
   - GUI text needs alpha, or it's invisible.
   - 26.3 samples in `float`, so small terrain drift is expected.

   See [26.1.md](26.1.md#silent-changes-compile-fine-behave-differently).
4. **Loom silently drops dead access-widener entries.** 55 of UTF's 76 entries are dead by 26.3, and about 20 are unused even today. Prune now, and run `tools/checkaw.py` on every bump.
5. **The code that copies vanilla data is the most fragile part of UTF.** `Preset*` re-creates vanilla's router, surface rules, features, biomes and dimension types in code; 441 of the 873 26.3 errors are in five `data/preset` files. `tools/vanilla-datagen.sh` generates the exact vanilla JSON per version to port against.
6. **Registry-name trap in 26.3.** `Registries.FEATURE`/`MATERIAL_RULE`/`MATERIAL_CONDITION`/`CARVER` now mean *datapack* registries; the codec registries are the `*_TYPE` keys. UTF registers into all three builtin ones today.
7. **NeoForge is a chance to drop internal-API mixins.** Use NeoForge's public biome-modifier API instead of patching `ServerLifecycleHooks`, `RegisterPresetEditorsEvent` instead of `PresetEditor.<clinit>`, and reload listeners instead of lambda-named `MinecraftServer` mixins. Forge's hand-written 35-method `DeferredRegistry` goes away entirely. See [neoforge.md](neoforge.md).
8. **Ecosystem:**
   - TerraBlender exists for every target; its 26.3 API renames `SurfaceRuleManager` → `MaterialRuleManager`.
   - World Preview is abandoned; *World Preview Prime* covers 26.1.x only.
   - Voxy has no 1.21.1 build, so the dev-run plan starts at 26.1.
   - NeoForge 26.3 is still beta-only (`26.3.0.23-beta`); 26.2 has a stable NeoForge (`26.2.0.88`).

## Suggested order of work

1. **Prune on 1.20.1 first** (cheap, and it shrinks every later diff): the ~20 unused AW entries, `VolatileAirBlock`, the unused Fabric datagen invoker, `RegistryUtil.getBiomeModifierRegistry`, and the no-op `@ModifyVariable` in `MixinSurfaceSystem`. Where cheap, move dispatch codecs to `MapCodec` already: DFU 6 has `MapCodec`, and it's required from 1.21.1 anyway.
2. **1.21.1**: create the `neoforge` subproject, then fix the census, retarget the four mixins, and run the headless tests. Fingerprints must not move.
3. **26.1.2**: toolchain switch plus the `@ExpectPlatform` spike, then the renames and the silent-change audit.
4. **26.2**: short; make the surface-rule codecs 26.3-shaped.
5. **26.3**: A → C → B → rest, per [26.3.md](26.3.md#strategy-options).

## Decisions for you

- **Stonecutter layout.** One tree spanning 1.21.1–26.3 needs two Loom plugins and two JDKs (obfuscated vs. unobfuscated), plus class-level swaps for 26.3's worldgen. The options are one tree with a small version-bridge layer, or a 1.21.1 branch plus a 26.x tree. See [toolchain.md](toolchain.md#stonecutter) and [26.3.md](26.3.md#strategy-options).
- **Is 26.1.x worth a release?** 26.1 and 26.1.2 are one jar. 26.2 has stable NeoForge and TerraBlender. Shipping 26.1.x mostly buys World Preview Prime compat.
- **26.3 on a beta NeoForge**, or wait for a stable build?
- **`MixinSpawnFinder` currently changes spawn finding for every world**, not just UTF ones. 26.3's `getOrigin` hook makes it easy to scope to UTF worlds; decide whether to scope it earlier.

## Index

| Doc | Contents |
|---|---|
| [toolchain.md](toolchain.md) | Version matrix (Java, Loom, Gradle, Fabric API, NeoForge, TerraBlender, pack formats), the unobfuscated-build recipe, Stonecutter notes, CI |
| [neoforge.md](neoforge.md) | Forge module → NeoForge 21.1, file by file; metadata; registries; biome modifiers; later NeoForge changes |
| [1.21.1.md](1.21.1.md) | 1.20.1 → 1.21.1: errors by fix, mixins, AW, data folder renames, tests, ecosystem |
| [26.1.md](26.1.md) | 1.21.1 → 26.1/26.1.2: errors by fix, silent changes, mixins, GUI, world creation |
| [26.2.md](26.2.md) | 26.1.2 → 26.2 |
| [26.3.md](26.3.md) | 26.2 → 26.3: the rewrite, with a solution per subsystem and a checklist |
| [verification.md](verification.md) | How to re-run every check: class dumps, mixin/AW checks, compile census, vanilla datagen, test plan |
| [inventory.md](inventory.md) | Every Minecraft and loader API UTF touches, as of 1.20.1: mixins, AW, `@ExpectPlatform`, registrations, GUI, datagen, tests |
| [census/](census) | Raw per-file compile errors per hop, plus mixin and AW check output |
| [tools/](tools) | The scripts |
