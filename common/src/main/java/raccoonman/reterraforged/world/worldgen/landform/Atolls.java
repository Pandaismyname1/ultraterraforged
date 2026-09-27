package raccoonman.reterraforged.world.worldgen.landform;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Rings of coral reef far out in warm seas, with low sandy islets on the reef and a shallow lagoon inside. Each atoll
 * sits in its own cell of a grid.
 *
 * @param islets where along the reef land breaks the surface
 * @param shoreline continent edge values this far below the shore are the open sea
 */
public record Atolls(int seed, float chance, float gridSize, float shoreline, Noise warpX, Noise warpZ, Noise islets, float isletThreshold, Levels levels, ClimateGrid climate, ThreadLocal<Long2FloatOpenHashMap> sites) implements Landform {
	// in blocks
	private static final float MIN_RADIUS = 45.0F;
	private static final float MAX_RADIUS = 115.0F;
	private static final float OUTER_SLOPE = 45.0F;
	private static final int LAGOON_DEPTH = 5;
	private static final int REEF_DEPTH = 1;
	private static final int ISLET_HEIGHT = 2;
	// atolls stand this far out in the open sea, in continent edge units
	private static final float OFFSHORE = 0.1F;
	private static final float ABSENT = -1.0F;

	public static Atolls make(int seed, LandformSettings.Atolls settings, float shoreline, Levels levels) {
		float gridSize = 520.0F * settings.size;
		Noise warpX = Noises.perlin(seed + 1, 90, 2);
		Noise warpZ = Noises.perlin(seed + 2, 90, 2);
		Noise islets = Noises.perlin(seed + 3, 28, 2);
		// about half the reef carries islets
		float isletThreshold = Landform.quantile(islets, 0.5F);
		return new Atolls(seed, settings.frequency, gridSize, shoreline, warpX, warpZ, islets, isletThreshold, levels, ClimateGrid.of(128.0F, BiomeType.TROPICAL_RAINFOREST, BiomeType.SAVANNA, BiomeType.DESERT), ThreadLocal.withInitial(Long2FloatOpenHashMap::new));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.continentEdge > this.shoreline - OFFSHORE * 0.5F || cell.height >= this.levels.water(-2)) {
			return;
		}
		int gridX = NoiseUtil.floor(x / this.gridSize);
		int gridZ = NoiseUtil.floor(z / this.gridSize);
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				this.applyAtoll(cell, x, z, gridX + dx, gridZ + dz, heightmap);
			}
		}
	}

	private void applyAtoll(Cell cell, float x, float z, int gridX, int gridZ, Heightmap heightmap) {
		if (random(this.seed, gridX, gridZ, 0) >= this.chance) {
			return;
		}
		float scale = this.gridSize / 520.0F;
		float radius = NoiseUtil.lerp(MIN_RADIUS, MAX_RADIUS, random(this.seed, gridX, gridZ, 1)) * scale;
		float reef = NoiseUtil.lerp(7.0F, 13.0F, random(this.seed, gridX, gridZ, 2));
		float extent = radius + reef + OUTER_SLOPE;
		float free = Math.max(0.0F, this.gridSize - extent * 2.0F);
		float centerX = gridX * this.gridSize + extent + random(this.seed, gridX, gridZ, 3) * free;
		float centerZ = gridZ * this.gridSize + extent + random(this.seed, gridX, gridZ, 4) * free;
		// an uneven ring
		float wobble = radius * 0.3F;
		float dx = x + this.warpX.compute(x, z, 0) * wobble - centerX;
		float dz = z + this.warpZ.compute(x, z, 0) * wobble - centerZ;
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		if (distance > extent) {
			return;
		}
		if (!this.stands(gridX, gridZ, centerX, centerZ, heightmap)) {
			return;
		}
		float fromReef = Math.abs(distance - radius);
		float height;
		if (fromReef <= reef) {
			// the reef, with islets where it breaks the surface
			boolean islet = this.islets.compute(x, z, 0) > this.isletThreshold && fromReef < reef * 0.8F;
			height = islet ? this.levels.water(ISLET_HEIGHT) : this.levels.water(-REEF_DEPTH);
			cell.terrain = islet ? TerrainType.BEACH : TerrainType.SHALLOW_OCEAN;
		} else if (distance < radius) {
			// the lagoon
			height = Math.max(cell.height, this.levels.water(-LAGOON_DEPTH));
			cell.terrain = TerrainType.SHALLOW_OCEAN;
		} else {
			// the outer slope down to the deep sea
			float t = (distance - radius - reef) / OUTER_SLOPE;
			height = NoiseUtil.lerp(this.levels.water(-REEF_DEPTH - 1), cell.height, Landform.smoothstep(t, 0.0F, 1.0F));
			if (height <= cell.height) {
				return;
			}
		}
		cell.height = Math.max(cell.height, height);
		cell.erosionMask = true;
	}

	// whether this site holds an atoll: open, deep, warm sea; remembered for the cells around it
	private boolean stands(int gridX, int gridZ, float centerX, float centerZ, Heightmap heightmap) {
		Long2FloatOpenHashMap sites = this.sites.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (sites.containsKey(key)) {
			return sites.get(key) != ABSENT;
		}
		if (sites.size() > 4096) {
			sites.clear();
		}
		Cell center = heightmap.sampleTerrain(centerX, centerZ);
		boolean stands = center.continentEdge < this.shoreline - OFFSHORE && center.height < this.levels.water(-8) && this.climate.get(centerX, centerZ, heightmap) > 0.5F;
		sites.put(key, stands ? 1.0F : ABSENT);
		return stands;
	}

	private static float random(int seed, int gridX, int gridZ, int purpose) {
		return (NoiseUtil.valCoord2D(seed + purpose * 1013, gridX, gridZ) + 1.0F) * 0.5F;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Atolls(this.seed, this.chance, this.gridSize, this.shoreline, this.warpX.mapAll(visitor), this.warpZ.mapAll(visitor), this.islets.mapAll(visitor), this.isletThreshold, this.levels, this.climate, this.sites);
	}
}
