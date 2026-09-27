package raccoonman.reterraforged.world.worldgen.landform;

import java.util.Set;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

/**
 * Whether the climate suits a landform, 1 or 0, sampled at the corners of a grid and blended between them, so the
 * landform fades out smoothly at climate borders. Landforms are shaped before the climate is worked out, so it's
 * sampled separately; the grid keeps that cheap. The samples belong to one world, so each landform needs its own
 * grid.
 */
public record ClimateGrid(Set<BiomeType> climates, float spacing, ThreadLocal<Long2FloatOpenHashMap> cache) {

	public static ClimateGrid of(float spacing, BiomeType... climates) {
		return new ClimateGrid(Set.of(climates), spacing, ThreadLocal.withInitial(Long2FloatOpenHashMap::new));
	}

	public float get(float x, float z, Heightmap heightmap) {
		float gx = x / this.spacing;
		float gz = z / this.spacing;
		int x0 = NoiseUtil.floor(gx);
		int z0 = NoiseUtil.floor(gz);
		float tx = gx - x0;
		float tz = gz - z0;
		float top = NoiseUtil.lerp(this.corner(x0, z0, heightmap), this.corner(x0 + 1, z0, heightmap), tx);
		float bottom = NoiseUtil.lerp(this.corner(x0, z0 + 1, heightmap), this.corner(x0 + 1, z0 + 1, heightmap), tx);
		return NoiseUtil.lerp(top, bottom, tz);
	}

	private float corner(int gridX, int gridZ, Heightmap heightmap) {
		Long2FloatOpenHashMap cache = this.cache.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (cache.containsKey(key)) {
			return cache.get(key);
		}
		if (cache.size() > 16384) {
			cache.clear();
		}
		BiomeType type = heightmap.sampleTerrain(gridX * this.spacing, gridZ * this.spacing).biomeType;
		float value = this.climates.contains(type) ? 1.0F : 0.0F;
		cache.put(key, value);
		return value;
	}
}
