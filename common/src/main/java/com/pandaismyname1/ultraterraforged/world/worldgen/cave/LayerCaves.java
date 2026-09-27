package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Landform;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

/**
 * Layer caves: wide, low caves running level along a bed of the rock, where a softer layer wore away. The beds lie a
 * set distance apart, tilting gently across the land, so where they meet a cliff their mouths open in rows. Carved
 * column by column, so each chunk carves its own part.
 */
final class LayerCaves implements CaveFeatures.Feature {
	private static final int SPACING = 22;
	private static final int FIRST = -40;
	private static final int BEDS = 12;
	// the tallest caves are this many blocks high
	private static final float HEIGHT = 5.0F;

	private final Noise tilt;
	private final Noise[] beds;
	private final float threshold;

	LayerCaves(int seed, float frequency) {
		this.tilt = Noises.perlin(seed, 600, 2);
		this.beds = new Noise[BEDS];
		for (int i = 0; i < BEDS; i++) {
			this.beds[i] = Noises.perlin(seed + 1 + i, 56, 2);
		}
		// at full frequency about a fifth of each bed is hollow
		this.threshold = Landform.quantile(this.beds[0], 1.0F - 0.2F * frequency);
	}

	@Override
	public void carve(CaveCarving carving) {
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				int x = carving.minX + dx;
				int z = carving.minZ + dz;
				int ground = carving.ground(x, z);
				float shift = (this.tilt.compute(x, z, 0) - 0.5F) * 16.0F;
				for (int i = 0; i < BEDS; i++) {
					int bed = FIRST + i * SPACING + Math.round(shift);
					if (bed <= carving.bottom + 8 || bed > ground - 2) {
						continue;
					}
					float value = this.beds[i].compute(x, z, 0);
					if (value <= this.threshold) {
						continue;
					}
					int height = 2 + Math.round((value - this.threshold) / (1.0F - this.threshold) * (HEIGHT - 2.0F));
					// under a roof of a few blocks, so fields don't cave in; a cliff face still opens onto the air beside it
					for (int y = bed; y < bed + height && y < ground - 4; y++) {
						carving.clear(x, y, z, CaveCarving.DRY);
					}
				}
			}
		}
	}
}
