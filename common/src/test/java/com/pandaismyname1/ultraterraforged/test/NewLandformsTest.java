package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.TerrainLocator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Salt flats, alluvial fans, glacial valleys and cirques, moraines and drumlins, barrier islands, karst and deltas:
 * each is found as /rtf locate would find it, then the place is generated with it on and off and measured.
 * With -Drtf.render=true the places are also drawn to build/terrain-views.
 */
public class NewLandformsTest {

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

	@Test
	void saltFlatsAreDeadFlat() throws IOException {
		Preset preset = preset("badlands", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SALT_FLAT);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "salt_flat");
		render(TerrainViews.view(preset, found.x(), found.z(), 2.0F), "salt_flat_wide");
		int cells = 0;
		int pairs = 0;
		int level = 0;
		for (int x = 0; x < view.size() - 1; x++) {
			for (int z = 0; z < view.size() - 1; z++) {
				if (view.cell(x, z).terrain != TerrainType.SALT_FLAT) {
					continue;
				}
				cells++;
				for (int[] step : new int[][] { { 1, 0 }, { 0, 1 } }) {
					if (view.cell(x + step[0], z + step[1]).terrain == TerrainType.SALT_FLAT) {
						pairs++;
						if (view.blockY(x + step[0], z + step[1]) == view.blockY(x, z)) {
							level++;
						}
					}
				}
			}
		}
		System.out.printf("%d salt flat cells, %.1f%% of neighbouring pairs level%n", cells, level * 100.0F / pairs);
		assertTrue(cells > 2000, "only " + cells + " salt flat cells");
		assertTrue(level > pairs * 0.99F, (pairs - level) + " of " + pairs + " neighbouring salt flat cells differ in height");
	}

	@Test
	void alluvialFansOnlyAddGround() throws IOException {
		Preset with = preset("default", (p) -> {});
		Preset without = preset("default", (p) -> p.landforms().alluvialFans.enabled = false);
		TerrainLocator.Found found = find(with, TerrainType.ALLUVIAL_FAN);
		TerrainViews.View on = TerrainViews.view(with, found.x(), found.z(), 1.0F);
		TerrainViews.View off = TerrainViews.view(without, found.x(), found.z(), 1.0F);
		render(on, "alluvial_fan");
		render(TerrainViews.view(with, found.x(), found.z(), 2.0F), "alluvial_fan_wide");
		render(TerrainViews.view(without, found.x(), found.z(), 2.0F), "alluvial_fan_wide_off");
		int fan = 0;
		int lowered = 0;
		int maxRise = 0;
		for (int x = 0; x < on.size(); x++) {
			for (int z = 0; z < on.size(); z++) {
				int rise = on.blockY(x, z) - off.blockY(x, z);
				maxRise = Math.max(maxRise, rise);
				if (rise < -1) {
					lowered++;
				}
				if (on.cell(x, z).terrain == TerrainType.ALLUVIAL_FAN) {
					fan++;
				}
			}
		}
		System.out.printf("%d fan cells, rising up to %d blocks, %d lowered%n", fan, maxRise, lowered);
		assertTrue(fan > 300, "only " + fan + " fan cells");
		assertTrue(maxRise >= 5, "the fans rise only " + maxRise + " blocks");
		assertTrue(lowered < on.size() * on.size() / 1000, lowered + " cells lowered");
	}

	@Test
	void glacialValleysHaveFlatFloors() throws IOException {
		Preset with = preset("frozen_north", (p) -> {});
		Preset without = preset("frozen_north", (p) -> p.landforms().glacialValleys.enabled = false);
		TerrainLocator.Found found = find(with, TerrainType.GLACIAL_VALLEY);
		TerrainViews.View on = TerrainViews.view(with, found.x(), found.z(), 1.0F);
		TerrainViews.View off = TerrainViews.view(without, found.x(), found.z(), 1.0F);
		render(on, "glacial_valley");
		render(off, "glacial_valley_off");
		int floor = count(on, TerrainType.GLACIAL_VALLEY);
		// over the ground the glaciers wore down, how much of it is flat, and how much sheer, before and after
		int worn = 0;
		int flatOn = 0;
		int flatOff = 0;
		int steepOn = 0;
		int steepOff = 0;
		int raised = 0;
		int drowned = 0;
		int water = on.levels().waterLevel;
		for (int x = 1; x < on.size() - 1; x++) {
			for (int z = 1; z < on.size() - 1; z++) {
				if (on.blockY(x, z) > off.blockY(x, z) + 1) {
					raised++;
				}
				if (on.blockY(x, z) < water && off.blockY(x, z) >= water) {
					drowned++;
				}
				if (on.blockY(x, z) > off.blockY(x, z) - 3) {
					continue;
				}
				worn++;
				flatOn += slope(on, x, z) < 0.3F ? 1 : 0;
				flatOff += slope(off, x, z) < 0.3F ? 1 : 0;
				steepOn += slope(on, x, z) > 1.0F ? 1 : 0;
				steepOff += slope(off, x, z) > 1.0F ? 1 : 0;
			}
		}
		System.out.printf("%d valley floor cells; of %d worn down, %d flat and %d steep, against %d and %d before; %d raised, %d drowned%n", floor, worn, flatOn, steepOn, flatOff, steepOff, raised, drowned);
		assertTrue(floor > 1000, "only " + floor + " valley floor cells");
		assertTrue(worn > 5000, "only " + worn + " cells worn down");
		assertTrue(flatOn > flatOff * 1.3F && steepOn > steepOff * 1.3F, "the valleys aren't U-shaped: " + flatOn + " flat and " + steepOn + " steep cells against " + flatOff + " and " + steepOff);
		assertTrue(raised < on.size() * on.size() / 1000, raised + " cells raised");
		assertTrue(drowned < on.size() * on.size() / 1000, drowned + " cells sunk under the sea");
	}

	private static float slope(TerrainViews.View view, int x, int z) {
		float dx = (view.exactY(x + 1, z) - view.exactY(x - 1, z)) * 0.5F;
		float dz = (view.exactY(x, z + 1) - view.exactY(x, z - 1)) * 0.5F;
		return (float) Math.sqrt(dx * dx + dz * dz);
	}

	@Test
	void cirquesHoldLakes() throws IOException {
		Preset preset = preset("frozen_north", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.CIRQUE);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "cirque");
		int bowl = 0;
		int lake = 0;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				if (view.cell(x, z).terrain == TerrainType.CIRQUE) {
					bowl++;
					if (view.raisedWater(x, z)) {
						lake++;
					}
				}
			}
		}
		System.out.printf("%d cirque cells, %d under its lake%n", bowl, lake);
		assertTrue(bowl > 400, "only " + bowl + " cirque cells");
		assertTrue(lake > 50, "only " + lake + " cells of lake");
	}

	@Test
	void drumlinsAndMorainesOnlyAddGround() throws IOException {
		Preset with = preset("frozen_north", (p) -> {});
		Preset without = preset("frozen_north", (p) -> p.landforms().drumlins.enabled = false);
		TerrainLocator.Found found = find(with, TerrainType.DRUMLINS);
		TerrainViews.View on = TerrainViews.view(with, found.x(), found.z(), 1.0F);
		TerrainViews.View off = TerrainViews.view(without, found.x(), found.z(), 1.0F);
		render(on, "drumlins");
		render(TerrainViews.view(with, found.x(), found.z(), 2.0F), "drumlins_wide");
		int drumlins = count(on, TerrainType.DRUMLINS);
		int moraines = count(on, TerrainType.MORAINE);
		int lowered = 0;
		int maxRise = 0;
		for (int x = 0; x < on.size(); x++) {
			for (int z = 0; z < on.size(); z++) {
				int rise = on.blockY(x, z) - off.blockY(x, z);
				maxRise = Math.max(maxRise, rise);
				if (rise < -1) {
					lowered++;
				}
			}
		}
		System.out.printf("%d drumlin cells, %d moraine cells, rising up to %d blocks, %d lowered%n", drumlins, moraines, maxRise, lowered);
		assertTrue(drumlins > 1000, "only " + drumlins + " drumlin cells");
		assertTrue(maxRise >= 4 && maxRise <= with.landforms().drumlins.height + 2, "the drumlins rise " + maxRise + " blocks");
		assertTrue(lowered < on.size() * on.size() / 1000, lowered + " cells lowered");
	}

	@Test
	void barrierIslandsStandOffTheCoast() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.BARRIER_ISLAND);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "barrier_island");
		int size = view.size();
		int water = view.levels().waterLevel;
		boolean[] seen = new boolean[size * size];
		int islands = 0;
		int separate = 0;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				if (seen[x * size + z] || view.cell(x, z).terrain != TerrainType.BARRIER_ISLAND || view.blockY(x, z) < water) {
					continue;
				}
				// the dry land joined to this island: small unless it reaches the mainland
				int land = 0;
				int island = 0;
				ArrayDeque<int[]> queue = new ArrayDeque<>();
				queue.add(new int[] { x, z });
				seen[x * size + z] = true;
				while (!queue.isEmpty()) {
					int[] at = queue.poll();
					land++;
					if (view.cell(at[0], at[1]).terrain == TerrainType.BARRIER_ISLAND) {
						island++;
					}
					for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
						int nx = at[0] + step[0];
						int nz = at[1] + step[1];
						if (nx < 0 || nz < 0 || nx >= size || nz >= size || seen[nx * size + nz] || view.blockY(nx, nz) < water) {
							continue;
						}
						seen[nx * size + nz] = true;
						queue.add(new int[] { nx, nz });
					}
				}
				islands += island;
				if (land < island * 2) {
					separate += island;
				}
			}
		}
		System.out.printf("%d barrier island cells, %d of them on islands of their own%n", islands, separate);
		assertTrue(islands > 300, "only " + islands + " barrier island cells");
		assertTrue(separate > islands / 2, "only " + separate + " of " + islands + " barrier island cells lie apart from the mainland");
	}

	@Test
	void karstTowersRiseSheer() throws IOException {
		Preset with = preset("tropics", (p) -> {});
		Preset without = preset("tropics", (p) -> p.landforms().karst.enabled = false);
		TerrainLocator.Found found = find(with, TerrainType.KARST);
		TerrainViews.View on = TerrainViews.view(with, found.x(), found.z(), 1.0F);
		TerrainViews.View off = TerrainViews.view(without, found.x(), found.z(), 1.0F);
		render(on, "karst");
		int towers = count(on, TerrainType.KARST);
		int tall = 0;
		int steep = 0;
		int sides = 0;
		for (int x = 1; x < on.size() - 1; x++) {
			for (int z = 1; z < on.size() - 1; z++) {
				if (on.blockY(x, z) - off.blockY(x, z) > 20) {
					tall++;
				}
				if (on.cell(x, z).terrain == TerrainType.KARST && on.blockY(x, z) - off.blockY(x, z) > 3 && on.blockY(x, z) - off.blockY(x, z) < 15) {
					sides++;
					if (slope(on, x, z) > 2.0F) {
						steep++;
					}
				}
			}
		}
		System.out.printf("%d karst cells, %d over 20 blocks high; %d of %d low side cells steeper than 2 blocks per block%n", towers, tall, steep, sides);
		assertTrue(towers > 2000, "only " + towers + " karst cells");
		assertTrue(tall > 200, "only " + tall + " cells of tall towers");
		assertTrue(steep > sides / 2, "the towers' sides aren't sheer: " + steep + " of " + sides);
	}

	@Test
	void cenotesHoldWaterInTheirWalls() throws IOException {
		Preset preset = preset("tropics", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.SINKHOLE);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "sinkhole");
		int holes = count(view, TerrainType.SINKHOLE);
		int wet = 0;
		int leaks = 0;
		for (int x = 1; x < view.size() - 1; x++) {
			for (int z = 1; z < view.size() - 1; z++) {
				if (view.cell(x, z).terrain == TerrainType.SINKHOLE && (view.raisedWater(x, z) || view.blockY(x, z) < view.levels().waterLevel)) {
					wet++;
				}
				if (!view.raisedWater(x, z)) {
					continue;
				}
				int top = view.waterY(x, z);
				for (int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
					int nx = x + step[0];
					int nz = z + step[1];
					boolean water = view.raisedWater(nx, nz) || view.blockY(nx, nz) < view.levels().waterLevel;
					if (!water && view.blockY(nx, nz) < top && view.cell(nx, nz).terrain != TerrainType.CIRQUE) {
						leaks++;
						break;
					}
				}
			}
		}
		System.out.printf("%d sinkhole cells, %d under a cenote's water, %d leaks%n", holes, wet, leaks);
		assertTrue(holes > 100, "only " + holes + " sinkhole cells");
		assertTrue(wet > 20, "only " + wet + " cells of cenote water");
		assertTrue(leaks == 0, leaks + " cells of raised water beside lower dry ground");
	}

	@Test
	void deltasSplitIntoChannelsAndIslands() throws IOException {
		Preset preset = preset("default", (p) -> {});
		TerrainLocator.Found found = find(preset, TerrainType.DELTA);
		TerrainViews.View view = TerrainViews.view(preset, found.x(), found.z(), 1.0F);
		render(view, "delta");
		render(TerrainViews.view(preset, found.x(), found.z(), 2.0F), "delta_wide");
		int land = 0;
		int channels = 0;
		int water = view.levels().waterLevel;
		for (int x = 0; x < view.size(); x++) {
			for (int z = 0; z < view.size(); z++) {
				Cell cell = view.cell(x, z);
				if (cell.terrain != TerrainType.DELTA) {
					continue;
				}
				if (view.blockY(x, z) >= water) {
					land++;
				} else {
					channels++;
				}
			}
		}
		System.out.printf("%d delta cells of land, %d of water%n", land, channels);
		assertTrue(land > 1000, "only " + land + " cells of delta land");
		assertTrue(channels > 300, "only " + channels + " cells of delta channels");
	}
}
