package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/**
 * Renders relief maps of typical places into common/build/terrain-views, to look at terrain changes. Only runs when
 * asked: ./gradlew :common:test --tests '*TerrainViewTest*' -Dutf.render=true
 */
public class TerrainViewTest {

	@BeforeAll
	static void onlyWhenAsked() {
		assumeTrue(Boolean.getBoolean("utf.render"), "set -Dutf.render=true to render terrain views");
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
		long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.terrain == com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType.BADLANDS && cell.terrainRegionEdge > 0.95F && cell.mountainChainAlpha < 0.3F);
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
		TerrainViews.writePerspective(close, "buttes_3d");
		TerrainViews.writeProfile(close, "buttes_profile");
		TerrainViews.write(TerrainViews.view(preset, x, z, 3.0F), "buttes_wide");
	}

	@Test
	void buttesInDesert() throws Exception {
		Preset preset = preset("badlands");
		long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.biomeType == com.pandaismyname1.ultraterraforged.world.worldgen.biome.type.BiomeType.DESERT && cell.terrain.isFlat() && cell.riverDistance > 0.9F);
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

	@Test
	void canyon() throws Exception {
		for (String name : new String[] { "badlands", "highlands", "default" }) {
			Preset preset = preset(name);
			Levels levels = TerrainViews.levels(preset);
			// the high ground right beside a river: the rim of a gorge
			long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.riverDistance < 0.12F && levels.scale(cell.height) > levels.waterLevel + 45);
			if (place == Long.MIN_VALUE) {
				System.out.println("no river through plateau or badlands in " + name);
				continue;
			}
			float x = PosUtil.unpackLeft(place);
			float z = PosUtil.unpackRight(place);
			System.out.println("canyon candidate in " + name + " at " + x + ", " + z);
			TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
			TerrainViews.write(close, "canyon_" + name + "_close");
			TerrainViews.writeProfile(close, "canyon_" + name + "_profile");
			TerrainViews.write(TerrainViews.view(preset, x, z, 4.0F), "canyon_" + name + "_wide");
			return;
		}
	}

	@Test
	void plateauCanyons() throws Exception {
		Preset preset = preset("badlands");
		long place = TerrainViews.find(preset, 16.0F, (cell) -> (cell.terrain.includes(com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType.PLATEAU) || cell.terrain.includes(com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType.BADLANDS)) && cell.terrainRegionEdge > 0.9F);
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("plateau at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
		TerrainViews.write(close, "canyons_close");
		TerrainViews.writeProfile(close, "canyons_profile");
		TerrainViews.write(TerrainViews.view(preset, x, z, 3.0F), "canyons_wide");
	}

	@Test
	void highPlateauCanyons() throws Exception {
		Preset preset = preset("highlands");
		Levels levels = TerrainViews.levels(preset);
		long place = TerrainViews.find(preset, 16.0F, (cell) -> cell.terrain.includes(com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType.PLATEAU) && cell.terrainRegionEdge > 0.5F && levels.scale(cell.height) > levels.waterLevel + 30);
		if (place == Long.MIN_VALUE) {
			System.out.println("no high plateau");
			return;
		}
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("high plateau at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
		TerrainViews.write(close, "high_canyons_close");
		TerrainViews.writePerspective(close, "high_canyons_3d");
		TerrainViews.writeProfile(close, "high_canyons_profile");
		TerrainViews.write(TerrainViews.view(preset, x, z, 3.0F), "high_canyons_wide");
	}

	@Test
	void seaCliffs() throws Exception {
		Preset preset = preset("default");
		// only the cliffs, to see them on their own
		preset.landforms().canyons.enabled = false;
		preset.landforms().buttes.enabled = false;
		Preset without = preset.copy();
		without.landforms().seaCliffs.enabled = false;
		// the coast west of spawn, found by where the cliffs change it most
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 8.0F), TerrainViews.view(without, 0.0F, 0.0F, 8.0F));
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("sea cliffs at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
		TerrainViews.write(close, "sea_cliffs_close");
		TerrainViews.writeProfile(close, "sea_cliffs_profile_x");
		TerrainViews.writeProfileZ(close, "sea_cliffs_profile_z");
		TerrainViews.write(TerrainViews.view(without, x, z, 1.0F), "sea_cliffs_close_without");
		TerrainViews.View coast = TerrainViews.view(preset, x, z, 2.0F);
		for (int turns = 0; turns < 4; turns++) {
			TerrainViews.writePerspective(coast, "sea_cliffs_3d_" + turns, turns);
		}
		TerrainViews.write(TerrainViews.view(preset, x, z, 4.0F), "sea_cliffs_wide");
	}

	@Test
	void fjords() throws Exception {
		Preset preset = preset("frozen_north");
		Preset without = preset.copy();
		without.landforms().fjords.enabled = false;
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 12.0F), TerrainViews.view(without, 0.0F, 0.0F, 12.0F));
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("fjord at " + x + ", " + z);
		TerrainViews.View wide = TerrainViews.view(preset, x, z, 3.0F);
		TerrainViews.write(wide, "fjords_wide");
		TerrainViews.write(TerrainViews.view(without, x, z, 3.0F), "fjords_wide_without");
		TerrainViews.View close = TerrainViews.view(preset, x, z, 2.0F);
		for (int turns = 0; turns < 4; turns++) {
			TerrainViews.writePerspective(close, "fjords_3d_" + turns, turns);
		}
	}

	@Test
	void gravelBeaches() throws Exception {
		Preset preset = preset("default");
		preset.landforms().seaCliffs.gravelBeaches = 1.0F;
		Preset without = preset.copy();
		without.landforms().seaCliffs.enabled = false;
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 8.0F), TerrainViews.view(without, 0.0F, 0.0F, 8.0F));
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("gravel beach at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
		TerrainViews.write(close, "gravel_beach_close");
		for (int turns = 0; turns < 4; turns++) {
			TerrainViews.writePerspective(close, "gravel_beach_3d_" + turns, turns);
		}
	}

	@Test
	void tors() throws Exception {
		Preset preset = preset("default");
		long place = TerrainViews.find(preset, 8.0F, (cell) -> cell.terrain == com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType.TOR);
		if (place == Long.MIN_VALUE) {
			System.out.println("no tor found");
			return;
		}
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("tor at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 0.25F);
		TerrainViews.write(close, "tor_close");
		for (int turns = 0; turns < 4; turns++) {
			TerrainViews.writePerspective(close, "tor_3d_" + turns, turns);
		}
	}

	@Test
	void dunes() throws Exception {
		for (String name : new String[] { "default", "badlands" }) {
			Preset preset = preset(name);
			Preset without = preset.copy();
			without.landforms().dunes.enabled = false;
			long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 16.0F), TerrainViews.view(without, 0.0F, 0.0F, 16.0F));
			float x = PosUtil.unpackLeft(place);
			float z = PosUtil.unpackRight(place);
			System.out.println(name + ": dunes at " + x + ", " + z);
			TerrainViews.write(TerrainViews.view(preset, x, z, 4.0F), "dunes_" + name + "_wide");
			TerrainViews.View close = TerrainViews.view(preset, x, z, 1.0F);
			TerrainViews.write(close, "dunes_" + name + "_close");
			TerrainViews.writeProfile(close, "dunes_" + name + "_profile");
			TerrainViews.writePerspective(close, "dunes_" + name + "_3d");
		}
	}

	@Test
	void atolls() throws Exception {
		Preset preset = preset("tropics");
		Preset without = preset.copy();
		without.landforms().atolls.enabled = false;
		long place = TerrainViews.mostChanged(TerrainViews.view(preset, 0.0F, 0.0F, 24.0F), TerrainViews.view(without, 0.0F, 0.0F, 24.0F));
		float x = PosUtil.unpackLeft(place);
		float z = PosUtil.unpackRight(place);
		System.out.println("atoll at " + x + ", " + z);
		TerrainViews.View close = TerrainViews.view(preset, x, z, 1.5F);
		TerrainViews.write(close, "atoll_close");
		TerrainViews.writePerspective(close, "atoll_3d");
	}
}
