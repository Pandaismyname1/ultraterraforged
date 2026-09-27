package raccoonman.reterraforged.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.heightmap.TerrainLocator;
import raccoonman.reterraforged.world.worldgen.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Where the newer landforms turn up in the built-in presets: run by hand, with -Drtf.survey=true.
 */
public class LandformSurveyTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void desertRelief() {
		if (!Boolean.getBoolean("rtf.survey.desert")) {
			return;
		}
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		raccoonman.reterraforged.world.worldgen.heightmap.Heightmap heightmap = context.localHeightmap.get();
		java.util.Random random = new java.util.Random(3);
		java.util.List<Float> reliefs = new java.util.ArrayList<>();
		java.util.List<Float> heights = new java.util.ArrayList<>();
		java.util.Map<String, Integer> terrains = new java.util.TreeMap<>();
		int desert = 0;
		for (int i = 0; i < 20000; i++) {
			float x = random.nextFloat() * 40000 - 20000;
			float z = random.nextFloat() * 40000 - 20000;
			raccoonman.reterraforged.world.worldgen.cell.Cell cell = heightmap.sampleTerrain(x, z);
			if (cell.biomeType != raccoonman.reterraforged.world.worldgen.biome.type.BiomeType.DESERT || cell.height < context.levels.water) {
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
		if (!Boolean.getBoolean("rtf.survey")) {
			return;
		}
		Terrain[] terrains = { TerrainType.SALT_FLAT, TerrainType.ALLUVIAL_FAN, TerrainType.GLACIAL_VALLEY, TerrainType.CIRQUE, TerrainType.DRUMLINS, TerrainType.MORAINE, TerrainType.BARRIER_ISLAND, TerrainType.KARST, TerrainType.SINKHOLE, TerrainType.DELTA };
		for (String name : System.getProperty("rtf.survey.presets", "default").split(",")) {
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
