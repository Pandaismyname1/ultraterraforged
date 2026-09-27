package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.OceanSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

/**
 * Continental shelves: off every coast, a broad, shallow shelf sloping gently out from the shore, ending in a clear
 * drop-off down to the deep sea floor. The sea floor is only ever raised to the shelf, never lowered.
 *
 * @param width how far the shelf reaches out, in continent values
 * @param wobble roughens the shelf's edge
 */
public record Shelves(float shoreline, float width, Noise wobble, Levels levels) implements Landform {
	// the shelf's depth, in blocks, at the shore and at its edge
	private static final float SHORE_DEPTH = 2.0F;
	private static final float EDGE_DEPTH = 13.0F;
	// the drop-off takes up this much of the continent values past the shelf's edge
	private static final float DROP = 0.02F;
	private static final float WIDTH = 0.17F;

	public static Shelves make(int seed, OceanSettings.Shelves settings, float shoreline, Levels levels) {
		return new Shelves(shoreline, WIDTH * Math.max(0.1F, settings.width), Noises.perlin(seed, 300, 2), levels);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return;
		}
		float edge = this.shoreline - this.width * NoiseUtil.lerp(0.75F, 1.25F, this.wobble.compute(x, z, 0));
		if (cell.continentEdge < edge - DROP) {
			return;
		}
		float out = NoiseUtil.clamp((this.shoreline - cell.continentEdge) / (this.shoreline - edge), 0.0F, 1.0F);
		float depth = NoiseUtil.lerp(SHORE_DEPTH, EDGE_DEPTH, (float) Math.pow(out, 0.8F));
		float shelf = this.levels.water - depth * this.levels.unit;
		// the drop-off
		float mask = Landform.smoothstep(cell.continentEdge, edge - DROP, edge);
		float height = NoiseUtil.lerp(cell.height, shelf, mask);
		if (height > cell.height) {
			cell.height = height;
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Shelves(this.shoreline, this.width, this.wobble.mapAll(visitor), this.levels);
	}
}
