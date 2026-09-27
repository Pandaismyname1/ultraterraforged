package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.Terrain;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Rock shelters: overhangs worn into the foot of cliffs in badlands, plateaus and canyons, as the softer rock at the
 * bottom of a cliff wears back faster than the rock above it. Carved column by column into the cliff's own chunk.
 */
final class RockShelters implements CaveFeatures.Feature {
	private static final int[][] DIRECTIONS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
	// a cliff rises at least this many blocks over its foot, within REACH blocks
	private static final int CLIFF = 8;
	private static final int REACH = 6;

	private final Noise depth;
	private final Noise height;
	private final float frequency;

	RockShelters(int seed, float frequency) {
		this.depth = Noises.perlin(seed, 20, 2);
		this.height = Noises.perlin(seed + 1, 14, 1);
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		// only chunks in the right country
		Cell middle = carving.cell(carving.minX + 8, carving.minZ + 8);
		if (!suits(middle.terrain)) {
			return;
		}
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				int x = carving.minX + dx;
				int z = carving.minZ + dz;
				int top = carving.ground(x, z);
				for (int[] direction : DIRECTIONS) {
					// the foot of the cliff this column is part of, and how far this column lies back from its face
					for (int step = 1; step <= REACH; step++) {
						int foot = carving.ground(x + direction[0] * step, z + direction[1] * step);
						if (foot > top - CLIFF) {
							continue;
						}
						float worn = (1.0F + this.depth.compute(x, z, 0) * 6.0F) * this.frequency;
						if (step - 1 < worn) {
							int height = 3 + Math.round(this.height.compute(x, z, 0) * 3.0F);
							// a roof of rock stays over it
							for (int y = foot + 1; y <= foot + height && y < top - 2; y++) {
								carving.clear(x, y, z, CaveCarving.DRY);
							}
						}
						break;
					}
				}
			}
		}
	}

	private static boolean suits(Terrain terrain) {
		return terrain.includes(TerrainType.BADLANDS) || terrain.includes(TerrainType.PLATEAU) || terrain == TerrainType.KARST;
	}
}
