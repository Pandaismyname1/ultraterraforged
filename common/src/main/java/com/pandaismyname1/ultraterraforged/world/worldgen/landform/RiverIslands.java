package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.CoastSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Islands in lakes, and in wide rivers long islands down the middle of the channel, splitting it in two. They rise a
 * block or two above the water, which is the sea's, or the river's or lake's own where it lies above the sea.
 *
 * @param lakeIslands where lakes have islands, above lakeThreshold
 * @param riverIslands where wide rivers have them, above riverThreshold
 */
public record RiverIslands(Noise lakeIslands, float lakeThreshold, Noise riverIslands, float riverThreshold, Levels levels) implements Landform {
	// rivers need banks at least this many blocks wide, either side of their middle, to have islands
	private static final float MIN_RIVER_WIDTH = 14.0F;
	// river islands keep to this share of the width around the middle
	private static final float RIVER_MIDDLE = 0.32F;
	// lake islands keep to this share of the way to the shore
	private static final float LAKE_MIDDLE = 0.75F;

	public static RiverIslands make(int seed, CoastSettings.RiverIslands settings, Levels levels) {
		float frequency = NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F);
		Noise lake = Noises.perlin(seed, 42, 2);
		Noise river = Noises.perlin(seed + 1, 30, 1);
		// at full frequency islands cover about a third of the water they can stand in
		float lakeThreshold = Landform.quantile(lake, 1.0F - 0.3F * frequency);
		float riverThreshold = Landform.quantile(river, 1.0F - 0.45F * frequency);
		return new RiverIslands(lake, frequency <= 0.0F ? Float.POSITIVE_INFINITY : lakeThreshold, river, frequency <= 0.0F ? Float.POSITIVE_INFINITY : riverThreshold, levels);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		boolean lake = cell.terrain.isLake();
		boolean river = cell.terrain.isRiver() && cell.riverWidth >= MIN_RIVER_WIDTH;
		if (!lake && !river) {
			return;
		}
		float water = cell.waterLevel > 0.0F ? cell.waterLevel : this.levels.water;
		if (cell.height >= water) {
			return;
		}
		float value;
		float threshold;
		if (lake) {
			if (cell.riverBank > LAKE_MIDDLE) {
				return;
			}
			value = this.lakeIslands.compute(x, z, 0);
			threshold = this.lakeThreshold;
			// towards the shore, only the biggest islands
			threshold += Landform.smoothstep(cell.riverBank, LAKE_MIDDLE * 0.6F, LAKE_MIDDLE) * 0.1F;
		} else {
			if (cell.riverBank > RIVER_MIDDLE) {
				return;
			}
			value = this.riverIslands.compute(x, z, 0);
			threshold = this.riverThreshold + Landform.smoothstep(cell.riverBank, RIVER_MIDDLE * 0.5F, RIVER_MIDDLE) * 0.08F;
		}
		if (value <= threshold) {
			return;
		}
		// a low island, a little higher in its middle, with a shallow shore
		float rise = Landform.smoothstep(value, threshold, threshold + 0.06F);
		float blocks = rise < 0.35F ? -1.0F : 1.0F + rise;
		cell.height = Math.max(cell.height, water + blocks * this.levels.unit);
		cell.erosionMask = true;
		if (cell.height >= water) {
			cell.terrain = TerrainType.RIVER_ISLAND;
			// dry: not under the water above the sea
			cell.waterLevel = 0.0F;
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new RiverIslands(this.lakeIslands.mapAll(visitor), this.lakeThreshold, this.riverIslands.mapAll(visitor), this.riverThreshold, this.levels);
	}
}
