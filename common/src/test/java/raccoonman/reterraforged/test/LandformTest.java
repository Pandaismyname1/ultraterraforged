package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * Each landform is measured by generating the same place with it on and off.
 */
public class LandformTest {
	// a desert and badlands area of the Badlands preset, with several buttes
	private static final float DESERT_X = -304.0F;
	private static final float DESERT_Z = -128.0F;
	private static final int BORDER = 25;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	private static Preset preset(String name, Consumer<Preset> change) {
		Preset preset = BuiltinPresetRenderTest.presets().get(name).get();
		change.accept(preset);
		return preset;
	}

	@Test
	void buttesOnlyRaiseDryLand() {
		TerrainViews.View with = TerrainViews.view(preset("badlands", (p) -> {}), DESERT_X, DESERT_Z, 3.0F);
		TerrainViews.View without = TerrainViews.view(preset("badlands", (p) -> p.landforms().buttes.enabled = false), DESERT_X, DESERT_Z, 3.0F);
		int raised = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				int rise = with.blockY(x, z) - without.blockY(x, z);
				assertTrue(rise >= 0, "buttes lowered the ground at " + x + ", " + z);
				if (rise > 0) {
					Cell cell = without.cell(x, z);
					assertTrue(!cell.terrain.isSubmerged() && !cell.terrain.isRiver(), "a butte rose out of " + cell.terrain + " at " + x + ", " + z);
				}
				if (rise > 10) {
					raised++;
				}
			}
		}
		float share = raised / (float) (with.size() * with.size());
		System.out.printf("buttes cover %.1f%% of the area%n", share * 100.0F);
		// a few buttes and mesas, not a wall of them
		assertTrue(share > 0.005F && share < 0.25F, "buttes cover " + share);
	}

	@Test
	void butteTopsAreFlat() {
		TerrainViews.View with = TerrainViews.view(preset("badlands", (p) -> {}), DESERT_X, DESERT_Z, 1.0F);
		TerrainViews.View without = TerrainViews.view(preset("badlands", (p) -> p.landforms().buttes.enabled = false), DESERT_X, DESERT_Z, 1.0F);
		int top = 0;
		int flat = 0;
		for (int x = 1; x < with.size() - 1; x++) {
			for (int z = 1; z < with.size() - 1; z++) {
				// well inside a butte: raised a lot, and so are all its neighbours
				if (!raisedAround(with, without, x, z, 15)) {
					continue;
				}
				top++;
				int y = with.blockY(x, z);
				if (Math.abs(with.blockY(x + 1, z) - y) <= 1 && Math.abs(with.blockY(x, z + 1) - y) <= 1) {
					flat++;
				}
			}
		}
		assertTrue(top > 100, "found only " + top + " butte top cells");
		float share = flat / (float) top;
		System.out.printf("%.1f%% of %d butte top cells are flat%n", share * 100.0F, top);
		assertTrue(share > 0.85F, "only " + share + " of butte tops are flat");
	}

	private static boolean raisedAround(TerrainViews.View with, TerrainViews.View without, int x, int z, int amount) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (with.blockY(x + dx, z + dz) - without.blockY(x + dx, z + dz) < amount) {
					return false;
				}
			}
		}
		return true;
	}

	@Test
	void canyonsOnlyCutPlateausAndStayDry() {
		// a plateau area of the Badlands preset
		TerrainViews.View with = TerrainViews.view(preset("badlands", (p) -> p.landforms().buttes.enabled = false), -640.0F, 80.0F, 3.0F);
		TerrainViews.View without = TerrainViews.view(preset("badlands", (p) -> {
			p.landforms().buttes.enabled = false;
			p.landforms().canyons.enabled = false;
		}), -640.0F, 80.0F, 3.0F);
		int floor = with.levels().waterLevel + 3;
		int cut = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				int depth = without.blockY(x, z) - with.blockY(x, z);
				assertTrue(depth >= 0, "canyons raised the ground at " + x + ", " + z);
				if (depth > 0) {
					Cell cell = without.cell(x, z);
					// low ground by the sea is relabelled coast after the landforms are shaped
					boolean shore = cell.terrain == TerrainType.COAST || cell.terrain == TerrainType.BEACH;
					assertTrue(shore || cell.terrain.includes(TerrainType.PLATEAU) || cell.terrain.includes(TerrainType.BADLANDS), "a canyon cut into " + cell.terrain);
					assertTrue(with.blockY(x, z) >= floor - 1, "a canyon cut down to " + with.blockY(x, z) + " at " + x + ", " + z);
				}
				if (depth > 5) {
					cut++;
				}
			}
		}
		float share = cut / (float) (with.size() * with.size());
		System.out.printf("canyons cut %.1f%% of the area%n", share * 100.0F);
		assertTrue(share > 0.01F && share < 0.4F, "canyons cut " + share);
	}

	@Test
	void seaCliffsStayByTheSea() {
		Preset preset = preset("default", (p) -> {
			p.landforms().canyons.enabled = false;
			p.landforms().buttes.enabled = false;
		});
		Preset flat = preset.copy();
		flat.landforms().seaCliffs.enabled = false;
		TerrainViews.View with = TerrainViews.view(preset, 0.0F, 0.0F, 8.0F);
		TerrainViews.View without = TerrainViews.view(flat, 0.0F, 0.0F, 8.0F);
		int water = with.levels().waterLevel;
		int size = with.size();
		// distance in cells to the nearest water in the view without cliffs
		int[][] distance = new int[size][size];
		java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				distance[x][z] = without.blockY(x, z) < water ? 0 : Integer.MAX_VALUE;
				if (distance[x][z] == 0) {
					queue.add(new int[] { x, z });
				}
			}
		}
		while (!queue.isEmpty()) {
			int[] cell = queue.poll();
			for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
				int nx = cell[0] + step[0];
				int nz = cell[1] + step[1];
				if (nx >= 0 && nz >= 0 && nx < size && nz < size && distance[nx][nz] == Integer.MAX_VALUE) {
					distance[nx][nz] = distance[cell[0]][cell[1]] + 1;
					queue.add(new int[] { nx, nz });
				}
			}
		}
		int changed = 0;
		int farthest = 0;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				// the sea near the edge of the view may lie just outside it
				boolean inside = x >= BORDER && z >= BORDER && x < size - BORDER && z < size - BORDER;
				// the erosion filter runs over the whole area, so any change nudges the ground a little elsewhere too
				if (inside && Math.abs(with.blockY(x, z) - without.blockY(x, z)) >= 3) {
					changed++;
					farthest = Math.max(farthest, distance[x][z]);

				}
			}
		}
		int farthestBlocks = (int) (farthest * with.zoom());
		System.out.println("sea cliffs changed " + changed + " cells, at most " + farthestBlocks + " blocks from the sea");
		assertTrue(changed > 50, "sea cliffs changed only " + changed + " cells");
		// the headland behind a cliff slopes back over about 120 blocks
		assertTrue(farthestBlocks <= 200, "sea cliffs changed land " + farthestBlocks + " blocks from the sea");
	}

	@Test
	void seaCliffsMakeSheerDropsIntoTheSea() {
		Preset preset = preset("default", (p) -> {
			p.landforms().canyons.enabled = false;
			p.landforms().buttes.enabled = false;
		});
		Preset flat = preset.copy();
		flat.landforms().seaCliffs.enabled = false;
		// a 1 block per pixel look at the coast where the cliffs change it most
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 8.0F), TerrainViews.view(flat, 0.0F, 0.0F, 8.0F));
		float x = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackLeft(place);
		float z = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackRight(place);
		float edgeWith = shoreHeight(TerrainViews.view(preset, x, z, 1.0F));
		float edgeWithout = shoreHeight(TerrainViews.view(flat, x, z, 1.0F));
		System.out.printf("the tallest tenth of the water's edge stands %.1f blocks above the sea with cliffs, %.1f without%n", edgeWith, edgeWithout);
		assertTrue(edgeWith > edgeWithout + 6.0F, "cliffs raise the water's edge only from " + edgeWithout + " to " + edgeWith);
	}

	// how high the tallest tenth of the land next to water stands above the sea
	private static float shoreHeight(TerrainViews.View view) {
		int water = view.levels().waterLevel;
		java.util.List<Integer> heights = new java.util.ArrayList<>();
		for (int x = 1; x < view.size() - 1; x++) {
			for (int z = 1; z < view.size() - 1; z++) {
				int y = view.blockY(x, z);
				if (y < water) {
					continue;
				}
				if (view.blockY(x + 1, z) < water || view.blockY(x - 1, z) < water || view.blockY(x, z + 1) < water || view.blockY(x, z - 1) < water) {
					heights.add(y - water);
				}
			}
		}
		if (heights.isEmpty()) {
			return 0.0F;
		}
		java.util.Collections.sort(heights);
		return heights.get(heights.size() * 9 / 10);
	}

	@Test
	void gravelBeachesLieUnderTheCliffs() {
		Preset preset = preset("default", (p) -> {
			p.landforms().canyons.enabled = false;
			p.landforms().buttes.enabled = false;
			p.landforms().seaCliffs.gravelBeaches = 1.0F;
		});
		TerrainViews.View view = TerrainViews.view(preset, 0.0F, 0.0F, 4.0F);
		int water = view.levels().waterLevel;
		int beaches = 0;
		int underCliffs = 0;
		for (int x = 4; x < view.size() - 4; x++) {
			for (int z = 4; z < view.size() - 4; z++) {
				if (view.cell(x, z).terrain != TerrainType.SHINGLE_BEACH) {
					continue;
				}
				int y = view.blockY(x, z);
				assertTrue(y >= water - 1 && y <= water + 2, "a gravel beach at " + x + ", " + z + " lies at " + y);
				beaches++;
				// a cliff within 16 blocks; the beach is up to about 15 wide
				int highest = y;
				for (int dx = -4; dx <= 4; dx++) {
					for (int dz = -4; dz <= 4; dz++) {
						highest = Math.max(highest, view.blockY(x + dx, z + dz));
					}
				}
				if (highest >= water + 6) {
					underCliffs++;
				}
			}
		}
		System.out.println(beaches + " cells of gravel beach, " + underCliffs + " of them under a cliff");
		assertTrue(beaches > 20, "only " + beaches + " cells of gravel beach");
		assertTrue(underCliffs >= beaches * 3 / 4, "only " + underCliffs + " of " + beaches + " cells of gravel beach lie under a cliff");

		Preset none = preset.copy();
		none.landforms().seaCliffs.gravelBeaches = 0.0F;
		TerrainViews.View without = TerrainViews.view(none, 0.0F, 0.0F, 4.0F);
		for (int x = 0; x < without.size(); x++) {
			for (int z = 0; z < without.size(); z++) {
				assertTrue(without.cell(x, z).terrain != TerrainType.SHINGLE_BEACH, "a gravel beach with gravel beaches off at " + x + ", " + z);
			}
		}
	}

	@Test
	void seaStacksStandOffTheCliffs() {
		Preset preset = preset("default", (p) -> {
			p.landforms().canyons.enabled = false;
			p.landforms().buttes.enabled = false;
		});
		Preset noStacks = preset.copy();
		noStacks.landforms().seaCliffs.seaStacks = false;
		Preset noCliffs = preset.copy();
		noCliffs.landforms().seaCliffs.enabled = false;
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 8.0F), TerrainViews.view(noCliffs, 0.0F, 0.0F, 8.0F));
		float x = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackLeft(place);
		float z = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackRight(place);
		int with = islets(TerrainViews.view(preset, x, z, 1.0F));
		int without = islets(TerrainViews.view(noStacks, x, z, 1.0F));
		System.out.println("small rock islands off the cliffs: " + with + " with sea stacks, " + without + " without");
		assertTrue(with >= without + 2, with + " islets with stacks, " + without + " without");
	}

	// pieces of land surrounded by water and no bigger than a sea stack
	private static int islets(TerrainViews.View view) {
		int water = view.levels().waterLevel;
		int size = view.size();
		boolean[][] seen = new boolean[size][size];
		int islets = 0;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				if (seen[x][z] || view.blockY(x, z) < water) {
					continue;
				}
				// flood fill this piece of land
				int area = 0;
				boolean touchesBorder = false;
				java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
				queue.add(new int[] { x, z });
				seen[x][z] = true;
				while (!queue.isEmpty()) {
					int[] cell = queue.poll();
					area++;
					for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
						int nx = cell[0] + step[0];
						int nz = cell[1] + step[1];
						if (nx < 0 || nz < 0 || nx >= size || nz >= size) {
							touchesBorder = true;
							continue;
						}
						if (!seen[nx][nz] && view.blockY(nx, nz) >= water) {
							seen[nx][nz] = true;
							queue.add(new int[] { nx, nz });
						}
					}
				}
				if (!touchesBorder && area <= 300) {
					islets++;
				}
			}
		}
		return islets;
	}

	@Test
	void fjordsFloodValleysOnColdCoasts() {
		Preset preset = preset("frozen_north", (p) -> {});
		Preset flat = preset.copy();
		flat.landforms().fjords.enabled = false;
		TerrainViews.View with = TerrainViews.view(preset, 0.0F, 0.0F, 12.0F);
		TerrainViews.View without = TerrainViews.view(flat, 0.0F, 0.0F, 12.0F);
		int water = with.levels().waterLevel;
		int flooded = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				int change = with.blockY(x, z) - without.blockY(x, z);
				// the erosion filter nudges the ground a little everywhere once anything changes
				assertTrue(change <= 2, "fjords raised the ground by " + change + " at " + x + ", " + z);
				if (with.blockY(x, z) < water && without.blockY(x, z) >= water) {
					flooded++;
				}
			}
		}
		System.out.println("fjords flooded " + flooded + " cells of a cold coast");
		assertTrue(flooded > 20, "fjords flooded only " + flooded + " cells");
	}

	@Test
	void noFjordsInTheTropics() {
		Preset preset = preset("tropics", (p) -> {});
		Preset flat = preset.copy();
		flat.landforms().fjords.enabled = false;
		TerrainViews.View with = TerrainViews.view(preset, 0.0F, 0.0F, 12.0F);
		TerrainViews.View without = TerrainViews.view(flat, 0.0F, 0.0F, 12.0F);
		int water = with.levels().waterLevel;
		int flooded = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				if (with.blockY(x, z) < water && without.blockY(x, z) >= water) {
					flooded++;
				}
			}
		}
		assertTrue(flooded < 5, "fjords flooded " + flooded + " cells of a tropical coast");
	}

	@Test
	void atollsRiseInWarmSeasOnly() {
		int warm = newLand("tropics");
		int cold = newLand("frozen_north");
		System.out.println("atolls made " + warm + " cells of land in warm seas, " + cold + " in cold ones");
		assertTrue(warm > 30, "atolls made only " + warm + " cells of land in warm seas");
		assertTrue(cold == 0, "atolls made " + cold + " cells of land in cold seas");
	}

	@Test
	void dunesRiseInHotDesertsOnly() {
		Preset preset = preset("default", (p) -> {});
		Preset flat = preset.copy();
		flat.landforms().dunes.enabled = false;
		// deserts are rare in the default preset, so look for the dunes over a wide area first
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 16.0F), TerrainViews.view(flat, 0.0F, 0.0F, 16.0F));
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		int[] hot = duneRise(preset, flat, x, z, 2.0F);
		System.out.println("dunes by " + x + ", " + z + ": " + hot[0] + " cells raised, " + hot[1] + " of them outside deserts, up to " + hot[2] + " blocks");
		assertTrue(hot[0] > 500, "dunes raised only " + hot[0] + " cells");
		assertTrue(hot[1] <= hot[0] / 20, hot[1] + " of " + hot[0] + " dune cells lie outside deserts");
		assertTrue(hot[2] >= 8 && hot[2] <= 13, "the tallest dune is " + hot[2] + " blocks");
		Preset cold = preset("frozen_north", (p) -> {});
		Preset coldFlat = cold.copy();
		coldFlat.landforms().dunes.enabled = false;
		int[] frozen = duneRise(cold, coldFlat, 0.0F, 0.0F, 16.0F);
		System.out.println("dunes in the frozen north: " + frozen[0] + " cells raised");
		assertTrue(frozen[0] == 0, "dunes raised " + frozen[0] + " cells in the frozen north");
	}

	// cells dunes raise by more than 2 blocks, how many of those aren't desert, and the tallest rise
	private static int[] duneRise(Preset preset, Preset flat, float centerX, float centerZ, float zoom) {
		TerrainViews.View with = TerrainViews.view(preset, centerX, centerZ, zoom);
		TerrainViews.View without = TerrainViews.view(flat, centerX, centerZ, zoom);
		int raised = 0;
		int elsewhere = 0;
		int tallest = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				int rise = with.blockY(x, z) - without.blockY(x, z);
				// the erosion filter nudges the ground a little around anything that changes
				assertTrue(rise >= -2, "dunes lowered the ground by " + -rise + " at " + x + ", " + z);
				if (rise > 2) {
					raised++;
					if (with.cell(x, z).biomeType != BiomeType.DESERT) {
						elsewhere++;
					}
				}
				tallest = Math.max(tallest, rise);
			}
		}
		return new int[] { raised, elsewhere, tallest };
	}

	@Test
	void torsStandOnHilltops() {
		Preset preset = preset("default", (p) -> {});
		Preset flat = preset.copy();
		flat.landforms().tors.enabled = false;
		TerrainViews.View with = TerrainViews.view(preset, 0.0F, 0.0F, 8.0F);
		TerrainViews.View without = TerrainViews.view(flat, 0.0F, 0.0F, 8.0F);
		int rock = 0;
		int tallest = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				int rise = with.blockY(x, z) - without.blockY(x, z);
				// the erosion filter nudges the ground a little around anything that changes
				assertTrue(rise >= -2, "tors lowered the ground by " + -rise + " at " + x + ", " + z);
				if (with.cell(x, z).terrain == TerrainType.TOR) {
					rock++;
					assertTrue(!without.cell(x, z).terrain.isSubmerged(), "a tor stands in " + without.cell(x, z).terrain);
				}
				tallest = Math.max(tallest, rise);
			}
		}
		System.out.println("tors cover " + rock + " cells, rising up to " + tallest + " blocks");
		assertTrue(rock > 20, "tors cover only " + rock + " cells");
		assertTrue(tallest >= 4 && tallest <= preset.landforms().tors.height + 2, "the tallest tor rises " + tallest + " blocks");
	}

	// cells that are land with atolls and sea without, over a wide area
	private static int newLand(String name) {
		Preset preset = preset(name, (p) -> {});
		Preset flat = preset.copy();
		flat.landforms().atolls.enabled = false;
		TerrainViews.View with = TerrainViews.view(preset, 0.0F, 0.0F, 24.0F);
		TerrainViews.View without = TerrainViews.view(flat, 0.0F, 0.0F, 24.0F);
		int water = with.levels().waterLevel;
		int land = 0;
		for (int x = 0; x < with.size(); x++) {
			for (int z = 0; z < with.size(); z++) {
				if (with.blockY(x, z) >= water && without.blockY(x, z) < water - 3) {
					land++;
				}
			}
		}
		return land;
	}

	@Test
	void worldsDontShareLandformCaches() throws Exception {
		// landforms cache what they sample per thread; those samples belong to one world, so no cache may be shared
		// between two worlds' landforms, e.g. through a static field
		Preset preset = preset("default", (p) -> {});
		raccoonman.reterraforged.world.worldgen.heightmap.Levels levels = TerrainViews.levels(preset);
		var first = raccoonman.reterraforged.world.worldgen.landform.Landforms.make(new raccoonman.reterraforged.world.worldgen.util.Seed(1), preset.landforms(), levels, 0.34F);
		var second = raccoonman.reterraforged.world.worldgen.landform.Landforms.make(new raccoonman.reterraforged.world.worldgen.util.Seed(1), preset.landforms(), levels, 0.34F);
		assertTrue(first.landforms().size() >= 5, "expected every landform, got " + first.landforms());
		for (int i = 0; i < first.landforms().size(); i++) {
			Object a = first.landforms().get(i);
			Object b = second.landforms().get(i);
			for (java.lang.reflect.RecordComponent component : a.getClass().getRecordComponents()) {
				Class<?> type = component.getType();
				if (type == ThreadLocal.class || type == raccoonman.reterraforged.world.worldgen.landform.ClimateGrid.class) {
					Object cacheA = component.getAccessor().invoke(a);
					Object cacheB = component.getAccessor().invoke(b);
					assertTrue(cacheA != cacheB, a.getClass().getSimpleName() + "." + component.getName() + " is shared between worlds");
				}
			}
		}
	}

	@Test
	void volcanoesHaveConesAndLavaCraters() {
		// the centres of volcano regions on land, over a wide area
		Preset preset = preset("default", (p) -> {});
		TerrainViews.View survey = TerrainViews.view(preset, 0.0F, 0.0F, 48.0F);
		java.util.Set<Long> centres = new java.util.LinkedHashSet<>();
		for (int x = 0; x < survey.size(); x++) {
			for (int z = 0; z < survey.size(); z++) {
				Cell cell = survey.cell(x, z);
				if (cell.terrain.isVolcano() && survey.blockY(x, z) > survey.levels().waterLevel) {
					centres.add(cell.terrainRegionCenter);
				}
			}
		}
		assertTrue(!centres.isEmpty(), "no volcanoes on land");
		int craters = 0;
		for (long centre : centres) {
			float cx = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackLeft(centre);
			float cz = raccoonman.reterraforged.world.worldgen.util.PosUtil.unpackRight(centre);
			TerrainViews.View close = TerrainViews.view(preset, cx, cz, 1.0F);
			int water = close.levels().waterLevel;
			int highest = Integer.MIN_VALUE;
			int lowest = Integer.MAX_VALUE;
			int lavaMin = Integer.MAX_VALUE;
			int lavaMax = Integer.MIN_VALUE;
			int lava = 0;
			for (int x = 0; x < close.size(); x++) {
				for (int z = 0; z < close.size(); z++) {
					int y = close.blockY(x, z);
					highest = Math.max(highest, y);
					lowest = Math.min(lowest, y);
					if (close.cell(x, z).terrain == TerrainType.VOLCANO_PIPE) {
						lava++;
						lavaMin = Math.min(lavaMin, y);
						lavaMax = Math.max(lavaMax, y);
					}
				}
			}
			if (lowest < water) {
				// by the sea; the cone may stand in it
				continue;
			}
			System.out.println("volcano at " + cx + ", " + cz + ": rises " + (highest - lowest) + " blocks, crater lava " + lava + " cells at y " + lavaMin + " to " + lavaMax);
			assertTrue(highest - lowest > 50, "the volcano at " + cx + ", " + cz + " rises only " + (highest - lowest) + " blocks");
			if (lava > 0) {
				craters++;
				// a lava lake, not lava running down a slope
				assertTrue(lavaMax - lavaMin <= 3, "the crater floor at " + cx + ", " + cz + " spans " + lavaMin + " to " + lavaMax);
			}
		}
		assertTrue(craters > 0, "no volcano has a lava crater");
	}

	@Test
	void legacyPresetsHaveNoLandforms() {
		for (String name : new String[] { "legacy_default", "beautiful", "huge_biomes", "lite", "vanillaish" }) {
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().buttes.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().canyons.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().seaCliffs.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().fjords.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().atolls.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().dunes.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().tors.enabled, name);
			LandformSettings landforms = BuiltinPresetRenderTest.presets().get(name).get().landforms();
			assertTrue(!landforms.saltFlats.enabled && !landforms.alluvialFans.enabled && !landforms.glacialValleys.enabled && !landforms.drumlins.enabled && !landforms.barrierIslands.enabled && !landforms.karst.enabled && !landforms.deltas.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().volcanicSurface, name);
		}
	}
}
