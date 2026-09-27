package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.OceanSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.type.BiomeType;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Blue holes: deep, round, sheer-sided pits in warm, shallow seas, as off Belize or the Bahamas, sinkholes flooded when
 * the sea rose. From above they show as circles of deep blue in the pale shallows. Each stands in its own cell of a
 * grid, wherever that holds warm, shallow sea.
 *
 * @param wobble roughens their rims
 */
public record BlueHoles(int seed, float frequency, Noise wobble, Levels levels, ClimateGrid climate, SiteCache<Hole> holes) implements Landform {
	private static final float GRID = 500.0F;
	// the sea over them is no deeper than this many blocks
	private static final int MAX_DEPTH = 18;
	private static final Hole NONE = new Hole(0.0F, 0.0F, 0.0F, 0.0F);

	record Hole(float x, float z, float radius, float bottom) {
	}

	public static BlueHoles make(int seed, OceanSettings.BlueHoles settings, Levels levels) {
		return new BlueHoles(seed, NoiseUtil.clamp(settings.frequency, 0.0F, 1.0F), Noises.perlin(seed + 1, 12, 2), levels, ClimateGrid.of(128.0F, BiomeType.TROPICAL_RAINFOREST, BiomeType.SAVANNA, BiomeType.DESERT), SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.height >= this.levels.water || cell.height < this.levels.water(-MAX_DEPTH - 6)) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		if (Landform.random(this.seed, gridX, gridZ, 0) >= this.frequency) {
			return;
		}
		Hole hole = this.holes.get(gridX, gridZ, (hx, hz) -> this.find(hx, hz, heightmap));
		if (hole == NONE) {
			return;
		}
		float dx = x - hole.x();
		float dz = z - hole.z();
		float d = (float) Math.sqrt(dx * dx + dz * dz) / hole.radius() + (this.wobble.compute(x, z, 0) - 0.5F) * 0.15F;
		if (d >= 1.0F) {
			return;
		}
		// sheer walls
		float inside = Landform.smoothstep(1.0F - d, 0.0F, 0.1F);
		cell.height = Math.min(cell.height, NoiseUtil.lerp(cell.height, hole.bottom(), inside));
		cell.erosionMask = true;
		if (inside > 0.5F) {
			cell.terrain = TerrainType.BLUE_HOLE;
		}
	}

	private Hole find(int gridX, int gridZ, Heightmap heightmap) {
		float margin = 60.0F;
		float x = gridX * GRID + margin + Landform.random(this.seed, gridX, gridZ, 1) * (GRID - margin * 2.0F);
		float z = gridZ * GRID + margin + Landform.random(this.seed, gridX, gridZ, 2) * (GRID - margin * 2.0F);
		Cell ground = heightmap.sampleGround(x, z);
		if (ground.height >= this.levels.water(-2) || ground.height < this.levels.water(-MAX_DEPTH - 12)) {
			return NONE;
		}
		if (this.climate.get(x, z, heightmap) < 0.5F) {
			return NONE;
		}
		float radius = NoiseUtil.lerp(12.0F, 34.0F, Landform.random(this.seed, gridX, gridZ, 3));
		float depth = NoiseUtil.lerp(40.0F, 70.0F, Landform.random(this.seed, gridX, gridZ, 4));
		return new Hole(x, z, radius, this.levels.water - depth * this.levels.unit);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new BlueHoles(this.seed, this.frequency, this.wobble.mapAll(visitor), this.levels, this.climate, this.holes);
	}
}
