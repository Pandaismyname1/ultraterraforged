package raccoonman.reterraforged.world.worldgen.landform;

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
 * Karst: limestone worn away by rain in warm, wet lands, as around Guilin or Ha Long Bay. Clusters of tower hills rise
 * with near sheer sides and rounded tops, and the ground between them is pitted with sinkholes, some of them cenotes,
 * open down to a pool of water.
 *
 * @param field where the karst lies, above fieldLow
 * @param wobble roughens the towers' sides
 */
public record Karst(int seed, float height, Noise field, float fieldLow, float fieldHigh, Noise wobble, Levels levels, ClimateGrid climate, SiteCache<Float> cenotes) implements Landform {
	private static final float TOWER_GRID = 44.0F;
	private static final float TOWER_CHANCE = 0.7F;
	private static final float MIN_TOWER_RADIUS = 7.0F;
	private static final float MAX_TOWER_RADIUS = 14.0F;
	// the towers' sides take up this share of their radius; the rest is their rounded top
	private static final float TOWER_SIDE = 0.22F;
	private static final float HOLE_GRID = 72.0F;
	private static final float HOLE_CHANCE = 0.3F;
	private static final float MIN_HOLE_RADIUS = 5.0F;
	private static final float MAX_HOLE_RADIUS = 12.0F;
	// the share of sinkholes that are cenotes, sheer sided and holding water
	private static final float CENOTE_CHANCE = 0.4F;
	// how far below the ground around a cenote its water lies
	private static final float CENOTE_WATER = 5.0F;

	public static Karst make(int seed, LandformSettings.Karst settings, Levels levels) {
		Noise field = Noises.perlin(seed, 640, 2);
		float coverage = NoiseUtil.clamp(settings.coverage, 0.0F, 1.0F);
		float fieldLow = coverage >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, 1.0F - coverage);
		float fieldHigh = coverage >= 1.0F ? Float.NEGATIVE_INFINITY : Landform.quantile(field, Math.min(1.0F, 1.0F - coverage + 0.1F));
		return new Karst(seed, settings.height, field, fieldLow, fieldHigh, Noises.perlin(seed + 1, 9, 2), levels, ClimateGrid.of(64.0F, BiomeType.TROPICAL_RAINFOREST, BiomeType.TEMPERATE_RAINFOREST), SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (!Landform.isRollingLand(cell.terrain) || cell.terrain == TerrainType.TOR) {
			return;
		}
		float mask = Landform.smoothstep((cell.height - this.levels.water) * this.levels.worldHeight, 2.0F, 6.0F);
		mask *= Landform.smoothstep(cell.riverDistance, 0.3F, 0.6F);
		if (mask <= 0.0F) {
			return;
		}
		if (this.fieldHigh != Float.NEGATIVE_INFINITY) {
			mask *= Landform.smoothstep(this.field.compute(x, z, 0), this.fieldLow, this.fieldHigh);
			if (mask <= 0.0F) {
				return;
			}
		}
		mask *= Landform.smoothstep(this.climate.get(x, z, heightmap), 0.5F, 1.0F);
		if (mask <= 0.0F) {
			return;
		}
		float wobble = (this.wobble.compute(x, z, 0) - 0.5F) * 0.3F;
		float tower = this.tower(x, z, wobble);
		if (tower > 0.0F) {
			float rise = tower * this.height * mask;
			if (rise > 0.5F) {
				cell.height += rise * this.levels.unit;
				cell.terrain = TerrainType.KARST;
				// keep the sides sheer
				cell.erosionMask = true;
				return;
			}
		}
		// only where the land is fully karst, so no hole opens by a river or on the coast
		if (mask > 0.9F) {
			this.sinkhole(cell, x, z, wobble, heightmap);
		}
	}

