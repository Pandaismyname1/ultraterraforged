# Performance mods

ReTerraForged recommends a set of performance mods, shows in the Create World screen which of them are installed, and
runs them in the dev environment. Which builds exist changes with every Minecraft version and loader, so this page
records what was found for each version, what was tested with ReTerraForged, and what to change when porting.

State as of 2026-09-27 (ReTerraForged on 1.20.1, Fabric and Forge; the Forge jar also covers NeoForge 47.1).

## Where it lives

| What | Where |
|---|---|
| The list of mods, their mod ids and the builds per loader | `common/.../compat/performance/PerformanceMods.java` (data only) |
| The Terrain tab section and its light | `common/.../client/gui/createworld/PerformanceModsSection.java`, wired in `TerrainTab` |
| Tests of the light rules | `common/src/test/.../PerformanceModsTest.java` |
| The dev runs' mods | `fabric/build.gradle`, `forge/build.gradle` (`modLocalRuntime`, pinned) |
| Repositories | root `build.gradle`: Modrinth maven (`maven.modrinth`), Cursemaven (`curse.maven`) |
| Availability lookup | `docs/porting/tools/perfmods.py` |
| Server tests: timing chunk generation, running console commands | `docs/porting/tools/servers/` |

`-Prtf.perfMods=false` runs the dev game without them, e.g. to measure or debug ReTerraForged on its own.

Two things the dev runs need that a real game doesn't:

- **Mods made of nested jars** (C2ME: one jar holding its ~20 modules) don't load from the Gradle classpath, which
  doesn't unpack jars in jars ("requires c2me-base, which is missing"). Fabric puts them in the `devModsFolder`
  configuration instead: `syncDevMods` copies them unremapped to `fabric/build/devmods`, and every run passes that
  folder to Fabric loader as `fabric.addMods`, which unpacks and remaps them like a mods folder.
- **The Forge dev game runs with Mojang's names; released Forge mods use SRG names**, so released jars can't just go in
  `forge/run/mods` (unlike Fabric, whose loader remaps the mods folder in dev). Everything goes through Loom, which
  remaps dependencies, and a few things need more:
  - Remapping a mod drops its list of nested jars (`META-INF/jarjar/metadata.json`), so their libraries don't load.
    `extractNestedDevLibs` takes them out for the runtime classpath, marking any that aren't typed as
    `FMLModType: GAMELIBRARY` (as a real game treats them; otherwise Radium's config library is ignored and its patches
    clash), and adds MixinExtras once, the newest version any mod nests.
  - **Sinytra Connector** keeps its own mod inside its jar and only loads it from there, with SRG names.
    `prepareConnectorForDev` downloads Connector, remaps it and the mod inside from SRG to the dev names with Loom's
    mappings and tiny-remapper, and puts the mod back. It also needs `connector.clean.path`: the unpatched game with the
    dev names, taken from the Fabric side's Loom cache (so run a Fabric task once first).
  - **Forgified Fabric API** comes from Sinytra's Maven (`maven.su5ed.dev`), module by module so Loom can remap each,
    without its old separate Fabric loader, which clashes with Connector's.
  - **C2ME** goes in `forge/run/mods` unremapped (`copyConnectorMods`): Connector only looks for Fabric mods there, and
    remaps them itself.
  - Connector brings its own Mixin, which ignores the `--mixin.config` arguments the dev game passes, so
    `forge/src/main/resources/META-INF/MANIFEST.MF` names ReTerraForged's mixin configs, as the released jar's does.
- **Sodium refuses the dev game's LWJGL** (3.3.2 against the 3.3.1 launchers ship), so the runs set
  `sodium.checks.issue2561=false`. Check whether that's still needed after a port: newer Sodium versions require
  other LWJGL versions, and the property name follows Sodium's issue number.

### The light

Only mods with a build for the running Minecraft version and loader count; a mod that only runs through Sinytra
Connector counts once Connector is installed.

| Light | When |
|---|---|
| Red | none of the mods are installed |
| Orange | some are, but none of the world generation ones (C2ME, C2ME OpenCL, Noisium) |
| Yellow | some of those available are installed, including one for world generation |
| Green | every mod available for this version and loader is installed |

## The mods

