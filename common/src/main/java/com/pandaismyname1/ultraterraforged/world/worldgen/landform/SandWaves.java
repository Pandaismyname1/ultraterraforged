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
 * Sand waves: fields of long, rippled ridges of sand on the shallow sea floor, lined up across the tidal currents,
 * which run the same way over the whole world. The surface rules make them sand.
 *
 * @param field where the fields lie, above fieldThreshold
 * @param warp bends the ridges
 */
public record SandWaves(float height, float cosCurrent, float sinCurrent, Noise field, float fieldThreshold, Noise warp, Levels levels) implements Landform {
	private static final float WAVELENGTH = 18.0F;
	// the gentle side of each ridge takes this share of the way to the next
	private static final float GENTLE = 0.7F;
	// only on the floor between these depths, in blocks
	private static final int MIN_DEPTH = 5;
	private static final int MAX_DEPTH = 30;

	public static SandWaves make(int seed, OceanSettings.SandWaves settings, Levels levels) {
		float current = (NoiseUtil.valCoord2D(seed, 0, 0) + 1.0F) * (float) Math.PI;
		Noise field = Noises.perlin(seed + 1, 260, 2);
		return new SandWaves(settings.height, NoiseUtil.cos(current), NoiseUtil.sin(current), field, Landform.quantile(field, 0.55F), Noises.perlin(seed + 2, 90, 1), levels);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height > this.levels.water(-MIN_DEPTH) || cell.height < this.levels.water(-MAX_DEPTH) || !cell.terrain.isShallowOcean() && !cell.terrain.isDeepOcean()) {
			return;
		}
		float mask = Landform.smoothstep(this.field.compute(x, z, 0), this.fieldThreshold, this.fieldThreshold + 0.08F);
		// fading out at the depths they keep to
		mask *= Landform.smoothstep(cell.height, this.levels.water(-MAX_DEPTH), this.levels.water(-MAX_DEPTH + 4)) * (1.0F - Landform.smoothstep(cell.height, this.levels.water(-MIN_DEPTH - 3), this.levels.water(-MIN_DEPTH)));
		if (mask <= 0.0F) {
			return;
		}
		float across = x * this.cosCurrent + z * this.sinCurrent + (this.warp.compute(x, z, 0) - 0.5F) * 40.0F;
		float phase = across / WAVELENGTH;
		phase -= NoiseUtil.floor(phase);
		float profile = phase < GENTLE ? Landform.smoothstep(phase, 0.0F, GENTLE) : 1.0F - Landform.smoothstep(phase, GENTLE, 1.0F);
		float rise = this.height * mask * profile;
		cell.height += rise * this.levels.unit;
		if (mask > 0.5F && !cell.terrain.isDeepOcean()) {
			cell.terrain = TerrainType.SAND_WAVES;
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new SandWaves(this.height, this.cosCurrent, this.sinCurrent, this.field.mapAll(visitor), this.fieldThreshold, this.warp.mapAll(visitor), this.levels);
	}
}
