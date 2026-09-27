### UltraTerraForged
* UltraTerraForged is a continuation of ReTerraForged (by raccoonman, Steveplays28 and EdoEquin0x), which continued TerraForged (by dags and Won-Ton). Full credit and history are in the README and this repository, a fork of ReTerraForged.
* It has its own mod id, `ultraterraforged`, and isn't compatible with worlds made with ReTerraForged. The first start copies `config/reterraforged` to `config/ultraterraforged`, keeping saved presets and modpack setups. Dedicated servers use `level-type=ultraterraforged:ultraterraforged`.
* Minecraft 1.21.1 on Fabric and NeoForge; Minecraft 1.20.1 (Fabric, Forge and NeoForge 47.1) has its own build.

### Creating worlds
* A Terrain tab in Create World: presets, the main settings as sliders, and a live map of the result. UltraTerraForged is the default world type (configurable).
* The full editor a click away, with search, per-page reset and a marker on changed settings.
* Built-in presets (Default, Archipelago, Supercontinent, Highlands, Prairie, Badlands, Frozen North, Tropics, Volcanic Isles, Waterlands, Patchwork, and the original TerraForged presets); save your own or share them as codes.
* Dedicated servers create worlds from `config/ultraterraforged/server-preset.json`; modpacks can ship presets and choose the default.

### Terrain
* Landforms: buttes and mesas, canyons, sea cliffs and stacks, volcanic surfaces, fjords, atolls, dunes, tors, salt flats, alluvial fans, glacial valleys and cirques, moraines and drumlins, barrier islands and lagoons, karst with sinkholes and cenotes, river deltas.
* Rivers wind through the land; rivers and lakes can sit above the sea and step down in waterfalls, frozen in the cold; river beds and banks suit their setting; gorges.
* Coasts: peninsulas, headlands and bays, coastal islands and tombolos, spits, skerries, volcanic island arcs, river and lake islands.
* Oceans: continental shelves, submarine canyons, trenches, seamounts and guyots, mid-ocean ridges with hydrothermal vents, blue holes, coral reefs, sand waves, whale falls, and sand, gravel and clay sea floors.
* Caves shaped by the land, biomes untouched: underground rivers, springs, karst caves, lava tubes, sea caves and arches, layered caves, giant caverns, rock shelters, glacier caves; cave mouths in slopes.
* A snow line that depends on which way slopes face; rock layers from vanilla and modded stone.
* `/utf locate <terrain>` finds landforms and puts you on their surface.

### Performance
* The Terrain tab recommends performance mods and shows which are installed: C2ME, C2ME OpenCL, Noisium(ed), Lithium, ModernFix, AllTheLeaks (NeoForge), Sodium. With them, new land generates as fast as vanilla or faster.
* Rock shelters no longer recompute the terrain for every block around a chunk (up to a third faster chunk generation in badlands and plateaus).

### Fixes
* Forge no longer crashes at startup next to mods bundling MixinExtras up to 0.5.0 (ModernFix, Noisium, AllTheLeaks and many others).
* Fixed an ArrayIndexOutOfBoundsException generating chunks, from terrain tiles reused after being closed.
* Fixed from ReTerraForged: rivers generated before the terrain (no rivers at all), missing cave and structure settings, presets losing their scales, `/rtf locate` (now `/utf locate`) sending players to random places, bare rock over the sea floor, and a crash placing some trees.
