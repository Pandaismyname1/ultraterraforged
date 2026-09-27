# Build toolchain per target

Everything here was checked against Maven metadata on 2026-09-27, and by building the `common` module against each version in a throwaway copy (`tools/census-build.sh`). The EMI ports in `~/IdeaProjects/emi` (branches `26.1`, `26.2`, `26.3`) use the same Architectury setup and served as a working reference for the 26.x build files.

## Version matrix

| | 1.20.1 (today) | 1.21.1 | 26.1 / 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|---|
| Java (release / runtime) | 17 | **21** | **25** | 25 | 25 |
| Game jar | obfuscated | obfuscated | **unobfuscated** | unobfuscated | unobfuscated |
| Loom plugin | `dev.architectury.loom` 1.17.493 | `dev.architectury.loom` 1.17.493 | **`dev.architectury.loom-no-remap`** 1.17.493 | same | same |
| Mappings | `loom.officialMojangMappings()` | same | **none** (delete the line) | none | none |
| architectury-plugin | 3.5-SNAPSHOT | 3.5.170 (pin it) | 3.5.170 | 3.5.170 | 3.5.170 |
| Gradle | 9.4 | 9.4 | ≥9.1 for NeoForge; Fabric recommends 9.4. EMI and the census build use 9.5.0. | Fabric recommends 9.5.1 | Fabric recommends 9.6.0 |
| Daemon JVM (`gradle/gradle-daemon-jvm.properties`) | 21 | 21 | **25** | 25 | 25 |
| Loaders | Fabric + Forge (Forge jar also covers NeoForge 47.1) | **Fabric + NeoForge** | Fabric + NeoForge | Fabric + NeoForge | Fabric + NeoForge |
| Fabric Loader | 0.19.5 | 0.19.5 | 0.19.5 | 0.19.5 | 0.19.5 |
| Fabric API (latest) | 0.92.12+1.20.1 | 0.116.17+1.21.1 | 0.145.1+26.1 / **0.155.3+26.1.2** | 0.161.0+26.2 | 0.161.0+26.3 |
| NeoForge (latest) | – | 21.1.252 | 26.1.0.19-beta / **26.1.2.111** | 26.2.0.88 | 26.3.0.23-**beta** |
| TerraBlender (`com.github.glitchfiend:TerraBlender-*`) | 1.20.1-3.0.1.2 | 1.21.1-4.1.0.8 | 26.1-26.1.0.2 / 26.1.2-26.1.2.0.3 | 26.2-26.2.0.0.2 | 26.3-26.3.0.0.7 |
| Data pack format | 15 | 48 | 101.1 (`min_format`/`max_format` scheme) | 107.1 | 121.0 |
| Resource pack format | 15 | 34 | 84.0 | 88.0 | 97.1 |

Pack formats come from each version's `version.json`.

**26.1 vs 26.1.2.** A full class dump of both jars differs in exactly one method (`Checkbox.overflowsRowLimit`, added in 26.1.2), and the compile census is identical. Build against 26.1.2 and declare `>=26.1 <26.2` in the metadata: one jar covers the whole 26.1 line. 26.1.1 was not checked.

**26.3 is beta-only on NeoForge** (`26.3.0.x-beta`, 23 builds so far), unlike 26.2, which has stable 26.2.0.88. TerraBlender 26.3 exists for common, Fabric and NeoForge.

## 1.20.1 → 1.21.1: still the remapping toolchain

Keep `dev.architectury.loom` and Mojang mappings. What changes:

- `java_version=21`. `options.release = 21`. Mixin configs can stay `JAVA_17` (EMI kept `JAVA_17` through 26.3 without problems) or move to `JAVA_21`.
- Replace the `forge` subproject with `neoforge`:
  - `settings.gradle`: `include("common", "fabric", "neoforge")`.
  - `common/build.gradle`: `architectury { common("fabric", "neoforge") }`.
  - `neoforge/gradle.properties`: `loom.platform=neoforge`.
  - `neoforge/build.gradle`: `architectury { platformSetupLoomIde(); neoForge() }`, `neoForge "net.neoforged:neoforge:${neoforge_version}"`, `developmentNeoForge.extendsFrom common`, `shadowBundle(project(path: ":common", configuration: "transformProductionNeoForge"))`.
  - Metadata moves to `META-INF/neoforge.mods.toml`. See [neoforge.md](neoforge.md) for the loader-side details.
