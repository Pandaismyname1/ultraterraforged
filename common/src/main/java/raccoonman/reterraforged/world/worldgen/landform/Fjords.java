package raccoonman.reterraforged.world.worldgen.landform;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Drowned valleys on cold and rainy coasts, the way real fjords formed: the land near the sea is lowered, high ground
 * more than low, so the sea floods up the valleys between steep ridges in long, branching inlets. Positions inland are
 * measured in continent edge values, which change by about 0.001 per block near the shore.
 *
 * @param depth how far high ground at the coast is lowered, as a share of the world height
 * @param reach how far inland the land is lowered, in continent edge units
 * @param shoreline the continent edge value at the natural shoreline
 */
public record Fjords(float depth, float reach, float shoreline, Levels levels, ClimateGrid climate, ThreadLocal<Long2FloatOpenHashMap> grounds, ThreadLocal<Long2FloatOpenHashMap> surroundings) implements Landform {
	// high ground is lowered fully, ground this many blocks above the sea hardly at all
	private static final int FULL_HEIGHT = 30;
	// the drowned valleys are no deeper than this below the sea
	private static final int MAX_DEPTH = 25;
	private static final float GROUND_GRID = 16.0F;
	// the surroundings are averaged over this many grid points either way
	private static final int SURROUNDING_RADIUS = 3;
	// only valleys at least this many blocks below their surroundings sink, fully from VALLEY_FULL
	private static final float VALLEY_MIN = 3.0F;
	private static final float VALLEY_FULL = 14.0F;
	// and only among high ground: surroundings this far above the sea
	private static final float HIGH_MIN = 12.0F;
	private static final float HIGH_FULL = 40.0F;

	public static Fjords make(LandformSettings.Fjords settings, float shoreline, Levels levels) {
		return new Fjords(settings.depth * levels.unit, 0.2F * settings.reach, shoreline, levels, fjordClimate(), ThreadLocal.withInitial(Long2FloatOpenHashMap::new), ThreadLocal.withInitial(Long2FloatOpenHashMap::new));
	}

	// cold and rainy coasts, like Norway, Alaska or New Zealand; each world gets its own grid, the samples depend on it
	private static ClimateGrid fjordClimate() {
		return ClimateGrid.of(64.0F, BiomeType.TUNDRA, BiomeType.TAIGA, BiomeType.COLD_STEPPE, BiomeType.ALPINE, BiomeType.TEMPERATE_RAINFOREST);
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		float edge = cell.continentEdge;
		if (edge < this.shoreline - 0.05F || edge > this.shoreline + this.reach) {
			return;
		}
		if (cell.terrain.isRiver() || cell.terrain.isLake() || cell.terrain.isWetland() || cell.height <= this.levels.water(-MAX_DEPTH)) {
			return;
		}
		float inland = 1.0F - Landform.smoothstep(edge, this.shoreline + this.reach * 0.4F, this.shoreline + this.reach);
		if (inland <= 0.0F) {
			return;
		}
		// the valleys among high ground: how far this spot lies below the land around it
		float surroundings = this.blend(x, z, heightmap, this.surroundings.get(), this::surroundingsAt);
		float valley = (surroundings - cell.height) * this.levels.worldHeight;
		float high = Landform.smoothstep((surroundings - this.levels.water) * this.levels.worldHeight, HIGH_MIN, HIGH_FULL);
		float amount = this.depth * inland * high * Landform.smoothstep(valley, VALLEY_MIN, VALLEY_FULL);
		if (amount <= 0.0F) {
			return;
		}
		amount *= this.climate.get(x, z, heightmap);
		if (amount <= 0.0F) {
			return;
		}
		float lowered = Math.max(cell.height - amount, this.levels.water(-MAX_DEPTH));
		if (lowered < cell.height) {
			cell.height = lowered;
			if (cell.height < this.levels.water(-2)) {
				// an arm of the sea, with sea biomes
				cell.terrain = TerrainType.SHALLOW_OCEAN;
			}
		}
	}

	private interface GridValue {
		float at(int gridX, int gridZ, Heightmap heightmap);
	}

	// a value worked out at the corners of a 16 block grid and blended between them
	private float blend(float x, float z, Heightmap heightmap, Long2FloatOpenHashMap cache, GridValue value) {
		float gx = x / GROUND_GRID;
		float gz = z / GROUND_GRID;
		int x0 = NoiseUtil.floor(gx);
		int z0 = NoiseUtil.floor(gz);
		float tx = gx - x0;
		float tz = gz - z0;
		float top = NoiseUtil.lerp(cached(cache, x0, z0, heightmap, value), cached(cache, x0 + 1, z0, heightmap, value), tx);
		float bottom = NoiseUtil.lerp(cached(cache, x0, z0 + 1, heightmap, value), cached(cache, x0 + 1, z0 + 1, heightmap, value), tx);
		return NoiseUtil.lerp(top, bottom, tz);
	}

	private static float cached(Long2FloatOpenHashMap cache, int gridX, int gridZ, Heightmap heightmap, GridValue value) {
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (cache.containsKey(key)) {
			return cache.get(key);
		}
		if (cache.size() > 32768) {
			cache.clear();
		}
		float result = value.at(gridX, gridZ, heightmap);
		cache.put(key, result);
		return result;
	}

	// the average ground over a few grid points either way
	private float surroundingsAt(int gridX, int gridZ, Heightmap heightmap) {
		Long2FloatOpenHashMap grounds = this.grounds.get();
		float total = 0.0F;
		int count = 0;
		for (int dx = -SURROUNDING_RADIUS; dx <= SURROUNDING_RADIUS; dx++) {
			for (int dz = -SURROUNDING_RADIUS; dz <= SURROUNDING_RADIUS; dz++) {
				total += cached(grounds, gridX + dx, gridZ + dz, heightmap, (gx, gz, h) -> h.sampleGround(gx * GROUND_GRID, gz * GROUND_GRID).height);
				count++;
			}
		}
		return total / count;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return this;
	}
}
