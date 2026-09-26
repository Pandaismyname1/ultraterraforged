package raccoonman.reterraforged.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * Renders relief maps of typical places into common/build/terrain-views, to look at terrain changes. Only runs when
 * asked: ./gradlew :common:test --tests '*TerrainViewTest*' -Drtf.render=true
 */
public class TerrainViewTest {

	@BeforeAll
	static void onlyWhenAsked() {
		assumeTrue(Boolean.getBoolean("rtf.render"), "set -Drtf.render=true to render terrain views");
		TestBootstrap.init();
	}

	private static Preset preset(String name) {
		return BuiltinPresetRenderTest.presets().get(name).get();
	}

	@Test
	void overview() throws Exception {
		Preset preset = preset("default");
		TerrainViews.write(TerrainViews.view(preset, 0.0F, 0.0F, 16.0F), "overview");
	}

	@Test
	void riverThroughHighGround() throws Exception {
		Preset preset = preset("default");
		Levels levels = TerrainViews.levels(preset);
		// a river cut into ground well above the sea
		TerrainViews.View survey = TerrainViews.view(preset, 0.0F, 0.0F, 16.0F);
		int[] histogram = new int[8];
		int riverCells = 0;
		for (int i = 0; i < survey.size(); i++) {
			for (int j = 0; j < survey.size(); j++) {
				if (survey.cell(i, j).riverDistance < 0.35F) {
					riverCells++;
					histogram[Math.min(7, Math.max(0, (survey.blockY(i, j) - levels.waterLevel) / 20))]++;
				}
			}
		}
		System.out.println("cells near rivers: " + riverCells + ", by height above sea in 20s: " + java.util.Arrays.toString(histogram));
		long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.riverDistance < 0.35F && levels.scale(cell.height) > levels.waterLevel + 30);
		if (place == Long.MIN_VALUE) {
			System.out.println("no river through high ground found");
			return;
		}
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("river through high ground at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
		TerrainViews.write(close, "river_high_close");
		TerrainViews.writeProfile(close, "river_high_profile");
		TerrainViews.write(TerrainViews.view(preset, x, z, 4.0F), "river_high_wide");
	}

	@Test
	void buttes() throws Exception {
		Preset preset = preset("badlands");
		long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.terrain == raccoonman.reterraforged.world.worldgen.terrain.TerrainType.BADLANDS && cell.terrainRegionEdge > 0.95F && cell.mountainChainAlpha < 0.3F);
		if (place == Long.MIN_VALUE) {
			System.out.println("no badlands found");
			return;
		}
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("badlands at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
		Preset flat = preset.copy();
		flat.landforms().buttes.enabled = false;
		TerrainViews.View without = TerrainViews.view(flat, x, z, 1.0F);
		int raised = 0;
		int most = 0;
		for (int i = 0; i < close.size(); i++) {
			for (int j = 0; j < close.size(); j++) {
				int rise = close.blockY(i, j) - without.blockY(i, j);
				if (rise > 2) {
					raised++;
				}
				most = Math.max(most, rise);
			}
		}
		System.out.println("buttes raise " + raised + " of " + close.size() * close.size() + " cells, at most " + most + " blocks");
		TerrainViews.write(close, "buttes_close");
		TerrainViews.writeProfile(close, "buttes_profile");
		TerrainViews.write(TerrainViews.view(preset, x, z, 3.0F), "buttes_wide");
	}

	@Test
	void buttesInDesert() throws Exception {
		Preset preset = preset("badlands");
		long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.biomeType == raccoonman.reterraforged.world.worldgen.biome.type.BiomeType.DESERT && cell.terrain.isFlat() && cell.riverDistance > 0.9F);
		if (place == Long.MIN_VALUE) {
			System.out.println("no desert found");
			return;
		}
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("desert at " + x + ", " + z);
		TerrainViews.write(TerrainViews.view(preset, x, z, 1.0F), "desert_buttes_close");
		TerrainViews.write(TerrainViews.view(preset, x, z, 3.0F), "desert_buttes_wide");
	}
}
