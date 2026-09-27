package com.pandaismyname1.ultraterraforged.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.TerrainLocator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Where the newer landforms turn up in the built-in presets: run by hand, with -Dutf.survey=true.
 */
public class LandformSurveyTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void seaFloor() {
		if (!Boolean.getBoolean("utf.survey.sea")) {
			return;
		}
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		TerrainViews.View view = TerrainViews.view(preset, 0.0F, 0.0F, 32.0F);
		int water = view.levels().waterLevel;
		java.util.Map<Integer, Integer> depths = new java.util.TreeMap<>();
		java.util.Map<Integer, Integer> edges = new java.util.TreeMap<>();
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				int y = view.blockY(x, z);
				if (y < water) {
					depths.merge((water - y) / 5 * 5, 1, Integer::sum);
					edges.merge((int) (view.cell(x, z).continentEdge * 20), 1, Integer::sum);
				}
			}
		}
		System.out.println("SURVEY sea depths by 5 blocks " + depths);
		System.out.println("SURVEY sea continent edge by 0.05 " + edges);
	}

	@Test
	void shorelineEdge() {
		if (!Boolean.getBoolean("utf.survey.shore")) {
			return;
		}
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		TerrainViews.View view = TerrainViews.view(preset, 0.0F, 0.0F, 4.0F);
		int water = view.levels().waterLevel;
		java.util.List<Float> edges = new java.util.ArrayList<>();
		java.util.List<Float> above = new java.util.ArrayList<>();
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				int y = view.blockY(x, z);
				if (y == water - 1 || y == water) {
					edges.add(view.cell(x, z).continentEdge);
				}
				if (y >= water + 5 && y <= water + 6) {
					above.add(view.cell(x, z).continentEdge);
				}
			}
		}
		java.util.Collections.sort(edges);
		java.util.Collections.sort(above);
		for (float q : new float[] { 0.1F, 0.25F, 0.5F, 0.75F, 0.9F }) {
			System.out.println("SURVEY shore q" + q + " edge at the water " + edges.get((int) (q * edges.size())) + ", 5 blocks up " + above.get((int) (q * above.size())));
		}
	}

	@Test
	void gravelBeachCoasts() {
		if (!Boolean.getBoolean("utf.survey.gravel")) {
			return;
		}
		String[] off = { "none", "headlands", "peninsulas", "coastalIslands", "spits", "all" };
		for (String feature : off) {
			Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
			preset.landforms().canyons.enabled = false;
			preset.landforms().buttes.enabled = false;
			preset.landforms().seaCliffs.gravelBeaches = 1.0F;
			switch (feature) {
				case "headlands" -> preset.coasts().headlands.enabled = false;
				case "peninsulas" -> preset.coasts().peninsulas.enabled = false;
				case "coastalIslands" -> preset.coasts().coastalIslands.enabled = false;
				case "spits" -> preset.coasts().spits.enabled = false;
				case "all" -> { preset.coasts().headlands.enabled = false; preset.coasts().peninsulas.enabled = false; preset.coasts().coastalIslands.enabled = false; preset.coasts().spits.enabled = false; }
				default -> {}
			}
			TerrainViews.View view = TerrainViews.view(preset, 0.0F, 0.0F, 4.0F);
			int water = view.levels().waterLevel;
			int beaches = 0;
			int under = 0;
			for (int x = 4; x < view.size() - 4; x++) {
				for (int z = 4; z < view.size() - 4; z++) {
					if (view.cell(x, z).terrain != TerrainType.SHINGLE_BEACH) {
						continue;
					}
					beaches++;
					int highest = 0;
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							highest = Math.max(highest, view.blockY(x + dx, z + dz));
						}
					}
					if (highest >= water + 6) {
						under++;
					}
				}
			}
			System.out.println("SURVEY " + feature + " off: " + beaches + " beaches, " + under + " under cliffs");
		}
	}

	@Test
	void windingRivers() throws java.io.IOException {
		if (!Boolean.getBoolean("utf.survey.rivers")) {
			return;
		}
		Preset on = BuiltinPresetRenderTest.presets().get("default").get();
		Preset off = on.copy();
		off.rivers().winding = false;
		float zoom = Float.parseFloat(System.getProperty("utf.survey.zoom", "8"));
		TerrainViews.View wide = TerrainViews.view(on, 0.0F, 0.0F, 16.0F);
		int found = 0;
		for (int x = 8; x < wide.size() - 8 && found < 4; x += 17) {
			for (int z = 8; z < wide.size() - 8 && found < 4; z += 13) {
				if (wide.cell(x, z).riverDistance < 0.1F && wide.blockY(x, z) >= wide.levels().waterLevel + 10) {
					float bx = wide.blockX(x);
					float bz = wide.blockZ(z);
					System.out.println("SURVEY river spot " + bx + " " + bz);
					TerrainViews.write(TerrainViews.view(on, bx, bz, zoom), "rivers_winding_" + found);
					TerrainViews.write(TerrainViews.view(off, bx, bz, zoom), "rivers_straight_" + found);
					found++;
				}
			}
		}
	}

	@Test
	void saltCracks() {
		if (!Boolean.getBoolean("utf.survey.salt")) {
			return;
		}
		com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise noise = com.pandaismyname1.ultraterraforged.data.preset.PresetSurfaceNoise.makeSaltFlat();
		float[] values = new float[200 * 200];
		for (int x = 0; x < 200; x++) {
			for (int z = 0; z < 200; z++) {
				values[x * 200 + z] = noise.compute(x * 1.3F, z * 1.3F, 0);
			}
		}
		java.util.Arrays.sort(values);
		for (float q : new float[] { 0.5F, 0.8F, 0.85F, 0.9F, 0.92F, 0.95F }) {
			System.out.println("SURVEY salt q" + q + " " + values[(int) (q * values.length)]);
		}
	}

	@Test
	void desertRelief() {
		if (!Boolean.getBoolean("utf.survey.desert")) {
			return;
		}
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap heightmap = context.localHeightmap.get();
		java.util.Random random = new java.util.Random(3);
		java.util.List<Float> reliefs = new java.util.ArrayList<>();
		java.util.List<Float> heights = new java.util.ArrayList<>();
		java.util.Map<String, Integer> terrains = new java.util.TreeMap<>();
		int desert = 0;
		for (int i = 0; i < 20000; i++) {
			float x = random.nextFloat() * 40000 - 20000;
			float z = random.nextFloat() * 40000 - 20000;
			com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell cell = heightmap.sampleTerrain(x, z);
			if (cell.biomeType != com.pandaismyname1.ultraterraforged.world.worldgen.biome.type.BiomeType.DESERT || cell.height < context.levels.water) {
				continue;
			}
			desert++;
			terrains.merge(cell.terrain.getName(), 1, Integer::sum);
			float lowest = cell.height;
			float highest = cell.height;
			for (int j = 0; j < 16; j++) {
				double angle = j * Math.PI / 8.0D;
				float r = j % 2 == 0 ? 40.0F : 80.0F;
				float h = heightmap.sampleGround(x + (float) Math.cos(angle) * r, z + (float) Math.sin(angle) * r).height;
				lowest = Math.min(lowest, h);
				highest = Math.max(highest, h);
			}
			reliefs.add((highest - lowest) * context.levels.worldHeight);
			heights.add((lowest - context.levels.water) * context.levels.worldHeight);
		}
		java.util.Collections.sort(reliefs);
		java.util.Collections.sort(heights);
		System.out.println("SURVEY desert samples " + desert + " terrains " + terrains);
		for (float q : new float[] { 0.05F, 0.1F, 0.25F, 0.5F, 0.75F }) {
			System.out.println("SURVEY q" + q + " relief " + reliefs.get((int) (q * reliefs.size())) + " height " + heights.get((int) (q * heights.size())));
		}
	}

	@Test
	void survey() {
		if (!Boolean.getBoolean("utf.survey")) {
			return;
		}
		String names = System.getProperty("utf.survey.terrains", "salt_flat,alluvial_fan,glacial_valley,cirque,drumlins,moraine,barrier_island,karst,sinkhole,delta");
		Terrain[] terrains = java.util.Arrays.stream(names.split(",")).map(TerrainType::get).toArray(Terrain[]::new);
		for (String name : System.getProperty("utf.survey.presets", "default").split(",")) {
			Preset preset = BuiltinPresetRenderTest.presets().get(name).get();
			GeneratorContext context = GeneratorContext.makeCached(preset, PresetRenderer.SEED, 3, 6, false);
			for (Terrain terrain : terrains) {
				long start = System.currentTimeMillis();
				TerrainLocator.Found found = TerrainLocator.locate(context.lookup, terrain, 0, 0, 12000, 30_000L);
				System.out.println("SURVEY " + name + " " + terrain.getName() + ": " + (found == null ? "none" : found.x() + " " + found.z()) + " in " + (System.currentTimeMillis() - start) + " ms");
			}
		}
	}
}
