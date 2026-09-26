package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.tile.Tile;

/**
 * The curated presets each promise a certain kind of world. Measure the terrain they generate and check that each
 * actually differs from the Default preset in the way its description says.
 */
public class PresetCharacterTest {
	private static final Map<String, Stats> STATS = new LinkedHashMap<>();

	record Stats(float land, float height, float mountains, float flat, float badlands, float volcanoes, float temperature, float biomeChanges) {

		@Override
		public String toString() {
			return String.format("land %.3f  height %.3f  mountains %.3f  flat %.3f  badlands %.3f  volcanoes %.3f  temperature %.3f  biome changes %.3f", this.land, this.height, this.mountains, this.flat, this.badlands, this.volcanoes, this.temperature, this.biomeChanges);
		}
	}

	@BeforeAll
	static void measure() {
		TestBootstrap.init();
		BuiltinPresetRenderTest.presets().forEach((name, supplier) -> {
			Stats stats = measure(supplier.get());
			STATS.put(name, stats);
			System.out.printf("%-16s %s%n", name, stats);
		});
	}

	private static Stats measure(Preset preset) {
		Tile wide = generate(preset, 80.0F, false);

		int[] counts = new int[8];
		float[] temperature = new float[1];
		float[] height = new float[1];
		wide.iterate((cell, x, z) -> {
			counts[0]++;
			if (cell.terrain.isSubmerged()) {
				return;
			}
			counts[1]++;
			if (cell.terrain.isMountain()) {
				counts[2]++;
			}
			if (cell.terrain.isFlat()) {
				counts[3]++;
			}
			String name = cell.terrain.getName();
			if (name.contains("badlands") || name.contains("plateau")) {
				counts[4]++;
			}
			if (cell.terrain.isVolcano()) {
				counts[5]++;
			}
			temperature[0] += cell.temperature;
			height[0] += cell.height;
		});

		int size = wide.getBlockSize().size();
		int changes = 0;
		int pairs = 0;
		for (int z = 0; z < size; z++) {
			BiomeType previous = null;
			for (int x = 0; x < size; x++) {
				Cell cell = wide.lookup(x, z);
				if (cell.terrain.isSubmerged()) {
					previous = null;
					continue;
				}
				if (previous != null) {
					pairs++;
					if (previous != cell.biomeType) {
						changes++;
					}
				}
				previous = cell.biomeType;
			}
		}

			// continents can be tens of thousands of blocks across, so judge the land share from far out
		int[] landCounts = new int[2];
		generate(preset, 240.0F, false).iterate((cell, x, z) -> {
			landCounts[0]++;
			if (!cell.terrain.isSubmerged()) {
				landCounts[1]++;
			}
		});

		float land = Math.max(1, counts[1]);
		return new Stats(landCounts[1] / (float) landCounts[0], height[0] / land, counts[2] / land, counts[3] / land, counts[4] / land, counts[5] / land, temperature[0] / land, changes / (float) Math.max(1, pairs));
	}

	// onLand centers the view on the continent nearest the origin, like the preview does for spawn
	private static Tile generate(Preset preset, float zoom, boolean onLand) {
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		float x = 0.0F;
		float z = 0.0F;
		if (onLand) {
			long center = context.localHeightmap.get().continent().getNearestCenter(0.0F, 0.0F);
			x = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackLeft(center);
			z = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackRight(center);
		}
		return context.generator.generateZoomed(x, z, zoom, false).join();
	}

	private static Stats stats(String preset) {
		return STATS.get(preset);
	}

	@Test
	void archipelagoIsMostlySea() {
		assertLess("land", stats("archipelago").land(), stats("default").land() * 0.6F);
	}

	@Test
	void supercontinentIsMostlyLand() {
		assertMore("land", stats("supercontinent").land(), stats("default").land() * 1.25F);
	}

	@Test
	void highlandsAreHighAndRarelyFlat() {
		assertMore("height", stats("highlands").height(), stats("default").height() * 1.05F);
		assertLess("flat", stats("highlands").flat(), stats("default").flat() * 0.6F);
	}

	@Test
	void prairieIsFlat() {
		assertMore("flat", stats("prairie").flat(), stats("default").flat() * 2.0F);
		assertLess("height", stats("prairie").height(), stats("default").height());
	}

	@Test
	void badlandsAreHotWithMoreBadlandsAndPlateaus() {
		assertMore("badlands", stats("badlands").badlands(), stats("default").badlands() * 1.4F);
		assertMore("temperature", stats("badlands").temperature(), stats("default").temperature() + 0.3F);
	}

	@Test
	void frozenNorthIsCold() {
		assertLess("temperature", stats("frozen_north").temperature(), stats("default").temperature() - 0.3F);
	}

	@Test
	void tropicsAreHot() {
		assertMore("temperature", stats("tropics").temperature(), stats("default").temperature() + 0.2F);
	}

	@Test
	void volcanicIslesHaveMoreVolcanoesAndSea() {
		assertMore("volcanoes", stats("volcanic_isles").volcanoes(), stats("default").volcanoes() * 1.5F);
		assertLess("land", stats("volcanic_isles").land(), stats("default").land());
	}

	@Test
	void patchworkChangesBiomeOften() {
		assertMore("biome changes", stats("patchwork").biomeChanges(), stats("default").biomeChanges() * 2.0F);
	}

	private static void assertMore(String what, float actual, float threshold) {
		assertTrue(actual > threshold, what + " " + actual + " should be above " + threshold);
	}

	private static void assertLess(String what, float actual, float threshold) {
		assertTrue(actual < threshold, what + " " + actual + " should be below " + threshold);
	}
}
