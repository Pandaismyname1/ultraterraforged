package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.heightmap.TerrainLocator;
import raccoonman.reterraforged.world.worldgen.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Peninsulas, headlands and bays, coastal islands and sandbars, skerries, volcanic island arcs and lake and river
 * islands: each is found as /rtf locate would find it and measured, with it on and off. With -Drtf.render=true the
 * places are also drawn to build/terrain-views.
 */
public class CoastsTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	private static Preset preset(String name, Consumer<Preset> change) {
		Preset preset = BuiltinPresetRenderTest.presets().get(name).get();
		change.accept(preset);
		return preset;
	}

	private static TerrainLocator.Found find(Preset preset, Terrain terrain) {
		GeneratorContext context = GeneratorContext.makeCached(preset, PresetRenderer.SEED, 3, 6, false);
		TerrainLocator.Found found = TerrainLocator.locate(context.lookup, terrain, 0, 0, 12000, 60_000L);
		assertNotNull(found, "no " + terrain.getName() + " found");
		System.out.println(terrain.getName() + " at " + found.x() + " " + found.z());
		return found;
	}

	private static void render(TerrainViews.View view, String name) throws IOException {
		if (Boolean.getBoolean("rtf.render")) {
			TerrainViews.write(view, name);
			TerrainViews.writePerspective(view, name + "_3d");
		}
	}

	private static int count(TerrainViews.View view, Terrain terrain) {
		int count = 0;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				if (view.cell(x, z).terrain == terrain) {
					count++;
				}
			}
		}
		return count;
	}

	// how many cells are land in one view and sea in the other
	private static int[] landChange(TerrainViews.View on, TerrainViews.View off) {
		int water = on.levels().waterLevel;
		int gained = 0;
		int lost = 0;
		for (int x = 0; x < on.size(); x++) {
			for (int z = 0; z < on.size(); z++) {
				boolean landOn = on.blockY(x, z) >= water;
				boolean landOff = off.blockY(x, z) >= water;
				if (landOn && !landOff) {
					gained++;
				} else if (!landOn && landOff) {
					lost++;
				}
			}
		}
		return new int[] { gained, lost };
	}

	// the sizes of the pieces of dry land holding the given terrain, and how much of it lies on pieces no bigger than
	// the limit
	private static int[] islands(TerrainViews.View view, Terrain terrain, int limit) {
		int size = view.size();
		int water = view.levels().waterLevel;
		boolean[] seen = new boolean[size * size];
		int total = 0;
		int apart = 0;
		int pieces = 0;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				if (seen[x * size + z] || view.cell(x, z).terrain != terrain || view.blockY(x, z) < water) {
					continue;
				}
				int land = 0;
				int marked = 0;
				ArrayDeque<int[]> queue = new ArrayDeque<>();
				queue.add(new int[] { x, z });
				seen[x * size + z] = true;
				boolean edge = false;
				while (!queue.isEmpty()) {
					int[] at = queue.poll();
					land++;
					if (view.cell(at[0], at[1]).terrain == terrain) {
						marked++;
					}
					for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
						int nx = at[0] + step[0];
						int nz = at[1] + step[1];
						if (nx < 0 || nz < 0 || nx >= size || nz >= size) {
							edge = true;
							continue;
						}
						if (seen[nx * size + nz] || view.blockY(nx, nz) < water) {
							continue;
						}
						seen[nx * size + nz] = true;
						queue.add(new int[] { nx, nz });
					}
				}
				pieces++;
				total += marked;
				if (land <= limit && !edge) {
					apart += marked;
				}
			}
		}
		return new int[] { total, apart, pieces };
	}

	@Test
	void peninsulasReachOutToSea() throws IOException {
		Preset with = preset("default", (p) -> {});
		Preset without = preset("default", (p) -> p.coasts().peninsulas.enabled = false);
		TerrainLocator.Found found = find(with, TerrainType.PENINSULA);
		TerrainViews.View on = TerrainViews.view(with, found.x(), found.z(), 4.0F);
		TerrainViews.View off = TerrainViews.view(without, found.x(), found.z(), 4.0F);
		render(on, "peninsula");
		render(off, "peninsula_off");
		render(TerrainViews.view(with, found.x(), found.z(), 12.0F), "peninsula_wide");
		render(TerrainViews.view(without, found.x(), found.z(), 12.0F), "peninsula_wide_off");
		int[] change = landChange(on, off);
		System.out.printf("peninsulas turn %d cells of sea into land, and %d of land into sea (at 4 blocks a cell)%n", change[0], change[1]);
		assertTrue(change[0] > 1500, "the peninsulas added only " + change[0] + " cells of land");
		assertTrue(change[1] < change[0] / 20, "the peninsulas sank " + change[1] + " cells of land");
	}

	@Test
	void headlandsAndBaysRaggedTheCoast() throws IOException {
		Preset with = preset("default", (p) -> {});
		Preset without = preset("default", (p) -> p.coasts().headlands.enabled = false);
		TerrainViews.View on = TerrainViews.view(with, 0.0F, 0.0F, 8.0F);
		TerrainViews.View off = TerrainViews.view(without, 0.0F, 0.0F, 8.0F);
		render(on, "headlands");
		render(off, "headlands_off");
		int coastOn = coastline(on);
		int coastOff = coastline(off);
		int[] change = landChange(on, off);
		System.out.printf("coastline %d cells long with headlands and bays, %d without; %d cells gained, %d lost%n", coastOn, coastOff, change[0], change[1]);
		assertTrue(coastOn > coastOff * 1.08F, "the coastline is barely more ragged: " + coastOn + " against " + coastOff);
		assertTrue(change[0] > 100 && change[1] > 100, "the coast moved both ways only " + change[0] + " and " + change[1] + " cells");
	}

	private static int coastline(TerrainViews.View view) {
		int water = view.levels().waterLevel;
		int count = 0;
		for (int x = 0; x < view.size() - 1; x++) {
			for (int z = 0; z < view.size() - 1; z++) {
				boolean land = view.blockY(x, z) >= water;
				if (land != (view.blockY(x + 1, z) >= water) || land != (view.blockY(x, z + 1) >= water)) {
					count++;
				}
			}
		}
		return count;
	}

	@Test
	void coastalIslandsStandApart() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.COASTAL_ISLAND);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 2.0F);
		render(view, "coastal_island");
		int[] islands = islands(view, TerrainType.COASTAL_ISLAND, 40000);
		System.out.printf("%d coastal island cells on %d pieces of land, %d of them on islands of their own%n", islands[0], islands[2], islands[1]);
		assertTrue(islands[0] > 500, "only " + islands[0] + " coastal island cells");
		assertTrue(islands[1] > islands[0] / 3, "only " + islands[1] + " of " + islands[0] + " coastal island cells lie apart from the mainland");
	}

	@Test
	void sandBarsLieJustAboveTheSea() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SAND_BAR);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "sand_bar");
		int water = view.levels().waterLevel;
		int bars = 0;
		int low = 0;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				if (view.cell(x, z).terrain == TerrainType.SAND_BAR) {
					bars++;
					if (view.blockY(x, z) >= water - 1 && view.blockY(x, z) <= water + 3) {
						low++;
					}
				}
			}
		}
		System.out.printf("%d sand bar cells, %d of them within a few blocks of the sea%n", bars, low);
		assertTrue(bars > 150, "only " + bars + " sand bar cells");
		assertTrue(low > bars * 0.95F, (bars - low) + " sand bar cells stand high");
	}

	@Test
	void skerriesAreSwarmsOfIslets() throws IOException {
		Preset preset = preset("frozen_north", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SKERRY);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "skerries");
		int[] islets = islands(view, TerrainType.SKERRY, 400);
		System.out.printf("%d skerry cells on %d islets, %d of them on islets no bigger than 400 cells%n", islets[0], islets[2], islets[1]);
		assertTrue(islets[2] >= 10, "only " + islets[2] + " islets");
		assertTrue(islets[1] > islets[0] * 0.8F, "the islets run together: only " + islets[1] + " of " + islets[0] + " cells on small ones");
	}

	@Test
	void volcanicIslandsRiseOutOfTheOpenSea() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.VOLCANIC_ISLAND);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 16.0F);
		TerrainViews.View close = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "island_arc");
		render(close, "volcanic_island");
		int[] islands = islands(view, TerrainType.VOLCANIC_ISLAND, 40000);
		int highest = 0;
		for (int x = 0; x < close.size(); x++) {
			for (int z = 0; z < close.size(); z++) {
				highest = Math.max(highest, close.blockY(x, z) - close.levels().waterLevel);
			}
		}
		System.out.printf("%d volcanic island cells on %d islands in the arc, rising up to %d blocks%n", islands[0], islands[2], highest);
		assertTrue(islands[2] >= 3, "only " + islands[2] + " islands in the arc");
		assertTrue(highest >= 10, "the island rises only " + highest + " blocks");
	}

	@Test
	void lakesAndRiversHaveIslands() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.RIVER_ISLAND);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "river_island");
		int islands = count(view, TerrainType.RIVER_ISLAND);
		int[] apart = islands(view, TerrainType.RIVER_ISLAND, 20000);
		System.out.printf("%d river and lake island cells, %d of them apart from the shore%n", islands, apart[1]);
		assertTrue(islands > 100, "only " + islands + " island cells");
	}
}
