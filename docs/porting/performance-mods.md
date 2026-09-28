# Performance mods

UltraTerraForged recommends a set of performance mods, shows in the Create World screen which of them are installed, and
runs them in the dev environment. Which builds exist changes with every Minecraft version and loader, so this page
records what was found for each version, what was tested with UltraTerraForged, and what to change when porting.

State as of 2026-09-28, on the 26.1 branch (Fabric and NeoForge, Minecraft 26.1 to 26.1.2). Each version branch keeps
its own copy of this page; the findings for earlier versions are kept below.

## Where it lives

| What | Where |
|---|---|
| The list of mods, their mod ids and the builds per loader | `common/.../compat/performance/PerformanceMods.java` (data only) |
| The Terrain tab section and its light | `common/.../client/gui/createworld/PerformanceModsSection.java`, wired in `TerrainTab` |
| Tests of the light rules | `common/src/test/.../PerformanceModsTest.java` |
| The dev runs' mods | `fabric/build.gradle` (`modLocalRuntime`, `devModsFolder`), `neoforge/build.gradle` (`devMods`), pinned |
| Repositories | root `build.gradle`: Modrinth maven (`maven.modrinth`), Cursemaven (`curse.maven`) |
| Availability lookup | `docs/porting/tools/perfmods.py` |
| Server tests: timing chunk generation, running console commands | `docs/porting/tools/servers/` |

`-Putf.perfMods=false` runs the dev game without them, e.g. to measure or debug UltraTerraForged on its own.

What the dev runs need that a real game doesn't:

- **NeoForge (1.21.1): released jars don't work unchanged either.** Released NeoForge mods name Minecraft's lambdas as
  javac does (`lambda$fillFromNoise$11`), which the real game keeps, but Loom's dev jar renames them, so any mixin aimed
  at one fails in dev (C2ME, Noisiumed, Lithium, ModernFix, AllTheLeaks and Sodium all have some; a crash or a silently
  skipped fix). So `neoforge/build.gradle`'s `unpackDevMods` downloads the mods while Gradle configures the project,
  takes out the jars nested in them (Loom drops those: C2ME's modules are in `META-INF/jars/`), and puts every mod
  through Loom (`modLocalRuntime`), which renames the lambdas, and the libraries on the classpath as game libraries.
  MixinExtras is left to NeoForge. **The NeoForge dev runs use Embeddium instead of Sodium**: Sodium's jar is an
  early-loading service with the mod nested in it, and the mod uses the service's classes, which it can't reach once
  Loom has remapped them.
- **1.20.1 only: the Forge dev game runs with Mojang's names; released Forge mods use SRG names**, so released jars can't just go in
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
    `forge/src/main/resources/META-INF/MANIFEST.MF` names UltraTerraForged's mixin configs, as the released jar's does.
- **Sodium refused the 1.20.1 dev game's LWJGL** (3.3.2 against the 3.3.1 launchers ship), so the Fabric runs set
  `sodium.checks.issue2561=false`. Check whether that's still needed after a port: newer Sodium versions require
  other LWJGL versions, and the property name follows Sodium's issue number.

### The light

Only mods with a build for the running Minecraft version, loader and Java version count; a mod that only runs through
Sinytra Connector counts once Connector is installed. A build that needs a newer Java than the game runs on
(`Build.needsJava`) shows "Needs Java N" and doesn't count.

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
| [C2ME OpenCL](https://modrinth.com/mod/c2me-ocl) | C2ME add-on: parts of world generation on the GPU | `c2me-opts-accel-opencl` (Fabric), `c2me_opts_accel_opencl` (NeoForge, nested) | From 1.21.1, alpha. Needs C2ME, **Java 25** and an OpenCL device |
| [Noisiumed](https://modrinth.com/mod/noisiumed) / [Noisium](https://modrinth.com/mod/noisium) | faster block placement when filling chunks | `noisiumed`, `noisium` | Noisium is archived; Noisiumed is the maintained fork. Either counts; never install both. Noisiumed refuses to run under Connector |
| [Lithium](https://modrinth.com/mod/lithium) | ticking, AI, physics, some world generation | `lithium` | No Forge build: [Radium](https://modrinth.com/mod/radium) (`radium`, preferred) or [Canary](https://modrinth.com/mod/canary) (`canary`) stand in. Official NeoForge builds from 1.21.1 |
| [ModernFix](https://modrinth.com/mod/modernfix) | loading time, memory | `modernfix` | Overwrites the same biome temperature cache method as Lithium; the log warns, it's harmless |
| [AllTheLeaks](https://www.curseforge.com/minecraft/mc-mods/alltheleaks) | memory leak fixes | `alltheleaks` | CurseForge only. Forge 1.20.1, NeoForge 1.21.1; no Fabric |
| [Sodium](https://modrinth.com/mod/sodium) | rendering | `sodium` | Client only. No Forge build: [Embeddium](https://modrinth.com/mod/embeddium) (`embeddium`) stands in. Official NeoForge builds from 1.21.1 |
| [Sinytra Connector](https://modrinth.com/mod/connector) | runs Fabric mods on Forge/NeoForge | `connector` | Needs [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api). Not a performance mod; lets the Forge list count Fabric-only mods |

## 26.2 (current, the `26.2` branch)

The same setup as 26.1.2 (released jars straight into the NeoForge dev game's mods folder). ModernFix has no 26.2 build
on either loader, so the light asks for three mods on both: C2ME, Lithium and Sodium, with C2ME OpenCL optional.

| Mod | Fabric | NeoForge 26.2.0.88 |
|---|---|---|
| C2ME | 0.4.2-alpha.0.52 (`LmKTn6Yc`) | 0.4.2-alpha.0.96 (`Gj7cXd5I`) |
| C2ME OpenCL | 0.4.2-alpha.0.52 (`K2szayOS`), optional | 0.4.2-alpha.0.96 (`V5SBeTwQ`), optional |
| Noisium | none | none |
| Lithium | 0.25.3 (`f7vZ0VWU`) | 0.25.3 (`J9CowDXK`) |
| ModernFix | none | none |
| AllTheLeaks | none | none |
| Sodium | 0.9.2 (`xJZxADzI`) | 0.9.2 (`DmnNKsfS`) |

The Fabric dev game runs Voxy 0.2.19 (`LzyXnE51`).

## 26.1.2 (the `26.1` branch)

On 26.1 the game runs on Java 25 and isn't obfuscated, so the dev game and a real one use the same names: released jars
go into the NeoForge dev game's mods folder as they are (`syncDevMods`), and the 1.21.1 unpack-and-remap step is gone.

| Mod | Fabric | NeoForge 26.1.2 |
|---|---|---|
| C2ME | 0.4.0-alpha.0.62 (`o1m4A4Rk`) | 0.4.0-alpha.0.99 (`C4T7lj0z`) |
| C2ME OpenCL | 0.4.0-alpha.0.62 (`FIySyMEQ`), optional | 0.4.0-alpha.0.99 (`wJrPp4yA`), optional |
| Noisium | none | none |
| Lithium | 0.24.7 (`Oqq8TOAV`) | 0.24.7 (`eZ0KJiEA`) |
| ModernFix | none | 5.27.22 (`j7EoxpYe`) |
| AllTheLeaks | none | none |
| Sodium | 0.9.2 (`tZQ3jqnf`) | 0.9.2 (`zg4YQ9EL`) |

The Fabric dev game also runs Voxy 0.2.18 (`Zt3LPI0b`, Fabric only), to look at generated terrain from far off, as the
roadmap asked once a build existed. C2ME OpenCL stays optional: with no Noisium it has nothing to conflict with, but it
needs a graphics card with OpenCL. The light asks for three mods on Fabric and four on NeoForge.

## 1.21.1

Every mod has a build for both loaders except AllTheLeaks (NeoForge only), so Radium, Canary and Sinytra Connector drop
out. Embeddium stays as an alternative to Sodium on NeoForge: it counts when installed, Sodium is what's recommended. What the dev runs use:

| Mod | Fabric | NeoForge 21.1 |
|---|---|---|
| C2ME | 0.4.0-alpha.0.29 (`RMHYO1LJ`) | 0.4.0-alpha.0.122 (`c2me-neoforge`, `yxOYFgnK`) |
| C2ME OpenCL | 0.4.0-alpha.0.29 (`j7FyNUo9`), not in the dev runs: Java 25 | 0.4.0-alpha.0.122 (`shL6D1IO`), the same |
| Noisium | Noisiumed 3.0.6 (`8mNj0Di8`) | Noisiumed 3.0.6 (`U3gpLtpO`) |
| Lithium | 0.15.4 (`N08Z8wog`) | 0.15.4 (`DDUrRVCA`) |
| ModernFix | 5.25.1 (`NnNX8LBn`) | 5.27.24 (`5HLHxQ2F`) |
| AllTheLeaks | none | 1.1.13 (`curse.maven:alltheleaks-1091339:8943912`) |
| Sodium | 0.8.13 (`SMxNOGZ6`) | 0.8.13 (`uMOpc5uV`); Embeddium 1.0.15 (`J7b96IEd`) also counts, and is what the dev runs use |

C2ME OpenCL is `optional` in `PerformanceMods` (see below): it counts towards the light only once installed. So the
light asks for five mods on Fabric and six on NeoForge.

### Java 25

The launcher runs 1.21.1 on Java 21, but C2ME's 0.4.0 line (June 2026 on) builds two modules for newer Java:

- `c2me-opts-natives-math`, nested in C2ME itself, needs Java 25 (22 in the 0.3.0 line). On Java 21 Fabric simply
  skips it and C2ME runs without it.
- **C2ME OpenCL depends on that module, so it needs Java 25.** On Java 21 Fabric refuses to start
  ("requires version 25 or later of Java") and NeoForge crashes (`UnsupportedClassVersionError`). Hence
  `needsJava(25)` in `PerformanceMods` and its absence from the dev runs, which use the project's Java 21.
- **On Java 25, C2ME OpenCL and Noisiumed crash the game together**, on both loaders: OpenCL replaces the chunk filling
  that Noisiumed's Lithium compatibility mixin (`compat.lithium.LithiumNoiseChunkGeneratorMixin`) injects into
  ("cannot inject into net/minecraft/world/level/levelgen/NoiseBasedChunkGenerator"). Noisiumed is the one the light
  asks for (a release, on Java 21, no GPU needed); C2ME OpenCL is marked `optional`: shown as "Optional", with its
  description saying to pick one or the other, and counted only when installed.

### Tested with UltraTerraForged

Dedicated servers of each loader, seed 1111, default preset, `/utf locate river`, `/utf locate cirque`, 256
forceloaded chunks, on Java 21:

- Without performance mods, and with C2ME, Noisiumed, Lithium, ModernFix (and AllTheLeaks on NeoForge): both loaders
  generate and locate the same terrain (the nearest river at 3440, 1552; the nearest cirque at 6224, -48), with no
  errors. The only warnings are the mods' optional mixins probing for mods that aren't there (Starlight, Sodium on a
  server).
- NeoForge applies the preset's biome modifiers through UTF's one NeoForge biome modifier
  (`Applying the 30 biome modifiers of the world's preset` in the log).

## 1.20.1 (the 1.20.1 branch)

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

### Tested with UltraTerraForged

Dedicated servers, fresh world with the default preset, 1024 forceloaded chunks, Ryzen 7 9800X3D, measured with
`tools/servers/bench.sh`:

| Setup | Seed 1111 | Seed 2222 | Seed 3333 |
|---|---|---|---|
| Vanilla, Fabric | 14.5 s | 14.2 s | 14.3 s |
| UltraTerraForged, Fabric | 16.8 s | 18.9 s | 18.8 s |
| UltraTerraForged, Fabric + C2ME, Noisiumed, Lithium, ModernFix | 14.2 s | 14.2 s | 11.9 s |
| UltraTerraForged, Forge | 19.0 s | | 16.4 s |
| UltraTerraForged, Forge + Noisiumed, Radium, ModernFix, AllTheLeaks | 18.8 s | | |
| UltraTerraForged, Forge + C2ME through Connector | 16.8 s | | 16.4 s |
| UltraTerraForged, Forge + all of the above | 14.2 s | | 11.9 s |

C2ME does most of the work, but only with the rest: on Forge, C2ME alone gained 0–12 %, the others alone 1 %, all of
them together 25–28 %, which brings UltraTerraForged to vanilla's speed or better on both loaders. Every setup passed the
`/utf locate` smoke test with the same results.

- **Forge crashed at startup with Noisiumed, ModernFix or AllTheLeaks** (a `ClassCastException` in MixinExtras'
  `FactoryRedirectWrapperMixinTransformer`, loading `NoiseBasedChunkGenerator`). Not the mods' fault: Fabric's Mixin
  0.17.4 changed `at` of `@Redirect`, `@ModifyArg`, `@ModifyArgs` and `@ModifyVariable` from `At` to `At[]`, so common
  code compiled against it (through the Fabric loader dependency) carries arrays; Forge's Mixin accepts them, but
  MixinExtras up to 0.5.0, which those mods bundle, casts to a single annotation. Fixed by compiling common code against
  sponge-mixin 0.17.3 (`common/build.gradle`); `MixinAnnotationsTest` fails if an array comes back. **Keep this in mind
  on every port**: whenever the Fabric loader is bumped, check the pin still applies, and run a NeoForge/Forge server
  with a MixinExtras-bundling mod (ModernFix is in most packs).
- C2ME changes how chunks are scheduled. UltraTerraForged's hooks on the chunk status tasks (queueing tiles before
  generation, dropping them after features) still run under it; `/utf locate` and generation work, no errors.
- **C2ME on Forge through Sinytra Connector works** (Connector 1.0.0-beta.49, Forgified Fabric API 0.92.6, C2ME
  0.2.0+alpha.11.18): its modules load, UltraTerraForged generates and locates as on Fabric. The only errors in the log are
  Forgified Fabric API's client screen mixins being skipped on a dedicated server. So `PerformanceMods` lists C2ME for
  Forge as a Connector build: it counts once Connector is installed, and its row links to Connector until then.
  The Forge dev runs include it too (see above for what that takes).
- **C2ME OpenCL doesn't run on 1.20.1**, tried with its 1.21.1 build (0.4.0-alpha.0.13) on Java 25: Fabric loader
  refuses it, as it needs C2ME's density function compiler (`c2me-opts-dfc`) and chunk system rewrite
  (`c2me-rewrites-chunk-system`), which 1.20.1's C2ME doesn't have. With those dependencies overridden it crashes
  applying its mixins to a class added in 1.21 (`class_9310`). It stays listed as "Needs 1.21.1+".

## Later versions

From 1.21 on UltraTerraForged targets Fabric and NeoForge only. What exists (newest build per version, found
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
5. Run a dedicated server of each loader with the mods and UltraTerraForged (`tools/servers/bench.sh` and
   `tools/servers/smoke.sh`): a fresh world, `/utf locate` a few landforms, forceload an area, no errors in the
   log. C2ME and Noisium change the chunk pipeline UltraTerraForged hooks into, so this is the part most likely to break.
6. Run the dev client once to see the section: the light, and a click opening a mod's page.