| Mod | Does | Mod ids | Notes |
|---|---|---|---|
| [C2ME](https://modrinth.com/mod/c2me-fabric) | chunk generation, loading and saving on many threads | `c2me` | Always alpha. NeoForge builds from 1.21.1 ([c2me-neoforge](https://modrinth.com/mod/c2me-neoforge)); none for Forge, but the Fabric build runs through Connector on 1.20.1 |
| [C2ME OpenCL](https://modrinth.com/mod/c2me-ocl) | C2ME add-on: parts of world generation on the GPU | | From 1.21.1, alpha. Needs C2ME |
| [Noisiumed](https://modrinth.com/mod/noisiumed) / [Noisium](https://modrinth.com/mod/noisium) | faster block placement when filling chunks | `noisiumed`, `noisium` | Noisium is archived; Noisiumed is the maintained fork. Either counts; never install both. Noisiumed refuses to run under Connector |
| [Lithium](https://modrinth.com/mod/lithium) | ticking, AI, physics, some world generation | `lithium` | No Forge build: [Radium](https://modrinth.com/mod/radium) (`radium`, preferred) or [Canary](https://modrinth.com/mod/canary) (`canary`) stand in. Official NeoForge builds from 1.21.1 |
| [ModernFix](https://modrinth.com/mod/modernfix) | loading time, memory | `modernfix` | Overwrites the same biome temperature cache method as Lithium; the log warns, it's harmless |
| [AllTheLeaks](https://www.curseforge.com/minecraft/mc-mods/alltheleaks) | memory leak fixes | `alltheleaks` | CurseForge only. Forge 1.20.1, NeoForge 1.21.1; no Fabric |
| [Sodium](https://modrinth.com/mod/sodium) | rendering | `sodium` | Client only. No Forge build: [Embeddium](https://modrinth.com/mod/embeddium) (`embeddium`) stands in. Official NeoForge builds from 1.21.1 |
| [Sinytra Connector](https://modrinth.com/mod/connector) | runs Fabric mods on Forge/NeoForge | `connector` | Needs [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api). Not a performance mod; lets the Forge list count Fabric-only mods |

## 1.20.1 (current)

What the dev runs use, pinned to Modrinth version ids because Noisiumed shares its version numbers between loaders:

| Mod | Fabric | Forge / NeoForge 47.1 |
|---|---|---|
| C2ME | 0.2.0+alpha.11.18 (`fyt7FtgA`) | the Fabric build, through Connector 1.0.0-beta.49 + Forgified Fabric API 0.92.6 |
| C2ME OpenCL | none (from 1.21.1) | none |
| Noisium | Noisiumed 3.0.6 (`vcRbbvYP`) | Noisiumed 3.0.6 (`LbWCNzST`) |
| Lithium | 0.11.4 (`iEcXOkz4`) | Radium 0.12.4 (`n947JjJH`); Canary 0.3.3 also works |
| ModernFix | 5.25.2 (`rPmgLeZC`) | 5.27.83 (`jAZ7Ge3d`) |
| AllTheLeaks | none | 1.1.3 (`curse.maven:alltheleaks-1091339:8779054`) |
| Sodium | 0.5.13 (`OihdIimA`) | Embeddium 0.3.31 (`UTbfe5d1`) |

So five of the seven count on either loader: on Fabric everything but C2ME OpenCL and AllTheLeaks, on Forge everything
but C2ME and C2ME OpenCL, and six once Sinytra Connector is installed, which brings C2ME.

### Tested with ReTerraForged

Dedicated servers, fresh world with the default preset, 1024 forceloaded chunks, Ryzen 7 9800X3D, measured with
`tools/servers/bench.sh`:

| Setup | Seed 1111 | Seed 2222 | Seed 3333 |
|---|---|---|---|
| Vanilla, Fabric | 14.5 s | 14.2 s | 14.3 s |
| ReTerraForged, Fabric | 16.8 s | 18.9 s | 18.8 s |
| ReTerraForged, Fabric + C2ME, Noisiumed, Lithium, ModernFix | 14.2 s | 14.2 s | 11.9 s |
| ReTerraForged, Forge | 19.0 s | | 16.4 s |
| ReTerraForged, Forge + Noisiumed, Radium, ModernFix, AllTheLeaks | 18.8 s | | |
| ReTerraForged, Forge + C2ME through Connector | 16.8 s | | 16.4 s |
| ReTerraForged, Forge + all of the above | 14.2 s | | 11.9 s |

C2ME does most of the work, but only with the rest: on Forge, C2ME alone gained 0–12 %, the others alone 1 %, all of
them together 25–28 %, which brings ReTerraForged to vanilla's speed or better on both loaders. Every setup passed the
`/rtf locate` smoke test with the same results.

- **Forge crashed at startup with Noisiumed, ModernFix or AllTheLeaks** (a `ClassCastException` in MixinExtras'
  `FactoryRedirectWrapperMixinTransformer`, loading `NoiseBasedChunkGenerator`). Not the mods' fault: Fabric's Mixin
  0.17.4 changed `at` of `@Redirect`, `@ModifyArg`, `@ModifyArgs` and `@ModifyVariable` from `At` to `At[]`, so common
  code compiled against it (through the Fabric loader dependency) carries arrays; Forge's Mixin accepts them, but
  MixinExtras up to 0.5.0, which those mods bundle, casts to a single annotation. Fixed by compiling common code against
  sponge-mixin 0.17.3 (`common/build.gradle`); `MixinAnnotationsTest` fails if an array comes back. **Keep this in mind
  on every port**: whenever the Fabric loader is bumped, check the pin still applies, and run a NeoForge/Forge server
  with a MixinExtras-bundling mod (ModernFix is in most packs).
- C2ME changes how chunks are scheduled. ReTerraForged's hooks on the chunk status tasks (queueing tiles before
  generation, dropping them after features) still run under it; `/rtf locate` and generation work, no errors.
- **C2ME on Forge through Sinytra Connector works** (Connector 1.0.0-beta.49, Forgified Fabric API 0.92.6, C2ME
  0.2.0+alpha.11.18): its modules load, ReTerraForged generates and locates as on Fabric. The only errors in the log are
  Forgified Fabric API's client screen mixins being skipped on a dedicated server. So `PerformanceMods` lists C2ME for
  Forge as a Connector build: it counts once Connector is installed, and its row links to Connector until then.
  The Forge dev runs include it too (see above for what that takes).
- **C2ME OpenCL doesn't run on 1.20.1**, tried with its 1.21.1 build (0.4.0-alpha.0.13) on Java 25: Fabric loader
  refuses it, as it needs C2ME's density function compiler (`c2me-opts-dfc`) and chunk system rewrite
  (`c2me-rewrites-chunk-system`), which 1.20.1's C2ME doesn't have. With those dependencies overridden it crashes
  applying its mixins to a class added in 1.21 (`class_9310`). It stays listed as "Needs 1.21.1+".

## Later versions

From 1.21 on ReTerraForged targets Fabric and NeoForge only. What exists (newest build per version, found
2026-09-27 with `tools/perfmods.py`; a = alpha, b = beta):

| Mod | 1.21.1 | 1.21.4 – 1.21.5 | 1.21.8 – 1.21.11 | 26.1 / 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|---|---|
| C2ME | Fabric a, NeoForge a | Fabric a (NeoForge a on 1.21.5) | both, a | both, a | both, a | both, a |
| C2ME OpenCL | both, a | none | Fabric a on 1.21.11 | both, a (26.1.2) | both, a | none yet |
| Noisiumed / Noisium | both | both (up to 1.21.6) | none | none | none | none |
| Lithium | both | both | both | both | both | both |
| ModernFix | both | both on 1.21.4 | none | NeoForge only | none | none |
| AllTheLeaks | NeoForge | none | none | none | none | none |
| Sodium | both | both (b on 1.21.5) | both (b on 1.21.11) | both | both | both, a |
| Connector | NeoForge b | none | none | NeoForge b (26.1.2) | none | none |

What that means for the port:

- **1.21.1** is the best covered version: every mod has a native build on both loaders, so the stand-ins go:
  Radium, Canary and Embeddium are no longer needed on NeoForge (official Lithium and Sodium exist), and C2ME comes
  from `c2me-neoforge` on NeoForge. Voxy joins the Fabric dev runs here (see the roadmap).
- **1.21.2 and later** lose AllTheLeaks, and past 1.21.6 Noisium: with no world generation mod but C2ME, the light
  relies on C2ME, which is alpha on every version.
- **26.x**: Noisium's job may be moot: 26.3 rewrote vanilla's noise filling (see `26.3.md`). Check whether Noisiumed
  resumes before listing a replacement. ModernFix is NeoForge-only on 26.1.x and absent later.
- Mark mods with no build as `availableFrom` (not yet) or leave the loader list empty (not on that loader); they stay
  listed, greyed out, rather than disappear.

## Porting checklist

1. `python docs/porting/tools/perfmods.py` for the overview, then
   `python docs/porting/tools/perfmods.py <version> <loader>` for each loader: the newest builds with the ids to pin.
2. Update `PerformanceMods.MINECRAFT_VERSION` and `MODS`: the builds per loader (best first), stand-ins only where the
   mod itself has none, `availableFrom` for mods that don't exist yet. The loaders become `fabric` and `neoforge`;
   `ModLoaderUtil.loaderName()` and the `FORGE` constant change with them.
3. Update `PerformanceModsTest` for the new counts, and the dev dependencies in each loader's `build.gradle`.
4. Check each mod's mod id in its jar (`fabric.mod.json` `id`, `META-INF/neoforge.mods.toml` `modId`); forks
   sometimes keep the original's id, and Connector turns `-` in Fabric ids into `_`.
5. Run a dedicated server of each loader with the mods and ReTerraForged (`tools/servers/bench.sh` and
   `tools/servers/smoke.sh`): a fresh world, `/rtf locate` a few landforms, forceload an area, no errors in the
   log. C2ME and Noisium change the chunk pipeline ReTerraForged hooks into, so this is the part most likely to break.
6. Run the dev client once to see the section: the light, and a click opening a mod's page.
