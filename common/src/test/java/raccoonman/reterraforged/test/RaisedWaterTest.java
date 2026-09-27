package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;
import raccoonman.reterraforged.world.worldgen.rivermap.Rivermap;
import raccoonman.reterraforged.world.worldgen.rivermap.RiverRoutes;
import raccoonman.reterraforged.world.worldgen.rivermap.river.Network;
import raccoonman.reterraforged.world.worldgen.rivermap.river.River;
import raccoonman.reterraforged.world.worldgen.terrain.populator.RiverPopulator;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * Rivers, lakes and wetlands with their water above the sea, following the land.
 */
public class RaisedWaterTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	private static Preset preset() {
		return BuiltinPresetRenderTest.presets().get("default").get();
	}

	// places on rivers whose water lies well above the sea, spread over a few rivermaps
	private static List<float[]> riverSpots(Preset preset, int count) {
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		Heightmap heightmap = context.localHeightmap.get();
		Levels levels = heightmap.levels();
		List<float[]> spots = new ArrayList<>();
		for (float[] at : new float[][] { { 0.0F, 0.0F }, { 6000.0F, 2000.0F }, { -5000.0F, 4000.0F } }) {
			Cell cell = new Cell();
			heightmap.applyContinent(cell, at[0], at[1]);
			Rivermap rivermap = Rivermap.get(cell, null, heightmap);
			try {
				for (Network network : (Network[]) field(Rivermap.class, "networks").get(rivermap)) {
					spots(network, levels, spots);
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
		Collections.shuffle(spots, new Random(1));
		return spots.subList(0, Math.min(count, spots.size()));
	}

	private static void spots(Network network, Levels levels, List<float[]> spots) {
		RiverPopulator carver = network.riverCarver();
		for (float t = 0.1F; t < 0.9F; t += 0.1F) {
			if (carver.waterLevelAt(t) >= levels.water(10)) {
				long world = RiverRoutes.channelInWorld(carver, t);
				spots.add(new float[] { PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world) });
			}
		}
		for (Network child : network.children()) {
			spots(child, levels, spots);
		}
	}

	@Test
	void waterStaysInItsBanks() {
		Preset preset = preset();
		int wet = 0;
		int leaks = 0;
		for (float[] spot : riverSpots(preset, 5)) {
			TerrainViews.View view = TerrainViews.view(preset, spot[0], spot[1], 1.0F);
			for (int x = 1; x < view.size() - 1; x++) {
				for (int z = 1; z < view.size() - 1; z++) {
					if (!view.raisedWater(x, z)) {
						continue;
					}
					wet++;
					int top = view.waterY(x, z);
					for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
						int nx = x + step[0];
						int nz = z + step[1];
						// water beside it, raised or the sea's, may be lower: the river steps down there
						boolean water = view.raisedWater(nx, nz) || view.blockY(nx, nz) < view.levels().waterLevel;
						if (!water && view.blockY(nx, nz) < top) {
							leaks++;
							break;
						}
					}
				}
			}
		}
		System.out.println(wet + " cells of raised water, " + leaks + " beside lower dry ground");
		assertTrue(wet > 5000, "only " + wet + " cells of raised water");
		assertEquals(0, leaks, leaks + " cells of raised water lie beside lower dry ground");
	}

	@Test
	void riversRunNearTheLandNotInTrenches() {
		Preset preset = preset();
		Preset legacy = preset.copy();
		legacy.rivers().raisedWater = false;
		long depth = 0;
		long legacyDepth = 0;
		int cells = 0;
		int raised = 0;
		for (float[] spot : riverSpots(preset, 5)) {
			TerrainViews.View view = TerrainViews.view(preset, spot[0], spot[1], 1.0F);
			TerrainViews.View old = TerrainViews.view(legacy, spot[0], spot[1], 1.0F);
			for (int x = 32; x < view.size() - 32; x += 2) {
				for (int z = 32; z < view.size() - 32; z += 2) {
					if (!view.cell(x, z).terrain.isRiver() || !old.cell(x, z).terrain.isRiver()) {
						continue;
					}
					cells++;
					if (view.waterY(x, z) >= view.levels().waterY + 10) {
						raised++;
					}
					// how deep the river lies below the highest land within 32 blocks
					depth += highest(view, x, z) - view.waterY(x, z);
					legacyDepth += highest(old, x, z) - old.levels().waterY;
				}
			}
		}
		float mean = depth / (float) cells;
		float legacyMean = legacyDepth / (float) cells;
		System.out.printf("%d river cells, %d with their water 10 or more blocks above the sea; the land within 32 blocks rises %.1f blocks above the water, %.1f with sea-level rivers%n", cells, raised, mean, legacyMean);
		assertTrue(cells > 500, "only " + cells + " river cells");
		assertTrue(raised > cells / 4, "only " + raised + " of " + cells + " river cells are well above the sea");
		assertTrue(mean < legacyMean * 0.6F, "rivers lie " + mean + " blocks below the land around them, " + legacyMean + " at sea level");
	}

	private static int highest(TerrainViews.View view, int x, int z) {
		int highest = Integer.MIN_VALUE;
		for (int d = -32; d <= 32; d += 4) {
			highest = Math.max(highest, Math.max(view.blockY(x + d, z), view.blockY(x, z + d)));
		}
		return highest;
	}

	@Test
	void forksMeetTheirRiverAndCutNoDeeperThanAGorge() throws Exception {
		Preset preset = preset();
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		Heightmap heightmap = context.localHeightmap.get();
		Levels levels = heightmap.levels();
		Cell cell = new Cell();
		heightmap.applyContinent(cell, 0.0F, 0.0F);
		Rivermap rivermap = Rivermap.get(cell, null, heightmap);
		Network[] networks = (Network[]) field(Rivermap.class, "networks").get(rivermap);
		Domain warp = (Domain) field(Rivermap.class, "riverWarp").get(rivermap);
		int[] counts = new int[5];
		for (Network network : networks) {
			assertEquals(levels.water, network.riverCarver().waterLevelAt(1.0F), 1.0E-6F, "a river doesn't reach the sea at sea level");
			check(network, warp, heightmap, preset.rivers().gorgeDepth, counts);
		}
		System.out.println(counts[0] + " forks, " + counts[1] + " points along rivers checked, " + counts[2] + " in gorges, " + counts[3] + " with water more than 4 blocks above the land beside it, " + counts[4] + " cutting more than 8 blocks deeper than a gorge");
		assertTrue(counts[0] > 10 && counts[1] > 1000, "too few rivers checked");
		// the land is measured every 16 blocks along a river; a dip in between can leave the water above it, held in by
		// an embankment
		assertTrue(counts[3] < counts[1] / 50, counts[3] + " points with water above the land beside it");
		assertTrue(counts[4] < counts[1] / 50, counts[4] + " points cutting deeper than a gorge");
	}

	private static void check(Network network, Domain warp, Heightmap heightmap, int gorgeDepth, int[] counts) {
		Levels levels = heightmap.levels();
		RiverPopulator carver = network.riverCarver();
		River river = carver.river;
		for (float t = 0.02F; t < 0.98F; t += 0.02F) {
			float water = carver.waterLevelAt(t);
			long channel = RiverRoutes.channel(carver, t);
			float lowest = Float.MAX_VALUE;
			float side = carver.config.bankWidth + 4.0F;
			for (float offset : new float[] { 0.0F, side, -side }) {
				long world = carver.frame.toWorld(PosUtil.unpackLeftf(channel) + river.normX * offset, PosUtil.unpackRightf(channel) + river.normZ * offset);
				lowest = Math.min(lowest, heightmap.sampleGround(PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world)).height);
			}
			counts[1]++;
			// between samples the land can dip or rise, so allow a few blocks
			if (lowest > levels.water(4)) {
				float cut = (lowest - water) * levels.worldHeight;
				// the water is level for 16 blocks at a time, and the land can rise over that
				if (cut > gorgeDepth + 8) {
					counts[4]++;
				}
				if (cut < -4) {
					counts[3]++;
				}
				if (cut > gorgeDepth / 2) {
					counts[2]++;
				}
			}
		}
		for (Network child : network.children()) {
			counts[0]++;
			float junction = carver.waterLevelAt(child.riverCarver().junction);
			assertEquals(junction, child.riverCarver().waterLevelAt(1.0F), 1.0E-6F, "a fork doesn't meet its river at its river's level");
			check(child, warp, heightmap, gorgeDepth, counts);
		}
	}

	private static Field field(Class<?> type, String name) throws NoSuchFieldException {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	@Test
	void windingRiversClimbLess() throws Exception {
		Preset winding = preset();
		Preset straight = preset();
		straight.rivers().winding = false;
		long start = System.nanoTime();
		float[] on = climbs(winding);
		long middle = System.nanoTime();
		float[] off = climbs(straight);
		System.out.printf("preparing the rivermaps took %d ms winding, %d ms straight%n", (middle - start) / 1_000_000L, (System.nanoTime() - middle) / 1_000_000L);
		System.out.printf("winding rivers: %.0f blocks of river, %.0f climbs, %.0f blocks climbed, %.0f gorge samples, %.0f%% well above the sea; straight: %.0f climbs, %.0f blocks climbed, %.0f gorge samples, %.0f%% well above the sea%n", on[0], on[1], on[2], on[3], on[5] * 100.0F / on[4], off[1], off[2], off[3], off[5] * 100.0F / off[4]);
		assertTrue(on[2] < off[2] * 0.6F, "winding rivers climb " + on[2] + " blocks, straight ones " + off[2]);
	}

	// over the rivers of a few rivermaps: their length, how often and how far their water climbs going downstream, and how
	// often it lies more than half a gorge below the land beside it
	private static float[] climbs(Preset preset) throws Exception {
		GeneratorContext context = GeneratorContext.makeUncached(preset, PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		Heightmap heightmap = context.localHeightmap.get();
		float[] totals = new float[6];
		java.util.Set<Rivermap> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (float[] at : new float[][] { { 0.0F, 0.0F }, { 6000.0F, 2000.0F }, { -5000.0F, 4000.0F }, { 2000.0F, -6000.0F } }) {
			Cell cell = new Cell();
			heightmap.applyContinent(cell, at[0], at[1]);
			Rivermap rivermap = Rivermap.get(cell, null, heightmap);
			if (!seen.add(rivermap)) {
				continue;
			}
			for (Network network : (Network[]) field(Rivermap.class, "networks").get(rivermap)) {
				climbs(network, heightmap, preset.rivers().gorgeDepth, totals);
			}
		}
		return totals;
	}

	private static void climbs(Network network, Heightmap heightmap, int gorgeDepth, float[] totals) {
		RiverPopulator carver = network.riverCarver();
		Levels levels = heightmap.levels();
		int samples = (int) (carver.river.length / 16.0F);
		totals[0] += carver.river.length;
		float previous = carver.waterLevelAt(0.0F);
		for (int i = 1; i <= samples; i++) {
			float t = i / (float) samples;
			float water = carver.waterLevelAt(t);
			if (water > previous + levels.unit * 0.5F) {
				totals[1]++;
				totals[2] += (water - previous) * levels.worldHeight;
			}
			previous = water;
			totals[4]++;
			if (water >= levels.water(10)) {
				totals[5]++;
			}
			long world = RiverRoutes.channelInWorld(carver, t);
			float ground = heightmap.sampleGround(PosUtil.unpackLeftf(world), PosUtil.unpackRightf(world)).height;
			if ((ground - water) * levels.worldHeight > gorgeDepth / 2) {
				totals[3]++;
			}
		}
		for (Network child : network.children()) {
			climbs(child, heightmap, gorgeDepth, totals);
		}
	}

	@Test
	void legacyPresetsKeepWaterAtSeaLevel() {
		for (String name : new String[] { "legacy_default", "beautiful", "huge_biomes", "lite", "vanillaish" }) {
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().rivers().raisedWater, name);
		}
		assertTrue(preset().rivers().raisedWater);
	}
}