- Access widener on NeoForge: Forge used `forge { convertAccessWideners = true }`, which has no NeoForge equivalent. On 1.21.1, use `remapJar { atAccessWideners.add("ultraterraforged.accesswidener") }` or the experimental `loom.neoForge.convertAccessWideners(tasks.remapJar, "ultraterraforged.accesswidener")`. From 26.1 the conversion is broken on loom-no-remap ([architectury-loom#337](https://github.com/architectury/architectury-loom/issues/337)); see [neoforge.md](neoforge.md#build-neoforge). Stale AT lines silently match nothing, so check them with `tools/checkaw.py` like the widener.
- The 1.20.1 note about NeoForge 47.1 reporting as `forge` no longer applies. NeoForge 20.2+ is its own loader id (`neoforge`).
- TerraBlender: compile common against `TerraBlender-common:1.21.1-4.1.0.8` instead of the Forge jar. That's what the census build used, and it resolves from `maven.minecraftforge.net`.
- **Loom silently ignores access-widener entries whose target doesn't exist** (the 1.21.1 census build carried 3 dead entries without a warning). Run `tools/checkaw.py` on every bump.

## 1.21.1 → 26.1: the unobfuscated toolchain

26.1 is the first version Mojang ships without obfuscation. Classic `dev.architectury.loom` cannot build it; use **`dev.architectury.loom-no-remap`**. This is the recipe from the EMI port (fork commit `9d2aa5a1`), and the census build confirmed each step:

1. **Root `build.gradle`:** `id "dev.architectury.loom-no-remap" version "1.17.493" apply false`. Pin `architectury-plugin` to `3.5.170`, not `3.5-SNAPSHOT`.
2. **Delete every `mappings ...` line**, including `loom.officialMojangMappings()` and `silentMojangMappingsLicense()`.
3. **`mod*` configurations become plain ones:** `modImplementation` → `implementation`, `modApi` → `api`, `modCompileOnly` → `compileOnly`, `modLocalRuntime` → `localRuntime`.
4. **Project wiring:** `common(project(path: ":common", configuration: "namedElements"))` becomes `common(project(path: ":common"))`. `transformProductionFabric` and `transformProductionNeoForge` stay.
5. **Jars:** remove `remapJar`/`remapSourcesJar`. The release jar is the `shadowJar` with classifier `""`, and `jar` gets classifier `"dev"`. `remapJar { injectAccessWidener = true }` has no equivalent. Check that the Fabric jar still carries the widener through `fabric.mod.json`'s `accessWidener` field.
6. **Access widener header: `accessWidener v2 official`**, not `named`. With `named`, Loom fails at configuration time: `Expected official namespace for access widener entry, found: named`. The entries themselves stay in Mojang names, because the "official" names are now the Mojang names.
7. **`gradle/gradle-daemon-jvm.properties`: `toolchainVersion=25`.** With 21, the daemon's compiler fails: `error: release version 25 not supported`. Setting `JAVA_HOME` isn't enough, because this file wins.
8. `options.release = 25`, and `"java": ">=25"` in `fabric.mod.json`.
9. **Refmaps:** there are none (nothing to remap). Mixins that target lambdas by intermediary or SRG name (`method_39464`, `m_289181_`, `method_29440`) must switch to the real `lambda$name$N` names, and those have to be checked with `javap` on every version. Better, retarget them to named methods (see [1.21.1.md](1.21.1.md#chunk-status-hooks)).
10. **Gradle 9:** EMI needed `zipTree(task.get().archiveFile)` where older scripts used `flatMap`, and `DuplicatesStrategy.EXCLUDE` on jars.

From 26.1 to 26.2 to 26.3, the build files only change version numbers. One exception: from 26.2, NeoForge deprecates `logoFile` in favour of `iconFile` plus `bannerFile`.

### `@ExpectPlatform` under loom-no-remap: untested

EMI doesn't use `@ExpectPlatform` (it picks its platform class with `Class.forName`), so the EMI port proves nothing here. UTF has 12 `@ExpectPlatform` methods in 6 classes. Make it the **first spike of the 26.1 port**: build the Fabric and NeoForge jars with one `@ExpectPlatform` method and call it at runtime. If the transformer misbehaves without remapping, the fallback is a `ServiceLoader`, or EMI's `Class.forName` lookup of `com.pandaismyname1.ultraterraforged.platform.<loader>.*Impl`.

## Stonecutter

The roadmap picked Stonecutter (latest 0.9.8) for multi-version. What this exploration found that affects the setup:

- **Two build flavours.** 1.21.1 needs the remapping Loom, Mojang mappings and Java 21. 26.x needs loom-no-remap, no mappings and Java 25. Each Stonecutter node is its own Gradle project, so choose the plugin and JDK per node: for example `plugins { id(if (sc.eval(sc.current.version, ">=26.1")) "dev.architectury.loom-no-remap" else "dev.architectury.loom") }` in a Kotlin build script, or a `versions/<v>/gradle.properties` flag read by the script. The access-widener header differs too (`named` vs `official`), so generate it or keep one file per flavour. **Verify** that Architectury Loom and Architectury plugin tolerate two Loom plugin ids in one settings tree. If they don't, fall back to one Stonecutter tree for 26.x and a plain branch for 1.21.1.
- **Where conditionals will pile up.** From the census, the per-version differences cluster in:
  - the client GUI (every hop touches it)
  - `data/preset/*` (vanilla bootstrap builders)
  - the mixins
  - on 26.3, all of `world/worldgen/{densityfunction,surface,feature,feature/placement}`

  The rest of UTF (noise library, tiles, rivers, continents, cache, `preset/option`) touches almost no Minecraft API. Keep it version-free.
- **26.3 needs class-level swaps, not line-level `//? if` blocks.** Its worldgen classes have no line-by-line resemblance to 26.2's. See the bridge suggestion in [26.3.md](26.3.md#strategy-options).

## CI

- 1.21.1: `actions/setup-java` with 21. The current workflow already runs on 21.
- 26.x: temurin 25. EMI's `build.yml` runs `:fabric:build` then `:neoforge:build` on JDK 25 and caches `~/.gradle/caches/fabric-loom`.
- Artifact names change from `*-forge.jar` to `*-neoforge.jar`. Update the upload globs in `.github/workflows/build.yml`.
