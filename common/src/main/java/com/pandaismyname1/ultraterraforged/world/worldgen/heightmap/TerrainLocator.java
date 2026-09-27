package com.pandaismyname1.ultraterraforged.world.worldgen.heightmap;

import org.jetbrains.annotations.Nullable;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Finds the nearest place with a given terrain, searching outward in a square spiral, and the height to stand at there.
 */
public final class TerrainLocator {
	private static final int[][] RING = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
	// how many of the 8 points around a place must have the terrain too for it to count as well inside it
	private static final int INSIDE = 7;

	/**
	 * Searches as finely, and insists on being as far inside the terrain, as its size calls for.
	 *
	 * @param maxDistance how far to search, in blocks
	 */
	@Nullable
	public static Found locate(WorldLookup lookup, Terrain target, int originX, int originZ, int maxDistance, long timeoutMillis) {
		int step = step(target);
		return locate(lookup, target, originX, originZ, step, 0, maxDistance / step, coreRadius(target), timeoutMillis);
	}

	// blocks between the places tested: small landforms slip between the samples of a coarse search
	private static int step(Terrain terrain) {
		if (isSmall(terrain)) {
			return 8;
		}
		if (isNarrow(terrain) || isLandform(terrain)) {
			return 32;
		}
		return 128;
	}

	// how far around a place the terrain must reach too, so the search leads inside it rather than to its edge
	private static int coreRadius(Terrain terrain) {
		if (terrain == TerrainType.SHINGLE_BEACH || terrain == TerrainType.MORAINE) {
			return 0;
		}
		if (isSmall(terrain) || isNarrow(terrain)) {
			return 2;
		}
		if (terrain == TerrainType.KARST || terrain == TerrainType.DRUMLINS || terrain == TerrainType.CIRQUE) {
			return 4;
		}
		if (isLandform(terrain)) {
			return 12;
		}
		return 48;
	}

	private static boolean isSmall(Terrain terrain) {
		return terrain == TerrainType.TOR || terrain == TerrainType.SHINGLE_BEACH || terrain == TerrainType.VOLCANO_PIPE || terrain == TerrainType.SINKHOLE || terrain == TerrainType.SKERRY;
	}

	// strips along rivers and shores
	private static boolean isNarrow(Terrain terrain) {
		return terrain.isRiver() || terrain == TerrainType.RIVER_BANKS || terrain == TerrainType.BEACH || terrain == TerrainType.COAST || terrain == TerrainType.BARRIER_ISLAND || terrain == TerrainType.MORAINE || terrain == TerrainType.SAND_BAR || terrain == TerrainType.RIVER_ISLAND || terrain == TerrainType.SUBMARINE_CANYON || terrain == TerrainType.OCEAN_TRENCH || terrain == TerrainType.OCEAN_RIDGE;
	}

	// landforms and features a few hundred blocks across
	private static boolean isLandform(Terrain terrain) {
		return terrain.isLake() || terrain.isWetland() || terrain == TerrainType.DUNES || terrain == TerrainType.VOLCANO || terrain == TerrainType.LAGOON || terrain == TerrainType.DEEP_LAGOON
			|| terrain == TerrainType.SALT_FLAT || terrain == TerrainType.ALLUVIAL_FAN || terrain == TerrainType.GLACIAL_VALLEY || terrain == TerrainType.CIRQUE || terrain == TerrainType.DRUMLINS || terrain == TerrainType.KARST || terrain == TerrainType.DELTA || terrain == TerrainType.PENINSULA || terrain == TerrainType.COASTAL_ISLAND || terrain == TerrainType.VOLCANIC_ISLAND || terrain == TerrainType.SEAMOUNT || terrain == TerrainType.GUYOT || terrain == TerrainType.BLUE_HOLE || terrain == TerrainType.CORAL_REEF || terrain == TerrainType.SAND_WAVES;
	}

	/**
	 * @param step blocks between the positions tested
	 * @param minRadius places closer than this many blocks are skipped
	 * @param maxRadius how many steps out to search
	 * @param coreRadius a place is only taken if the terrain also lies around it at this distance, so the search
	 * doesn't lead to the ragged edge of a terrain, where it's mixed with others; the first place found at all is
	 * taken if no such place turns up
	 * @param timeoutMillis how long to search before giving up
	 * @return the place found, or null
	 */
	@Nullable
	public static Found locate(WorldLookup lookup, Terrain target, int originX, int originZ, int step, int minRadius, int maxRadius, int coreRadius, long timeoutMillis) {
		long minRadiusSq = (long) minRadius * minRadius;
		long timeOut = System.currentTimeMillis() + timeoutMillis;
		Cell cell = new Cell();
		Found edge = null;
		int x = 0;
		int z = 0;
		int dx = 0;
		int dz = -1;
		long size = 2L * maxRadius + 1;
		long max = size * size;
		for (long i = 0; i < max; i++) {
			// checking the clock every time costs more than the lookups
			if ((i & 63) == 0 && System.currentTimeMillis() > timeOut) {
				break;
			}
			int blockX = originX + x * step;
			int blockZ = originZ + z * step;
			long offX = blockX - originX;
			long offZ = blockZ - originZ;
			if (minRadiusSq == 0 || offX * offX + offZ * offZ >= minRadiusSq) {
				if (is(lookup, cell, blockX, blockZ, target)) {
					Found found = new Found(blockX, blockZ);
					if (coreRadius <= 0 || inside(lookup, blockX, blockZ, target, coreRadius)) {
						return found;
					}
					if (edge == null) {
						edge = found;
					}
				}
				// the edge found first is as good as it gets once the search has gone well past it
				if (edge != null && Math.abs(x) + Math.abs(z) > 0 && distanceSq(edge, originX, originZ) * 4 < offX * offX + offZ * offZ) {
					return edge;
				}
			}
			if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
				int turn = dx;
				dx = -dz;
				dz = turn;
			}
			x += dx;
			z += dz;
		}
		return edge;
	}

	private static boolean is(WorldLookup lookup, Cell cell, int x, int z, Terrain target) {
		// the cell is reused, so clear what the last place left in it
		lookup.apply(cell.reset(), x, z);
		return target.equals(cell.terrain);
	}

	private static boolean inside(WorldLookup lookup, int x, int z, Terrain target, int radius) {
		Cell cell = new Cell();
		int count = 0;
		for (int i = 0; i < RING.length; i++) {
			if (is(lookup, cell, x + RING[i][0] * radius, z + RING[i][1] * radius, target)) {
				count++;
			}
			// too many misses already
			if (i + 1 - count > RING.length - INSIDE) {
				return false;
			}
		}
		return true;
	}

	private static long distanceSq(Found found, int x, int z) {
		long dx = found.x() - x;
		long dz = found.z() - z;
		return dx * dx + dz * dz;
	}

	/**
	 * The block to stand on at a place: above the ground as the world has it, after erosion, or on the water over it.
	 */
	public static int surface(WorldLookup lookup, Levels levels, int x, int z) {
		Cell cell = new Cell();
		lookup.apply(cell, x, z, true);
		int y = Math.max(levels.scale(cell.height), levels.waterY);
		if (cell.waterLevel > 0.0F) {
			y = Math.max(y, levels.scale(cell.waterLevel));
		}
		return y + 1;
	}

	public record Found(int x, int z) {
	}
}
