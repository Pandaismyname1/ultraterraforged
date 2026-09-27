package raccoonman.reterraforged.world.worldgen.landform;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

/**
 * A value worked out at the corners of a grid, like the lowest ground nearby, and blended between them, so something
 * costly to measure is measured rarely and still varies smoothly. The samples belong to one world, so each landform
 * needs its own grid.
 */
public record SampleGrid(float spacing, ThreadLocal<Long2FloatOpenHashMap> cache) {
	private static final int LIMIT = 16384;

	public interface Sampler {
		float sample(float x, float z);
	}

	public static SampleGrid of(float spacing) {
		return new SampleGrid(spacing, ThreadLocal.withInitial(Long2FloatOpenHashMap::new));
	}

	public float get(float x, float z, Sampler sampler) {
		float gx = x / this.spacing;
		float gz = z / this.spacing;
		int x0 = NoiseUtil.floor(gx);
		int z0 = NoiseUtil.floor(gz);
		float tx = gx - x0;
		float tz = gz - z0;
		float top = NoiseUtil.lerp(this.corner(x0, z0, sampler), this.corner(x0 + 1, z0, sampler), tx);
		float bottom = NoiseUtil.lerp(this.corner(x0, z0 + 1, sampler), this.corner(x0 + 1, z0 + 1, sampler), tx);
		return NoiseUtil.lerp(top, bottom, tz);
	}

	public float corner(int gridX, int gridZ, Sampler sampler) {
		Long2FloatOpenHashMap cache = this.cache.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (cache.containsKey(key)) {
			return cache.get(key);
		}
		if (cache.size() > LIMIT) {
			cache.clear();
		}
		float value = sampler.sample(gridX * this.spacing, gridZ * this.spacing);
		cache.put(key, value);
		return value;
	}
}
