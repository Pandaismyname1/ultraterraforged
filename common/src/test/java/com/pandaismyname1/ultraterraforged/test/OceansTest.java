package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.TerrainLocator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * The sea floor's features: each is found as /rtf locate would find it and measured. With -Drtf.render=true the places
 * are also drawn to build/terrain-views.
 */
public class OceansTest {

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
			TerrainViews.writeProfile(view, name + "_profile");
		}
	}

	// the depths of the cells with the given terrain, and of those around them without it, in blocks under the sea
	private static float[] depths(TerrainViews.View view, Terrain terrain) {
		int water = view.levels().waterLevel;
		long inside = 0;
		long outside = 0;
		int in = 0;
		int out = 0;
		int deepest = 0;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				int depth = water - view.blockY(x, z);
				if (depth <= 0) {
					continue;
				}
				if (view.cell(x, z).terrain == terrain) {
					inside += depth;
					in++;
					deepest = Math.max(deepest, depth);
				} else {
					outside += depth;
					out++;
				}
			}
		}
		return new float[] { in, in == 0 ? 0 : inside / (float) in, out == 0 ? 0 : outside / (float) out, deepest };
	}

	@Test
	void shelvesEndInADropOff() throws IOException {
		Preset with = preset("default", (p) -> {});
		Preset without = preset("default", (p) -> p.oceans().shelves.enabled = false);
		TerrainViews.View on = TerrainViews.view(with, 0.0F, 0.0F, 4.0F);
		TerrainViews.View off = TerrainViews.view(without, 0.0F, 0.0F, 4.0F);
		render(on, "shelves");
		render(off, "shelves_off");
		int water = on.levels().waterLevel;
		int shelfOn = 0;
		int shelfOff = 0;
		int sea = 0;
		int lowered = 0;
		for (int x = 0; x < on.size(); x++) {
			for (int z = 0; z < on.size(); z++) {
				if (off.blockY(x, z) >= water) {
					continue;
				}
				sea++;
				int depthOn = water - on.blockY(x, z);
				int depthOff = water - off.blockY(x, z);
				shelfOn += depthOn > 0 && depthOn <= 15 ? 1 : 0;
				shelfOff += depthOff > 0 && depthOff <= 15 ? 1 : 0;
				if (on.blockY(x, z) < off.blockY(x, z) - 1) {
					lowered++;
				}
			}
		}
		System.out.printf("of %d cells of sea, %d are shallow shelf with shelves, %d without; %d lowered%n", sea, shelfOn, shelfOff, lowered);
		assertTrue(shelfOn > shelfOff * 1.3F, "the shelves barely widen the shallows: " + shelfOn + " against " + shelfOff);
		assertTrue(lowered < sea / 200, lowered + " cells of sea floor lowered");
	}

	@Test
	void submarineCanyonsCutTheShelf() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SUBMARINE_CANYON);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 2.0F);
		render(view, "submarine_canyon");
		float[] depths = depths(view, TerrainType.SUBMARINE_CANYON);
		System.out.printf("%.0f submarine canyon cells, %.1f blocks deep on average against %.1f around them%n", depths[0], depths[1], depths[2]);
		assertTrue(depths[0] > 300, "only " + depths[0] + " canyon cells");
		assertTrue(depths[1] > depths[2] + 5.0F, "the canyons are only " + depths[1] + " deep against " + depths[2]);
	}

	@Test
	void trenchesPlungeBelowTheFloor() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.OCEAN_TRENCH);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 4.0F);
		render(view, "ocean_trench");
		float[] depths = depths(view, TerrainType.OCEAN_TRENCH);
		System.out.printf("%.0f trench cells, %.1f blocks deep on average, down to %.0f, against %.1f around them%n", depths[0], depths[1], depths[3], depths[2]);
		assertTrue(depths[0] > 300, "only " + depths[0] + " trench cells");
		assertTrue(depths[1] > depths[2] + 20.0F, "the trench is only " + depths[1] + " deep against " + depths[2]);
	}

	@Test
	void guyotsHaveFlatTops() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.GUYOT);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 2.0F);
		render(view, "guyot");
		int water = view.levels().waterLevel;
		int top = Integer.MIN_VALUE;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				if (view.cell(x, z).terrain == TerrainType.GUYOT) {
					top = Math.max(top, view.blockY(x, z));
				}
			}
		}
		int flat = 0;
		int cells = 0;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				if (view.cell(x, z).terrain == TerrainType.GUYOT) {
					cells++;
					if (view.blockY(x, z) >= top - 1) {
						flat++;
					}
				}
			}
		}
		System.out.printf("%d guyot cells, %d of them on its flat top %d blocks under the sea%n", cells, flat, water - top);
		assertTrue(cells > 300, "only " + cells + " guyot cells");
		assertTrue(flat > cells / 6, "only " + flat + " of " + cells + " on the flat top");
		assertTrue(top < water - 5, "the guyot rises to " + top);
	}

	@Test
	void seamountsRiseFromTheDeep() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SEAMOUNT);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 2.0F);
		render(view, "seamount");
		float[] depths = depths(view, TerrainType.SEAMOUNT);
		System.out.printf("%.0f seamount cells, %.1f blocks deep on average against %.1f around them%n", depths[0], depths[1], depths[2]);
		assertTrue(depths[0] > 300, "only " + depths[0] + " seamount cells");
		assertTrue(depths[1] < depths[2] - 5.0F, "the seamount rises only to " + depths[1] + " against " + depths[2]);
	}

	@Test
	void ridgesHaveARift() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.OCEAN_RIDGE);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 4.0F);
		render(view, "ocean_ridge");
		float[] depths = depths(view, TerrainType.OCEAN_RIDGE);
		System.out.printf("%.0f rift cells, %.1f blocks deep on average against %.1f around them%n", depths[0], depths[1], depths[2]);
		assertTrue(depths[0] > 100, "only " + depths[0] + " rift cells");
	}

	@Test
	void blueHolesAreDeepAndRound() throws IOException {
		Preset preset = preset("tropics", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.BLUE_HOLE);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "blue_hole");
		float[] depths = depths(view, TerrainType.BLUE_HOLE);
		System.out.printf("%.0f blue hole cells, %.1f blocks deep on average against %.1f around them%n", depths[0], depths[1], depths[2]);
		assertTrue(depths[0] > 200, "only " + depths[0] + " blue hole cells");
		assertTrue(depths[1] > depths[2] + 20.0F, "the blue hole is only " + depths[1] + " deep against " + depths[2]);
	}

	@Test
	void coralReefsReachNearlyToTheSurface() throws IOException {
		Preset preset = preset("tropics", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.CORAL_REEF);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "coral_reef");
		int water = view.levels().waterLevel;
		int reef = 0;
		int shallow = 0;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				if (view.cell(x, z).terrain == TerrainType.CORAL_REEF) {
					reef++;
					int depth = water - view.blockY(x, z);
					if (depth >= 1 && depth <= 4) {
						shallow++;
					}
				}
			}
		}
		System.out.printf("%d coral reef cells, %d of them 1 to 4 blocks under the surface%n", reef, shallow);
		assertTrue(reef > 300, "only " + reef + " reef cells");
		assertTrue(shallow > reef * 0.6F, "only " + shallow + " of " + reef + " reef cells near the surface");
	}

	@Test
	void sandWavesRipple() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SAND_WAVES);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "sand_waves");
		// along the rows, how often the floor changes direction
		int turns = 0;
		int cells = 0;
		for (int z = 0; z < view.size(); z++) {
			int last = 0;
			for (int x = 1; x < view.size(); x++) {
				if (view.cell(x, z).terrain != TerrainType.SAND_WAVES) {
					continue;
				}
				cells++;
				int step = Integer.signum(view.blockY(x, z) - view.blockY(x - 1, z));
				if (step != 0) {
					if (last != 0 && step != last) {
						turns++;
					}
					last = step;
				}
			}
		}
		System.out.printf("%d sand wave cells, turning %d times%n", cells, turns);
		assertTrue(cells > 1000, "only " + cells + " sand wave cells");
		assertTrue(turns > cells / 60, "the sand barely ripples: " + turns + " turns over " + cells + " cells");
	}
}
