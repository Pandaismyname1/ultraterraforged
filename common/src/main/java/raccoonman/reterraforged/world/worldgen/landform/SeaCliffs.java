package raccoonman.reterraforged.world.worldgen.landform;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.continent.CoastShaper;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Stretches of coast where the land ends in a sheer cliff straight into the sea, with no beach, and sea stacks
 * standing offshore. Land near the sea is raised, most at the shore and less further inland, while the sea is not, so
 * the cliff always stands exactly on the natural coastline. Along some stretches the lowest strip of shore is left
 * low, as a narrow gravel beach at the foot of the cliff.
 *
 * @param shoreline continent edge values below this are the open sea, rather than a lake inland
 * @param coastline where along the coast cliffs form
 * @param heightVariation how tall the cliffs are along the coast, as a share of the full height
 * @param beaches where along the cliff coast there are gravel beaches, above beachThreshold
 */
public record SeaCliffs(int seed, float shoreline, float threshold, float height, boolean stacks, Noise coastline, Noise heightVariation, Noise beaches, float beachThreshold, Levels levels, ThreadLocal<Long2FloatOpenHashMap> stackSites, ThreadLocal<Long2FloatOpenHashMap> nearSea) implements Landform {
	// in blocks
	private static final int SHELF_DEPTH = 2;
	// ground this many blocks above the sea is left as beach, where there is one
	private static final float BEACH_HEIGHT = 3.0F;
	// and it's a gravel beach under cliffs at least this tall
	private static final float MIN_BEACH_CLIFF = 6.0F;
	private static final float STACK_GRID = 40.0F;
	private static final float STACK_CHANCE = 0.35F;
	private static final float[] STACK_LAND_SEARCH = { 20.0F, 36.0F, 52.0F };
	private static final float ABSENT = -1.0F;
	private static final float SEA_GRID = 32.0F;
	private static final float[] SEA_SEARCH_RADII = { 0.0F, 24.0F, 48.0F, 72.0F };
	// continent edge values past this are far from any open sea
	private static final float INLAND = 0.25F;

