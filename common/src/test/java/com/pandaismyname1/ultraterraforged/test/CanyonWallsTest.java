package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;

/**
 * Canyons' walls are no steeper than a few blocks per block: narrow canyons cut to the full depth had walls that came
 * out as columns of very different heights (#10). Measured on a stretch of plateau with canyons, against the same
 * world without them; before the fix 72% of the columns canyons cut stepped 5 or more blocks to a neighbour.
 */
public class CanyonWallsTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void wallsAreNotColumns() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		Preset off = preset.copy();
		off.landforms().canyons.enabled = false;
		int seed = (int) -2567989122628637348L;
		GeneratorContext canyons = GeneratorContext.makeCached(preset, seed, 3, 6, false);
		GeneratorContext none = GeneratorContext.makeCached(off, seed, 3, 6, false);
		int x0 = 1400, z0 = 4500, n = 440;
		int[][] height = new int[n][n];
		boolean[][] cut = new boolean[n][n];
		Cell cell = new Cell();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				canyons.lookup.apply(cell.reset(), x0 + i, z0 + j, false);
				height[i][j] = canyons.levels.scale(cell.height);
				none.lookup.apply(cell.reset(), x0 + i, z0 + j, false);
				cut[i][j] = none.levels.scale(cell.height) > height[i][j];
			}
		}
		int columns = 0, steps = 0;
		for (int i = 1; i < n - 1; i++) {
			for (int j = 1; j < n - 1; j++) {
				if (!cut[i][j]) {
					continue;
				}
				columns++;
				int h = height[i][j];
				int step = Math.max(Math.max(Math.abs(h - height[i - 1][j]), Math.abs(h - height[i + 1][j])), Math.max(Math.abs(h - height[i][j - 1]), Math.abs(h - height[i][j + 1])));
				if (step >= 5) {
					steps++;
				}
			}
		}
		System.out.printf("%d columns cut by canyons, %d (%.1f%%) step 5 or more blocks to a neighbour%n", columns, steps, 100.0F * steps / columns);
		assertTrue(columns > 5000, "no canyons here any more: " + columns);
		assertTrue(steps < columns * 0.3F, "steep columns: " + steps + " of " + columns);
		// the gully of #10's kind seen in game: 26 blocks deep and 6 wide
		for (int z = 4915; z <= 4928; z++) {
			int step = Math.abs(height[1812 - x0][z - z0] - height[1812 - x0][z - 1 - z0]);
			assertTrue(step <= 5, "a step of " + step + " at 1812 " + z);
		}
	}
}
