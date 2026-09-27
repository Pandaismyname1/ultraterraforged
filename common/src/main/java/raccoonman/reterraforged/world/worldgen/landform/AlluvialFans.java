package raccoonman.reterraforged.world.worldgen.landform;

import java.util.Set;

import raccoonman.reterraforged.data.preset.settings.LandformSettings;
import raccoonman.reterraforged.world.worldgen.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Alluvial fans: cones of gravel and sand spread out where steep mountain slopes meet flat land, left by floods washing
 * rock down from the mountain. Each fan's apex sits at the foot of the slope and it spreads downhill over the plain in
 * a gently curving cone, highest at the apex and meeting the plain at its toe. They're most common in dry climates,
 * where little grows to hold the rock back.
 */
public record AlluvialFans(int seed, float density, float size, Levels levels, SiteCache<Fan> fans) implements Landform {
	private static final float GRID = 256.0F;
	private static final float MIN_RADIUS = 80.0F;
	private static final float MAX_RADIUS = 150.0F;
	// the uphill side is looked for this far from the spot
	private static final float SEARCH = 56.0F;
	private static final float STEP = 8.0F;
	// a mountain slope rises at least this many blocks over the search distance
	private static final float MIN_RISE = 18.0F;
	// and the apex is where the ground starts rising this many blocks per step
	private static final float APEX_RISE = 3.0F;
	// the apex stands this many blocks above the ground at the foot of the slope
	private static final float APEX_HEIGHT = 6.0F;
	// the cone spreads this far to either side of straight downhill, as the cosine of the angle: fully up to the first,
	// fading out by the second
	private static final float CORE_COS = 0.8F;
	private static final float EDGE_COS = 0.45F;
	private static final Set<BiomeType> DRY = Set.of(BiomeType.DESERT, BiomeType.SAVANNA, BiomeType.STEPPE, BiomeType.COLD_STEPPE);
	// the share of the usual density that wetter climates get
	private static final float WET_SHARE = 0.25F;
	private static final Fan ABSENT = new Fan(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	/**
	 * @param dirX the way downhill
	 */
	record Fan(float x, float z, float dirX, float dirZ, float radius, float apex, float toe) {
	}

	public static AlluvialFans make(int seed, LandformSettings.AlluvialFans settings, Levels levels) {
		return new AlluvialFans(seed, NoiseUtil.clamp(settings.density, 0.0F, 1.0F), Math.max(0.1F, settings.size), levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.terrain.isSubmerged() || cell.terrain.isRiver() || cell.terrain.isLake() || cell.terrain.isWetland() || cell.terrain.isVolcano() || cell.riverDistance < 0.25F) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		int range = (int) Math.ceil((MAX_RADIUS * this.size + SEARCH) / GRID);
		// how much rock the fans have laid down here
		float deposit = 0.0F;
		for (int gx = gridX - range; gx <= gridX + range; gx++) {
			for (int gz = gridZ - range; gz <= gridZ + range; gz++) {
				if (Landform.random(this.seed, gx, gz, 0) >= this.density) {
					continue;
				}
				Fan fan = this.fans.get(gx, gz, (fx, fz) -> this.find(fx, fz, heightmap));
				if (fan != ABSENT) {
					deposit = Math.max(deposit, deposit(fan, x, z, cell.height));
				}
			}
		}
		if (deposit <= 0.0F) {
			return;
		}
		// thinning out onto rivers' valleys
		deposit *= Landform.smoothstep(cell.riverDistance, 0.25F, 0.5F);
		if (deposit > this.levels.unit) {
			cell.terrain = TerrainType.ALLUVIAL_FAN;
		}
		cell.height += deposit;
		cell.erosionMask = true;
	}

	// how far the fan's surface stands above the ground here: a cone, highest at the apex, curving down to the toe
	// and falling away to the sides, thinning out to nothing at its edges wherever the ground there lies lower
	private static float deposit(Fan fan, float x, float z, float ground) {
		float dx = x - fan.x();
		float dz = z - fan.z();
		float along = dx * fan.dirX() + dz * fan.dirZ();
		if (along <= 0.0F) {
			return 0.0F;
		}
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		if (distance >= fan.radius()) {
			return 0.0F;
		}
		float sides = Landform.smoothstep(along / distance, EDGE_COS, CORE_COS);
		float reach = 1.0F - distance / fan.radius();
		float profile = (float) Math.pow(reach, 1.7F);
		float surface = fan.toe() + (fan.apex() - fan.toe()) * profile * (float) Math.sqrt(sides);
		float edges = Landform.smoothstep(sides, 0.0F, 0.4F) * Landform.smoothstep(reach, 0.0F, 0.25F) * Landform.smoothstep(along, 0.0F, 40.0F);
		return Math.max(0.0F, surface - ground) * edges;
	}

	private Fan find(int gridX, int gridZ, Heightmap heightmap) {
		float margin = 32.0F;
		float free = GRID - margin * 2.0F;
		float x = gridX * GRID + margin + Landform.random(this.seed, gridX, gridZ, 1) * free;
		float z = gridZ * GRID + margin + Landform.random(this.seed, gridX, gridZ, 2) * free;
		Cell spot = heightmap.sampleTerrain(x, z);
		// wetter climates get fewer
		if (!DRY.contains(spot.biomeType) && Landform.random(this.seed, gridX, gridZ, 3) >= WET_SHARE) {
			return ABSENT;
		}
		// on the plain
		if (!Landform.isRollingLand(spot.terrain) || spot.riverDistance < 0.4F || spot.height < this.levels.water(3)) {
			return ABSENT;
		}
		float ground = spot.height;
		// the way up the mountain: the steepest rise around
		float bestRise = 0.0F;
		float upX = 0.0F;
		float upZ = 0.0F;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0D;
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			float rise = heightmap.sampleGround(x + cos * SEARCH, z + sin * SEARCH).height - ground;
			if (rise > bestRise) {
				bestRise = rise;
				upX = cos;
				upZ = sin;
			}
		}
		if (bestRise * this.levels.worldHeight < MIN_RISE) {
			return ABSENT;
		}
		// and the plain carrying on the other way
		float behind = heightmap.sampleGround(x - upX * SEARCH, z - upZ * SEARCH).height - ground;
		if (behind * this.levels.worldHeight > 4.0F || behind * this.levels.worldHeight < -12.0F) {
			return ABSENT;
		}
		// the apex, where the slope starts
		float apexX = x;
		float apexZ = z;
		float apexGround = ground;
		for (float d = STEP; d <= SEARCH; d += STEP) {
			float height = heightmap.sampleGround(x + upX * d, z + upZ * d).height;
			if ((height - apexGround) * this.levels.worldHeight > APEX_RISE) {
				break;
			}
			apexX = x + upX * d;
			apexZ = z + upZ * d;
			apexGround = height;
		}
		float radius = NoiseUtil.lerp(MIN_RADIUS, MAX_RADIUS, Landform.random(this.seed, gridX, gridZ, 4)) * this.size;
		float toe = Math.min(ground, heightmap.sampleGround(apexX - upX * radius, apexZ - upZ * radius).height);
		// reaching a little way up into the valley it came from
		float apex = Math.max(apexGround + APEX_HEIGHT * this.levels.unit, heightmap.sampleGround(apexX + upX * 28.0F, apexZ + upZ * 28.0F).height);
		if ((apex - toe) * this.levels.worldHeight < 6.0F || toe < this.levels.water(2)) {
			return ABSENT;
		}
		return new Fan(apexX, apexZ, -upX, -upZ, radius, apex, toe);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return this;
	}
}
