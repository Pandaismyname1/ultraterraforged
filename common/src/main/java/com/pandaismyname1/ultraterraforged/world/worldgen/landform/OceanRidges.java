package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.OceanSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Mid-ocean ridges: long, broad ridges winding across the deep ocean floor where it's spreading apart, split along
 * their crest by a rift valley. Hydrothermal vents stand in the rift. The ridges follow the line where a slow noise
 * crosses its middle value.
 *
 * @param line traces the ridges where it's near 0.5
 * @param mask where the ridges are, above maskThreshold
 */
public record OceanRidges(Noise line, Noise mask, float maskThreshold, float shoreline, Levels levels) implements Landform {
	// the noise changes by about this much per block, to turn its values into distances
	private static final float GRADIENT = 1.0F / 1800.0F;
	// half the ridge's width, and the rift's, in blocks
	private static final float WIDTH = 160.0F;
	private static final float RIFT = 28.0F;
	private static final float HEIGHT = 16.0F;
	private static final float RIFT_DEPTH = 9.0F;
	// only out where the continent value is this far below the shoreline: the deep ocean
	private static final float OPEN_SEA = 0.22F;
	private static final int MIN_DEPTH = 20;

	public static OceanRidges make(int seed, OceanSettings.Ridges settings, float shoreline, Levels levels) {
		Noise line = Noises.perlin(seed, 3600, 2);
		Noise mask = Noises.perlin(seed + 1, 5000, 1);
		float frequency = NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F);
		float threshold = frequency >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(mask, 1.0F - frequency);
		return new OceanRidges(line, mask, threshold, shoreline, levels);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height > this.levels.water(-MIN_DEPTH) || cell.continentEdge > this.shoreline - OPEN_SEA) {
			return;
		}
		float distance = Math.abs(this.line.compute(x, z, 0) - 0.5F) / GRADIENT;
		if (distance >= WIDTH) {
			return;
		}
		float mask = this.maskThreshold == Float.NEGATIVE_INFINITY ? 1.0F : Landform.smoothstep(this.mask.compute(x, z, 0), this.maskThreshold, this.maskThreshold + 0.05F);
		// fading out towards shallower water
		mask *= 1.0F - Landform.smoothstep(cell.height, this.levels.water(-MIN_DEPTH - 6), this.levels.water(-MIN_DEPTH));
		if (mask <= 0.0F) {
			return;
		}
		float t = 1.0F - distance / WIDTH;
		float blocks = HEIGHT * t * t * (3.0F - 2.0F * t);
		boolean rift = distance < RIFT;
		if (rift) {
			blocks -= RIFT_DEPTH * Landform.smoothstep(1.0F - distance / RIFT, 0.0F, 0.4F);
		}
		cell.height += blocks * mask * this.levels.unit;
		if (rift && mask > 0.3F) {
			cell.terrain = TerrainType.OCEAN_RIDGE;
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new OceanRidges(this.line.mapAll(visitor), this.mask.mapAll(visitor), this.maskThreshold, this.shoreline, this.levels);
	}
}
