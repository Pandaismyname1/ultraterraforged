# Tools and verification

Scripts in [`tools/`](tools) were used to produce these docs. Re-run them on every Minecraft bump; they take minutes, not hours.

## Setup

All the tools read jars that Loom has already cached in `~/.gradle/caches/fabric-loom/<version>/`, so run any Gradle build for a version once before analysing it. JDKs expected in `~/.jdks`: `graalvm-ce-21.0.2` (1.21.1) and `openjdk-25.0.1` (26.x; its `javap` also reads older class files).

```bash
python docs/porting/tools/dumpjar.py ~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar work/maps/26.3.json
```

`dumpjar.py` parses class files directly (no `javap`, no dependencies) and writes every `net.minecraft`/`com.mojang` class with its supertypes, fields and method descriptors. For the obfuscated 1.20.1/1.21.1, dump the Mojang-named jar Loom made: `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<ver>-loom.mappings.<…>-v2/*.jar`. For 26.x, dump the plain `minecraft-merged.jar`. The version list at the top of `q.py`, `checktargets.py` and `checkaw.py` must match the dumps in `work/maps/`.

## Mixin target check

```bash
cd work && python ../docs/porting/tools/checktargets.py .
```

This prints, for every mixin target method or field (and every `@At` INVOKE target), its descriptor in each version, and only the lines where it changes. Current output: [census/mixin-targets.txt](census/mixin-targets.txt). Add new targets to the `T` list when mixins are added.

It can't see:
- **Lambda targets.** `lambda$foo$N` indices are assigned by javac in source order, and on NeoForge they come from the *patched* class. Check these with `javap -p` on the exact jar that runs: for NeoForge, `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/neoforge-<ver>-minecraft-merged*/…`.
- **Local-variable layouts**, for `@ModifyVariable(index = …)` and `LocalCapture`. There are 2 of those in UTF plus the TerraBlender one. Prefer MixinExtras `@Local` with a type, or drop them.
- **Anonymous classes** (`RandomState$1` in the 26.3 plan). Add them to the list once the mixin exists.

## Access widener check

```bash
python docs/porting/tools/checkaw.py work common/src/main/resources/ultraterraforged.accesswidener
```

This prints each entry as `ok` or `BREAKS@<first version>` with the reason: class gone, member missing, descriptor changed, or field type changed. It knows the class renames (`ResourceLocation` → `Identifier`, `BootstapContext` → `BootstrapContext`, `Util` → `util.Util`). **Loom silently ignores entries whose targets don't exist**, so this script is the only thing that notices. The same check applies to the hand-written NeoForge `accesstransformer.cfg` from 26.1. Current output: [census/aw-check.txt](census/aw-check.txt): 3 entries break at 1.21.1, 4 more at 26.1, 5 more at 26.2 and 43 more at 26.3. Only 21 of the 76 survive to 26.3, and about 20 are unused today (see the [inventory](inventory.md)), so prune first.

## Member lookups

```bash
cd work && python ../docs/porting/tools/q.py 'world/level/levelgen/NoiseChunk:<init>' 'client/Minecraft:setScreen'
```

For any class and member prefix, this prints how the members change across the dumped versions. It answers most "what's it called now" questions in one line. For bodies, use `javap -c -p -cp <jar> <class>`.

## Compile census

```bash
bash docs/porting/tools/census-build.sh 1.21.1 1.21.1-4.1.0.8 /tmp/utf-census
bash docs/porting/tools/census-build.sh 26.3   26.3-26.3.0.0.7 /tmp/utf-census
python docs/porting/tools/census.py /tmp/utf-census/out 1.21.1=/tmp/utf-census/1.21.1.log 26.3=/tmp/utf-census/26.3.log
```

This builds the current `common` sources against another version in a throwaway copy (`git archive HEAD`; the repo isn't touched) and turns the javac output into a per-file Markdown list. When given several logs in order, the later reports only list errors that are new since the previous version. The files in [census/](census) came from exactly this. Limits:
- javac stops attributing a class whose supertypes don't resolve, so fixing the first wave reveals more.
- The census covers `common` only; `fabric`/`neoforge` are small.
- It says nothing about mixins or runtime behaviour.

## Vanilla datagen

```bash
bash docs/porting/tools/vanilla-datagen.sh 26.3 build/vanilla-datagen
```

This runs Mojang's data generator (`--server --reports`) from the cached server jar. You get the exact vanilla `data/minecraft/worldgen/**` JSON and `reports/registries.json` for that version. Use it to:
- **Diff noise settings, routers, features, biomes and dimension types** that UTF re-creates in code (`PresetNoiseRouterData`, `PresetConfiguredFeatures`, `PresetBiomeData`, `PresetDimensionTypes`, `PresetNoiseParameters`). The most version-sensitive code in UTF is the code that copies vanilla.
- **Diff registries between versions.** This is how the 26.3 registry renames in [26.3.md](26.3.md) were found.
- **Compare against UTF's generated preset pack.** Unzip `ultraterraforged-preset*.zip` from a test world and diff it against vanilla's `noise_settings/overworld.json`: the keys should match exactly.

## Runtime verification per hop

In rough order of cost:

1. **Headless tests** (`./gradlew :common:test`). `TestBootstrap` bootstraps vanilla registries, and the suite checks preset round-trips, datapack generation, terrain sensitivity and **golden terrain fingerprints**.
   - 1.21.1, 26.1, 26.2: fingerprints must **not** change. A diff is a porting bug. Watch the `getMaxY` off-by-one ([26.1.md](26.1.md#silent-changes-compile-fine-behave-differently)).
   - 26.3: density samplers work in `float`, not `double`, so tiny fingerprint drift is expected. Regenerate them deliberately (`-Drtf.updateGolden=true`) only after checking `utf.render` images side by side with the 26.2 output.
   - `TestBootstrap` reflects on `MappedRegistry.frozen`. Re-check the field name per version (`q.py 'core/MappedRegistry:frozen'`).
2. **Target and AW scripts** (above), zero `BREAKS` expected after each hop.
3. **Dedicated servers**, all loaders (`scratchpad/servers/smoke2.sh` pattern): `level-type=ultraterraforged:ultraterraforged` + `server-preset.json`, generate spawn, check the log for mixin apply failures (`defaultRequire: 1` makes them fatal, which is what we want) and datapack errors ("missing following references").
4. **Client run.** Per the standing preference, **ask the user before running `:fabric:runClientTest`**: it opens a window on their desktop. The clienttest drives world creation through the Terrain tab and takes screenshots; it's the only automated check of the GUI, and the GUI changes on every hop.
5. **Mod compat smoke:** TerraBlender plus one biome mod (BOP), and on 26.1.x World Preview Prime.
