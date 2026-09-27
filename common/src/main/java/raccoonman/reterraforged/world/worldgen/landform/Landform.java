package raccoonman.reterraforged.world.worldgen.landform;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * A distinct feature of the land, like a butte or a sea stack, shaped into the heightmap after the terrain types and
 * rivers.
 */
public interface Landform {

	/**
	 * @param heightmap for landforms that need the terrain elsewhere, e.g. at their centre
	 */
	void apply(Cell cell, float x, float z, Heightmap heightmap);

	// like CellPopulator.mapNoise, so the heightmap's per thread caches reach the landform's noises
	Landform mapNoise(Noise.Visitor visitor);

	/**
	 * The value the noise stays below over the given share of the land, measured over a wide grid, so a setting like
	 * "30% of coasts" means that however the noise's values are spread.
	 */
	static float quantile(Noise noise, float share) {
		int samples = 64;
		float[] values = new float[samples * samples];
		for (int i = 0; i < samples; i++) {
			for (int j = 0; j < samples; j++) {
				values[i * samples + j] = noise.compute(i * 97.0F - 3100.0F, j * 97.0F - 3100.0F, 0);
			}
		}
		java.util.Arrays.sort(values);
		int index = Math.max(0, Math.min(values.length - 1, Math.round(share * (values.length - 1))));
		return values[index];
	}

	// 0 to 1, fixed for each grid cell and purpose
	static float random(int seed, int gridX, int gridZ, int purpose) {
		return (NoiseUtil.valCoord2D(seed + purpose * 1013, gridX, gridZ) + 1.0F) * 0.5F;
	}

	// rolling land: not the badlands and plateaus where buttes stand, volcanoes, mountains, rivers, lakes or the sea
	static boolean isRollingLand(Terrain terrain) {
		TerrainCategory category = terrain.getCategory();
		boolean rolling = category == TerrainCategory.FLATLAND || category == TerrainCategory.LOWLAND;
		return rolling && !terrain.isVolcano() && !terrain.isWetland() && !terrain.includes(TerrainType.BADLANDS) && !terrain.includes(TerrainType.PLATEAU);
	}

	static float smoothstep(float value, float from, float to) {
		float t = Math.max(0.0F, Math.min(1.0F, (value - from) / (to - from)));
		return t * t * (3.0F - 2.0F * t);
	}
}
