package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Each landform is measured by generating the same place with it on and off.
 */
public class LandformTest {
	// a desert and badlands area of the Badlands preset, with several buttes
	private static final float DESERT_X = -304.0F;
	private static final float DESERT_Z = -128.0F;

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
	void legacyPresetsHaveNoLandforms() {
		for (String name : new String[] { "legacy_default", "beautiful", "huge_biomes", "lite", "vanillaish" }) {
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().buttes.enabled, name);
			assertTrue(!BuiltinPresetRenderTest.presets().get(name).get().landforms().canyons.enabled, name);
		}
	}
}
