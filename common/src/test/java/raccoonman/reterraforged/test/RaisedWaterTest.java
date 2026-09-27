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
import raccoonman.reterraforged.world.worldgen.rivermap.WaterLevels;
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

	// places by rivers on ground well above the sea, spread over a wide area
	private static List<float[]> riverSpots(Preset preset, int count) {
		TerrainViews.View wide = TerrainViews.view(preset, 0.0F, 0.0F, 16.0F);
		List<float[]> spots = new ArrayList<>();
		for (int x = 8; x < wide.size() - 8; x++) {
			for (int z = 8; z < wide.size() - 8; z++) {
				if (wide.cell(x, z).riverDistance < 0.1F && wide.blockY(x, z) >= wide.levels().waterLevel + 10) {
					spots.add(new float[] { wide.blockX(x), wide.blockZ(z) });
				}
			}
		}
		Collections.shuffle(spots, new Random(1));
		return spots.subList(0, Math.min(count, spots.size()));
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
			long channel = WaterLevels.channel(river, carver.warp, t);
			float lowest = Float.MAX_VALUE;
			float side = carver.config.bankWidth + 4.0F;
			for (float offset : new float[] { 0.0F, side, -side }) {
				lowest = Math.min(lowest, ground(PosUtil.unpackLeftf(channel) + river.normX * offset, PosUtil.unpackRightf(channel) + river.normZ * offset, warp, heightmap));
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

	// as WaterLevels measures it, undoing the rivermap's warp
	private static float ground(float x, float z, Domain warp, Heightmap heightmap) {
		float worldX = x;
		float worldZ = z;
		for (int i = 0; i < 2; i++) {
			worldX = x - warp.getOffsetX(worldX, worldZ, 0);
			worldZ = z - warp.getOffsetZ(worldX, worldZ, 0);
		}
		return heightmap.sampleGround(worldX, worldZ).height;
	}

	private static Field field(Class<?> type, String name) throws NoSuchFieldException {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	@Test
	void legacyPresetsKeepWaterAtSeaLevel() {
		for (String name : new String[] { "legacy_default", "beautiful", "huge_biomes", "lite", "vanillaish" }) {
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().rivers().raisedWater, name);
		}
		assertTrue(preset().rivers().raisedWater);
	}
}