	/**
	 * @param shoreline the continent edge value where the natural shoreline lies
	 */
	public static SeaCliffs make(int seed, LandformSettings.SeaCliffs settings, float shoreline, Levels levels) {
		Noise coastline = Noises.perlin(seed, 900, 2);
		Noise heightVariation = Noises.map(Noises.perlin(seed + 3, 300, 2), 0.55F, 1.15F);
		// the share of coast with cliffs sets how high the noise must be
		float threshold = Landform.quantile(coastline, 1.0F - Math.max(0.0F, Math.min(1.0F, settings.frequency)));
		Noise beaches = Noises.perlin(seed + 5, 260, 2);
		float gravelBeaches = Math.max(0.0F, Math.min(1.0F, settings.gravelBeaches));
		// none at all when turned down fully, rather than the odd spot at the noise's peaks
		float beachThreshold = gravelBeaches <= 0.0F ? Float.POSITIVE_INFINITY : Landform.quantile(beaches, 1.0F - gravelBeaches);
		return new SeaCliffs(seed, shoreline, threshold, settings.height, settings.seaStacks, coastline, heightVariation, beaches, beachThreshold, levels, ThreadLocal.withInitial(Long2FloatOpenHashMap::new), ThreadLocal.withInitial(Long2FloatOpenHashMap::new));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.continentEdge > this.shoreline + INLAND) {
			return;
		}
		float mask = this.cliffMask(x, z) * Landform.smoothstep(cell.riverDistance, 0.2F, 0.5F);
		// nor on bars of sand, too low and narrow to hold a cliff
		if (cell.terrain.isRiver() || cell.terrain.isLake() || cell.terrain.isWetland() || cell.coastFeature == CoastShaper.BAR) {
			mask = 0.0F;
		}
		if (mask > 0.0F) {
			float proximity = this.seaProximity(x, z, heightmap);
			if (proximity > 0.0F) {
				this.applyCliff(cell, x, z, mask, proximity);
			}
		}
		if (this.stacks) {
			this.applyStacks(cell, x, z, heightmap);
		}
	}

	private void applyCliff(Cell cell, float x, float z, float mask, float proximity) {
		float beach = this.beachThreshold == Float.POSITIVE_INFINITY ? 0.0F : Landform.smoothstep(this.beaches.compute(x, z, 0), this.beachThreshold - 0.02F, this.beachThreshold + 0.02F);
		if (cell.height >= this.levels.water) {
			// land: raised most at the shore, sloping back down to the natural land inland
			float shape = Landform.smoothstep(proximity, 0.15F, 0.85F);
			// but the lowest strip of shore stays low where there is a beach, with the cliff rising behind it
			// (only right by the sea: low land further back still gets the cliff, rising behind the beach)
			float strip = beach * (1.0F - Landform.smoothstep((cell.height - this.levels.water) * this.levels.worldHeight, BEACH_HEIGHT, BEACH_HEIGHT + 1.5F));
			strip *= Landform.smoothstep(proximity, 0.55F, 0.72F);
			float cliff = this.height * this.heightVariation.compute(x, z, 0) * shape * mask;
			cell.height += cliff * (1.0F - strip) * this.levels.unit;
			if (shape > 0.6F) {
				// keep the cliff sheer; the filters would round it off
				cell.erosionMask = true;
			}
			// the beach is gravel where a real cliff stands over it
			if (strip > 0.5F && cliff >= MIN_BEACH_CLIFF && (cell.height - this.levels.water) * this.levels.worldHeight <= BEACH_HEIGHT + 0.5F) {
				cell.terrain = TerrainType.SHINGLE_BEACH;
				cell.erosionMask = true;
			}
		} else if (cell.height > this.levels.water(-SHELF_DEPTH)) {
			// a shelf of rock at the foot of a cliff, or the beach shelving into the sea
			float shelf = NoiseUtil.lerp(this.levels.water(-SHELF_DEPTH), this.levels.water(-1), beach);
			cell.height = Math.min(cell.height, NoiseUtil.lerp(cell.height, shelf, mask * proximity));
		}
	}

	// 1 near open sea, falling to 0 about 70 blocks from it; scored at the corners of a 32 block grid and blended
	// between them, so it's smooth
	private float seaProximity(float x, float z, Heightmap heightmap) {
		float gx = x / SEA_GRID;
		float gz = z / SEA_GRID;
		int x0 = NoiseUtil.floor(gx);
		int z0 = NoiseUtil.floor(gz);
		float tx = gx - x0;
		float tz = gz - z0;
		float top = NoiseUtil.lerp(this.cornerProximity(x0, z0, heightmap), this.cornerProximity(x0 + 1, z0, heightmap), tx);
		float bottom = NoiseUtil.lerp(this.cornerProximity(x0, z0 + 1, heightmap), this.cornerProximity(x0 + 1, z0 + 1, heightmap), tx);
		return NoiseUtil.lerp(top, bottom, tz);
	}

	private float cornerProximity(int gridX, int gridZ, Heightmap heightmap) {
		Long2FloatOpenHashMap cache = this.nearSea.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (cache.containsKey(key)) {
			return cache.get(key);
		}
		if (cache.size() > 16384) {
			cache.clear();
		}
		float cornerX = gridX * SEA_GRID;
		float cornerZ = gridZ * SEA_GRID;
		float proximity = 0.0F;
		search:
		for (int r = 0; r < SEA_SEARCH_RADII.length; r++) {
			float radius = SEA_SEARCH_RADII[r];
			for (int i = 0; i < (radius == 0.0F ? 1 : 8); i++) {
				double angle = i * Math.PI / 4.0D;
				Cell sample = heightmap.sampleGround(cornerX + (float) Math.cos(angle) * radius, cornerZ + (float) Math.sin(angle) * radius);
				if (sample.height < this.levels.water && sample.continentEdge < this.shoreline) {
					proximity = 1.0F - r / (float) SEA_SEARCH_RADII.length;
					break search;
				}
			}
		}
		cache.put(key, proximity);
		return proximity;
	}

	// 1 along the stretches of coast with cliffs, 0 elsewhere
	float cliffMask(float x, float z) {
		float value = this.coastline.compute(x, z, 0);
		return Landform.smoothstep(value, this.threshold - 0.02F, this.threshold + 0.02F);
	}

	private void applyStacks(Cell cell, float x, float z, Heightmap heightmap) {
		int gridX = NoiseUtil.floor(x / STACK_GRID);
		int gridZ = NoiseUtil.floor(z / STACK_GRID);
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				this.applyStack(cell, x, z, gridX + dx, gridZ + dz, heightmap);
			}
		}
	}

	private void applyStack(Cell cell, float x, float z, int gridX, int gridZ, Heightmap heightmap) {
		if (random(this.seed, gridX, gridZ, 0) >= STACK_CHANCE) {
			return;
		}
		float radius = NoiseUtil.lerp(3.0F, 8.0F, random(this.seed, gridX, gridZ, 1));
		float margin = radius + 3.0F;
		float free = STACK_GRID - margin * 2.0F;
		float centerX = gridX * STACK_GRID + margin + random(this.seed, gridX, gridZ, 2) * free;
		float centerZ = gridZ * STACK_GRID + margin + random(this.seed, gridX, gridZ, 3) * free;
		float dx = x - centerX;
		float dz = z - centerZ;
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		if (distance > radius + 2.0F) {
			return;
		}
		float top = this.stackTop(gridX, gridZ, centerX, centerZ, heightmap);
		if (top == ABSENT) {
			return;
		}
		// a slightly tapering pillar with a rounded top
		float profile = distance <= radius ? 1.0F : 1.0F - (distance - radius) / 2.0F;
		float crown = 1.0F - 0.15F * (distance / radius) * (distance / radius);
		float height = this.levels.water + (top - this.levels.water) * profile * crown;
		if (height > cell.height) {
			cell.height = height;
			cell.erosionMask = true;
		}
	}

	// the top of a sea stack, or ABSENT if its site isn't offshore of a cliff; remembered for the cells around it
	private float stackTop(int gridX, int gridZ, float centerX, float centerZ, Heightmap heightmap) {
		Long2FloatOpenHashMap sites = this.stackSites.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (sites.containsKey(key)) {
			return sites.get(key);
		}
		if (sites.size() > 4096) {
			sites.clear();
		}
		Cell center = heightmap.sampleTerrain(centerX, centerZ);
		boolean stands = center.height < this.levels.water(-1) && center.continentEdge < this.shoreline && center.riverDistance > 0.5F && this.cliffMask(centerX, centerZ) > 0.9F && this.landNearby(centerX, centerZ, heightmap);
		float top = ABSENT;
		if (stands) {
			float cliff = this.height * this.heightVariation.compute(centerX, centerZ, 0) * this.levels.unit;
			top = this.levels.water + cliff * NoiseUtil.lerp(0.5F, 1.0F, random(this.seed, gridX, gridZ, 4));
		}
		sites.put(key, top);
		return top;
	}

	// stacks stand just off the coast, not out in open water
	private boolean landNearby(float x, float z, Heightmap heightmap) {
		for (float radius : STACK_LAND_SEARCH) {
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0D;
				Cell sample = heightmap.sampleGround(x + (float) Math.cos(angle) * radius, z + (float) Math.sin(angle) * radius);
				if (sample.height >= this.levels.water && !sample.terrain.isRiver()) {
					return true;
				}
			}
		}
		return false;
	}

	private static float random(int seed, int gridX, int gridZ, int purpose) {
		return (NoiseUtil.valCoord2D(seed + purpose * 1013, gridX, gridZ) + 1.0F) * 0.5F;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new SeaCliffs(this.seed, this.shoreline, this.threshold, this.height, this.stacks, this.coastline.mapAll(visitor), this.heightVariation.mapAll(visitor), this.beaches.mapAll(visitor), this.beachThreshold, this.levels, this.stackSites, this.nearSea);
	}
}
