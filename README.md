# UltraTerraForged

Realistic, configurable terrain for Minecraft: continents and mountain ranges, rivers that follow the land, coasts,
caves and sea floors worth exploring, all set up from a Terrain tab when you create a world.

UltraTerraForged is a continuation of [ReTerraForged](https://github.com/racoonman2/ReTerraForged), which itself
continued [TerraForged](https://github.com/TerraForged/TerraForged). It only shapes the world: it adds no biomes, and
works alongside the mods that do.

- Minecraft 26.3, on Fabric (with Fabric API) and NeoForge 26.3 (beta), with Java 25. Each Minecraft version has its
  own branch: see [`26.2`](https://github.com/Pandaismyname1/ultraterraforged/tree/26.2),
  [`26.1`](https://github.com/Pandaismyname1/ultraterraforged/tree/26.1) (26.1 to 26.1.2),
  [`1.21.1`](https://github.com/Pandaismyname1/ultraterraforged/tree/1.21.1) and
  [`1.20.1`](https://github.com/Pandaismyname1/ultraterraforged/tree/1.20.1) (Fabric, Forge and NeoForge 47.1).
- Maintained by pandaismyname1
- Download: [Modrinth](https://modrinth.com/mod/ultraterraforged),
  [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ultraterraforged) (both awaiting moderation until the first
  release), or the [GitHub releases](https://github.com/Pandaismyname1/ultraterraforged/releases)

UltraTerraForged is an unofficial continuation, not made or endorsed by the TerraForged or ReTerraForged authors, and is
developed with the help of AI so it can keep up with new Minecraft releases.

## Credits

UltraTerraForged stands on years of other people's work:

- **[TerraForged](https://github.com/TerraForged/TerraForged)**, by **[dags](https://github.com/dags-)** and
  **[Won-Ton](https://github.com/Won-Ton)** (with a contribution from [codehz](https://github.com/codehz)), created the
  terrain engine: continents, climate, terrain regions, rivers and erosion. The heart of the world generation is still
  theirs.
- **[ReTerraForged](https://github.com/racoonman2/ReTerraForged)**, by **[raccoonman](https://github.com/racoonman2)**,
  with **[Steveplays28](https://github.com/Steveplays28)** and **[EdoEquin0x](https://github.com/EdoEquin0x)**, brought
  TerraForged to Minecraft 1.19 and later, rebuilt on the vanilla noise router, with presets as datapacks.
- **UltraTerraForged** is maintained by **[pandaismyname1](https://github.com/Pandaismyname1)**. This repository is a
  fork of ReTerraForged, so the full history of both projects, and everyone's commits, are kept in it.

Both are MIT licensed, as is UltraTerraForged; their copyright notices are kept in [LICENSE](LICENSE). If you enjoy
this mod, the credit for the ideas it's built on belongs to them.

## Why a continuation

ReTerraForged was left partway through: its 1.20.1 branch no longer compiled, and the newest work, for 1.20.2, was
unfinished (plateau-only terrain, rivers generated before the terrain they cut, and cave and structure
settings removed). The mod did nothing until a preset was applied, and its settings were hidden behind a Customize
button. UltraTerraForged picks it up to:

1. make it work, polished, on 1.20.1 for Fabric, Forge and NeoForge, with settings anyone can find;
2. take the terrain much further, with landforms, coasts, oceans and caves the original never had;
3. keep it fast, and measured;
4. then carry it to current Minecraft versions (1.21.1 and the 26.x releases, on Fabric and NeoForge).

As it now differs a great deal, it has its own mod id, `ultraterraforged`. It isn't compatible with worlds made with
ReTerraForged, and can be installed next to it. Its settings folder moves from `config/reterraforged` to
`config/ultraterraforged`; the first start copies the old one over, keeping saved presets and modpack setups.

## What's new

### Creating a world

- **Terrain tab** in Create World: pick a preset, adjust the main settings with sliders, and watch a live map of the
  result (biomes, terrain types, heights). UltraTerraForged is the default world type (configurable).
- **Full editor** a click away: every setting, with search, per-page reset, a marker on what differs from the preset
  you started from, and translated names.
- **Presets**: a set of distinct built-in worlds (Default, Archipelago, Supercontinent, Highlands, Prairie, Badlands,
  Frozen North, Tropics, Volcanic Isles, Waterlands, Patchwork) plus the original TerraForged presets. Save your own,
  or share one as a short code to copy and paste.
- **Dedicated servers** create UltraTerraForged worlds from a preset file (see below).

### Landforms

Buttes and mesas, canyons, sea cliffs and sea stacks, volcanic surfaces, fjords, atolls, dunes, tors, salt flats,
alluvial fans, glacial valleys and cirques, moraines and drumlins, barrier islands with lagoons, karst with
sinkholes and cenotes, and river deltas. Each can be switched off or tuned.

### Rivers

- Rivers wind through the land instead of cutting straight across it.
- Rivers and lakes can sit above sea level, stepping down in waterfalls; in cold places the falls freeze.
- River beds and banks suit their setting: gravel and cobbles in the mountains, sand and mud in the lowlands.
- Gorges where rivers cut through high ground.

### Coasts

Peninsulas, headlands and bays, coastal islands tied to the shore by tombolos, sand spits, skerries, chains of
volcanic islands, and islands in rivers and lakes. Beaches and biomes follow the reshaped coastline.

### Oceans

Continental shelves ending in a drop-off, submarine canyons, deep trenches, seamounts and flat-topped guyots,
mid-ocean ridges with hydrothermal vents, blue holes, coral reefs near the surface, rippled sand waves, whale falls,
and a sea floor of sand, gravel and clay instead of bare rock.

### Caves

Caves shaped by the land above them, without changing any biome: underground rivers, springs on hillsides, karst
caves, lava tubes below volcanoes, sea caves and arches in cliff coasts, caves along the rock layers, giant caverns
with underground lakes, rock shelters under cliffs, and ice caves under glaciers. Cave mouths open in slopes and cliffs
rather than in flat fields.

### Surface

- The snow line follows the sun: lower on north-facing slopes.
- Rock layers built from the stone of vanilla and any installed mod that tags its rocks, varying by region.

### Finding things

`/utf locate <terrain>` finds the nearest landform, coast or sea-floor feature and puts you on its surface, e.g.
`/utf locate cirque`, `/utf locate blue_hole`.

### Performance

- The Terrain tab recommends performance mods and shows with a light which of them are installed: C2ME, Lithium and
  Sodium. With them, UltraTerraForged generates new land as fast as
  vanilla, or faster.
- Tested and benchmarked with each; see [docs/porting/performance-mods.md](docs/porting/performance-mods.md).

### Fixes over ReTerraForged

Rivers generated before the terrain (no rivers at all), cave and structure settings missing, terrain presets losing
their scales, `/rtf locate` (now `/utf locate`) sending players to random places, bare rock over every sea floor, crashes when generating
chunks and when placing some trees, and a startup crash on Forge next to mods that bundle MixinExtras (ModernFix,
Noisium, AllTheLeaks and many more).

## Dedicated servers

Set `level-type=ultraterraforged:ultraterraforged` in `server.properties` before the world is created. The first start
writes the default preset to `config/ultraterraforged/server-preset.json`; edit it, or ship your own, to choose the
server's terrain.

## For modpack authors

A modpack can choose the terrain its players start with, without taking away their ability to change it. Everything
lives in the `config/ultraterraforged` folder, in files the mod never writes, so a modpack update can replace them
without touching anything a player saved.

1. Tune a preset in the Terrain tab of Create World and click **Save As...**. It's written to
   `config/ultraterraforged/presets/<name>.json`.
2. Move that file to `config/ultraterraforged/modpack-presets/`. Presets in this folder appear first in the preset list
   and can't be deleted or overwritten from the game. Optionally add `"name"` and `"description"` fields next to the
   settings; they are shown in the preset list and may be translation keys.
3. Create `config/ultraterraforged/modpack.json`:

   ```json
   {
     "defaultPreset": "My Preset",
     "showBuiltinPresets": true,
     "useAsDefaultWorldType": true
   }
   ```

   | Key | Default | Meaning |
   | --- | --- | --- |
   | `defaultPreset` | `default` | The preset new worlds start from: a file name in `modpack-presets` (without `.json`), a built-in preset such as `highlands`, or a full id such as `builtin/highlands`. Unknown names fall back to Default and log a warning. |
   | `showBuiltinPresets` | `true` | Set to `false` to offer only the modpack's presets. Ignored if `modpack-presets` has none. |
   | `useAsDefaultWorldType` | `true` | Whether UltraTerraForged is the selected world type in Create World. A player's own `client.json` takes precedence. |

Players still start from the modpack's default, can switch to any other preset, edit every setting, and save their own
presets. Dedicated servers seed `config/ultraterraforged/server-preset.json` from the same default the first time.

## Compatibility

- Biome mods (TerraBlender-based ones included) place their biomes into UltraTerraForged's terrain.
- World Preview shows UltraTerraForged worlds.
- Rock mods join the rock layers automatically when they tag their stone as vanilla does.

## Maven

Builds are published to the `maven` branch of this repository on every release: `ultraterraforged-fabric` and
`ultraterraforged-neoforge` (`ultraterraforged-forge` for 1.20.1), each with a `sources` classifier, under the group
`com.pandaismyname1.ultraterraforged`. Versions look like `1.0.1+26.3`.

```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "UltraTerraForged maven"
                url = "https://raw.githubusercontent.com/Pandaismyname1/ultraterraforged/maven"
            }
        }
        filter { includeGroup "com.pandaismyname1.ultraterraforged" }
    }
}

dependencies {
    implementation "com.pandaismyname1.ultraterraforged:ultraterraforged-fabric:1.0.1+26.3"
}
```

## Building

Gradle and the mod use Java 25 (Minecraft 26.x isn't obfuscated, so nothing is remapped): `./gradlew build`. The Fabric and NeoForge dev games include the recommended
performance mods; `-Putf.perfMods=false` leaves them out. `./gradlew :common:test` runs the tests, which
include checks that the built-in presets still generate the same terrain. Notes for porting to newer Minecraft
versions are in [docs/porting](docs/porting).

GitHub Actions builds both loaders on every push (`build.yml`); the tests take long, so they run locally only. Releases are started by hand
(`release.yml`, Actions → Release → Run workflow, choosing alpha, beta or release): they create the GitHub release
from [CHANGELOG.md](CHANGELOG.md), then upload the Fabric and NeoForge jars to Modrinth (`LpyUCfpY`) and CurseForge
(`1715379`), using the `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` repository secrets; a platform whose secret is missing is
skipped. Bump `mod_version` in `gradle.properties` and update the changelog first: the version tag must be new.
Publishing a release also adds its Maven artifacts to the `maven` branch (`publish-maven.yml`), next to those of
earlier releases and other Minecraft versions.

## License

MIT; see [LICENSE](LICENSE).
