### New in 1.0.1
* Other dimensions generate as their own again. With TerraBlender installed, the Nether, the Aether, Deeper Darker and other dimensions were cut off chunk by chunk at the overworld's height, in flat steps, and got its rivers and lakes.
* Without TerraBlender, generating the Nether or the End crashed the server.
* No more shipwrecks and ocean ruins on dry ground: islands and peninsulas no longer get ocean biomes.
* New worlds leave vanilla's overworld density functions alone, so modded dimensions built on them keep their own terrain. Existing worlds keep the preset they were created with.
* Works next to C2ME's OpenCL module: UltraTerraForged's worlds generate as usual instead of stopping the game; vanilla dimensions still use the graphics card.
* Fabric: needs Fabric API 0.155.2+26.2 or newer (it asked for 0.161.0+26.2).
* Terrain is the same as in 1.0.0; only biomes on islands and peninsulas change, in newly generated chunks.

### New in 1.0.0
* Vanilla's variant biomes generate: cherry groves, pale gardens, sunflower plains, ice spikes, bamboo and sparse jungles, old growth birch forests and pine taigas, frozen peaks, eroded badlands and windswept savannas. They never did: every region got the plain version of its biome.
* Wetlands are swamps and mangrove swamps, and rivers are rivers and frozen rivers, as in vanilla.
* Chunks get the biome `/locate` finds. A chunk could get another biome than the one `/locate` reported there, even a land biome in the sea.
* Works next to ModernFix: its world generation optimization no longer fails to apply.
* Trees grow on grass again: since 26.1 they only grew on bare dirt, and forests and jungles had few of them.
* Terrain is the same as in 0.1.0-alpha.1; biomes and trees differ, so in existing worlds new chunks can meet old ones at a visible seam.

### UltraTerraForged
* UltraTerraForged is a continuation of ReTerraForged (by raccoonman, Steveplays28 and EdoEquin0x), which continued TerraForged (by dags and Won-Ton). Full credit and history are in the README and this repository, a fork of ReTerraForged.
* It has its own mod id, `ultraterraforged`, and isn't compatible with worlds made with ReTerraForged. The first start copies `config/reterraforged` to `config/ultraterraforged`, keeping saved presets and modpack setups. Dedicated servers use `level-type=ultraterraforged:ultraterraforged`.
* The first release for Minecraft 26.2, on Fabric (with Fabric API) and NeoForge 26.2. It needs Java 25, as Minecraft 26.2 does. Its terrain generation is the same as in the 1.20.1, 1.21.1 and 26.1 releases.

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
* The Terrain tab recommends performance mods and shows which are installed: C2ME, Lithium and Sodium; C2ME OpenCL is optional, as it needs a graphics card with OpenCL. With them, new land generates as fast as vanilla or faster. Noisium, ModernFix and AllTheLeaks have no 26.2 builds.
* Rock shelters no longer recompute the terrain for every block around a chunk (up to a third faster chunk generation in badlands and plateaus).

### Fixes
* Fixed an ArrayIndexOutOfBoundsException generating chunks, from terrain tiles reused after being closed.
* Fixed from ReTerraForged: rivers generated before the terrain (no rivers at all), missing cave and structure settings, presets losing their scales, `/rtf locate` (now `/utf locate`) sending players to random places, bare rock over the sea floor, and a crash placing some trees.
