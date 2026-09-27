package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.TerrainLocator;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * /rtf locate should lead to the terrain asked for, as the world generates it.
 */
public class LocateTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void locateFindsTheTerrainTheWorldHas() {
		Preset preset = BuiltinPresetRenderTest.presets().get("default").get();
		GeneratorContext context = GeneratorContext.makeCached(preset, PresetRenderer.SEED, 3, 6, false);
		int found = 0;
		int wrong = 0;
		int buried = 0;
		long slowest = 0;
		StringBuilder report = new StringBuilder();
		for (Terrain terrain : TerrainType.REGISTRY) {
			// blends of two terrain types are rarely looked for
			if (terrain == TerrainType.NONE || terrain.getName().contains("-")) {
				continue;
			}
			for (int[] origin : new int[][] { { 0, 0 }, { 5000, -3000 } }) {
				long start = System.currentTimeMillis();
				TerrainLocator.Found result = TerrainLocator.locate(context.lookup, terrain, origin[0], origin[1], 8000, 20_000L);
				slowest = Math.max(slowest, System.currentTimeMillis() - start);
				if (result == null) {
					report.append(terrain.getName()).append(" not found; ");
					continue;
				}
				found++;
				Tile tile = context.cache.provide(context.cache.chunkToTile(result.x() >> 4), context.cache.chunkToTile(result.z() >> 4));
				// the terrain itself, or right beside it: filters can move an edge by a block or two
				boolean near = false;
				for (int dx = -2; dx <= 2 && !near; dx++) {
					for (int dz = -2; dz <= 2 && !near; dz++) {
						Cell cell = tile.lookup(result.x() + dx, result.z() + dz);
						near = terrain.equals(cell.terrain);
					}
				}
				// standing on the ground or water, not inside a hill or high above it
				int y = TerrainLocator.surface(context.lookup, context.levels, result.x(), result.z());
				Cell there = tile.lookup(result.x(), result.z());
				int ground = Math.max(context.levels.scale(there.height), context.levels.waterY);
				if (there.waterLevel > 0.0F) {
					ground = Math.max(ground, context.levels.scale(there.waterLevel));
				}
				if (y != ground + 1) {
					buried++;
					report.append(terrain.getName()).append(" y ").append(y).append(" over ").append(ground).append("; ");
				}
				if (!near) {
					wrong++;
					report.append(terrain.getName()).append(" at ").append(result.x()).append(' ').append(result.z()).append(" is ").append(tile.lookup(result.x(), result.z()).terrain.getName()).append("; ");
				}
			}
		}
		System.out.println(found + " found, " + wrong + " wrong, " + buried + " at the wrong height, slowest search " + slowest + " ms: " + report);
		assertTrue(buried == 0, buried + " places at the wrong height: " + report);
		assertTrue(found > 20, "found only " + found);
		assertTrue(wrong <= found / 20, wrong + " of " + found + " searches led somewhere else: " + report);
	}
}