	// 0 to 1: the highest tower here
	private float tower(float x, float z, float wobble) {
		int gridX = NoiseUtil.floor(x / TOWER_GRID);
		int gridZ = NoiseUtil.floor(z / TOWER_GRID);
		float best = 0.0F;
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (Landform.random(this.seed, gx, gz, 0) >= TOWER_CHANCE) {
					continue;
				}
				float radius = NoiseUtil.lerp(MIN_TOWER_RADIUS, MAX_TOWER_RADIUS, Landform.random(this.seed, gx, gz, 3));
				float centerX = (gx + 0.5F) * TOWER_GRID + (Landform.random(this.seed, gx, gz, 1) - 0.5F) * (TOWER_GRID - radius);
				float centerZ = (gz + 0.5F) * TOWER_GRID + (Landform.random(this.seed, gx, gz, 2) - 0.5F) * (TOWER_GRID - radius);
				float dx = x - centerX;
				float dz = z - centerZ;
				float distance = (float) Math.sqrt(dx * dx + dz * dz) / radius + wobble;
				if (distance >= 1.0F) {
					continue;
				}
				// sheer sides, then a rounded top
				float sides = NoiseUtil.clamp((1.0F - distance) / TOWER_SIDE, 0.0F, 1.0F);
				float top = 1.0F - 0.3F * distance * distance;
				float size = NoiseUtil.lerp(0.4F, 1.0F, Landform.random(this.seed, gx, gz, 4));
				best = Math.max(best, size * (float) Math.sqrt(sides) * top);
			}
		}
		return best;
	}

	private void sinkhole(Cell cell, float x, float z, float wobble, Heightmap heightmap) {
		int gridX = NoiseUtil.floor(x / HOLE_GRID);
		int gridZ = NoiseUtil.floor(z / HOLE_GRID);
		if (Landform.random(this.seed + 1, gridX, gridZ, 0) >= HOLE_CHANCE) {
			return;
		}
		float radius = NoiseUtil.lerp(MIN_HOLE_RADIUS, MAX_HOLE_RADIUS, Landform.random(this.seed + 1, gridX, gridZ, 3));
		// well inside the cell, so it's clear of the next one
		float free = HOLE_GRID - radius * 2.0F - 8.0F;
		float centerX = gridX * HOLE_GRID + radius + 4.0F + Landform.random(this.seed + 1, gridX, gridZ, 1) * free;
		float centerZ = gridZ * HOLE_GRID + radius + 4.0F + Landform.random(this.seed + 1, gridX, gridZ, 2) * free;
		float dx = x - centerX;
		float dz = z - centerZ;
		float distance = (float) Math.sqrt(dx * dx + dz * dz) / radius + wobble * 0.5F;
		if (distance >= 1.0F) {
			return;
		}
		boolean cenote = Landform.random(this.seed + 1, gridX, gridZ, 4) < CENOTE_CHANCE;
		float depth = NoiseUtil.lerp(8.0F, 18.0F, Landform.random(this.seed + 1, gridX, gridZ, 5));
		// a cenote drops sheer; a sinkhole is a steep funnel
		float shape = cenote ? NoiseUtil.clamp((1.0F - distance) / 0.12F, 0.0F, 1.0F) : 1.0F - distance * distance;
		cell.terrain = TerrainType.SINKHOLE;
		cell.erosionMask = true;
		if (!cenote) {
			cell.height -= depth * shape * this.levels.unit;
			return;
		}
		// one level over the whole pool, below the ground at its middle; near the sea the water table is the sea's
		// level, and the cenote reaches down to it
		float water = this.cenotes.get(gridX, gridZ, (gx, gz) -> heightmap.sampleGround(centerX, centerZ).height - CENOTE_WATER * this.levels.unit);
		boolean raised = water > this.levels.water(2);
		float bottom = raised ? cell.height - depth * this.levels.unit : Math.min(cell.height - depth * this.levels.unit, this.levels.water(-(int) (depth * 0.5F)));
		cell.height = NoiseUtil.lerp(cell.height, bottom, shape);
		if (raised && cell.height < water) {
			cell.waterLevel = cell.waterLevel > 0.0F ? Math.min(cell.waterLevel, water) : water;
		}
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Karst(this.seed, this.height, this.field.mapAll(visitor), this.fieldLow, this.fieldHigh, this.wobble.mapAll(visitor), this.levels, this.climate, this.cenotes);
	}
}
