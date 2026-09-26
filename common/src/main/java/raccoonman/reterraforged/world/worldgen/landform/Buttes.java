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
import raccoonman.reterraforged.world.worldgen.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Flat-topped towers of rock with sheer cliffs and a skirt of fallen rubble, from narrow buttes to broad mesas, in
 * badlands and plateau terrain. Each sits in its own cell of a grid, so neighbours never merge into one lump.
 */
public record Buttes(int seed, float density, float height, float gridSize, Noise warpX, Noise warpZ, Levels levels, ThreadLocal<Long2FloatOpenHashMap> tops) implements Landform {
	// a butte site that turned out to be in the wrong terrain
	private static final float ABSENT = -1.0F;
	// the cliff drops over this many blocks, then rubble slopes out to this share of the radius
	private static final float CLIFF_WIDTH = 3.0F;
	private static final float RUBBLE_WIDTH = 0.55F;
	// the rubble is piled this high up the cliff, as a share of the butte's height
	private static final float RUBBLE_HEIGHT = 0.3F;
	private static final float MIN_RADIUS = 10.0F;
	// broad mesas may carry a smaller second tier
	private static final float TIER_MIN_RADIUS = 28.0F;
	private static final float TIER_CLIFF_WIDTH = 2.0F;

	public static Buttes make(int seed, LandformSettings.Buttes settings, Levels levels) {
		float gridSize = 170.0F * settings.size;
		// ragged outlines, rather than perfect ovals
		Noise warpX = Noises.perlin(seed + 1, 40, 3);
		Noise warpZ = Noises.perlin(seed + 2, 40, 3);
		return new Buttes(seed, settings.density, settings.height, gridSize, warpX, warpZ, levels, ThreadLocal.withInitial(Long2FloatOpenHashMap::new));
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		// rivers keep flowing where they cut through a butte's rubble
		float mask = Landform.smoothstep(cell.riverDistance, 0.1F, 0.35F);
		if (mask <= 0.0F || cell.terrain.isSubmerged()) {
			return;
		}
		int gridX = NoiseUtil.floor(x / this.gridSize);
		int gridZ = NoiseUtil.floor(z / this.gridSize);
		// the outline wobble, scaled to each butte's size below
		float warpX = this.warpX.compute(x, z, 0);
		float warpZ = this.warpZ.compute(x, z, 0);
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				this.applyButte(cell, x, z, warpX, warpZ, gridX + dx, gridZ + dz, mask, heightmap);
			}
		}
	}

	private void applyButte(Cell cell, float x, float z, float warpX, float warpZ, int gridX, int gridZ, float mask, Heightmap heightmap) {
		if (random(this.seed, gridX, gridZ, 0) >= this.density) {
			return;
		}
		// narrow buttes are more common than broad mesas
		float sizeRoll = random(this.seed, gridX, gridZ, 3);
		float radius = MIN_RADIUS + sizeRoll * sizeRoll * (this.gridSize * 0.3F - MIN_RADIUS);
		// keep the whole butte and its rubble inside its grid cell
		float margin = radius * (1.0F + RUBBLE_WIDTH) + CLIFF_WIDTH;
		float free = Math.max(0.0F, this.gridSize - margin * 2.0F);
		float centerX = gridX * this.gridSize + margin + random(this.seed, gridX, gridZ, 1) * free;
		float centerZ = gridZ * this.gridSize + margin + random(this.seed, gridX, gridZ, 2) * free;

		// an oval turned any way, drawn out up to this much longer than wide, with a ragged edge
		float wobble = radius * 0.35F + 3.0F;
		float dx = x + warpX * wobble - centerX;
		float dz = z + warpZ * wobble - centerZ;
		float angle = random(this.seed, gridX, gridZ, 5) * (float) Math.PI;
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		float along = dx * cos + dz * sin;
		float across = (dz * cos - dx * sin) * NoiseUtil.lerp(1.0F, 1.8F, random(this.seed, gridX, gridZ, 6));
		float distance = (float) Math.sqrt(along * along + across * across);
		float cliff = radius + CLIFF_WIDTH;
		float outer = cliff + radius * RUBBLE_WIDTH;
		if (distance >= outer) {
			return;
		}
		float profile;
		if (distance <= radius) {
			profile = 1.0F;
		} else if (distance <= cliff) {
			profile = NoiseUtil.lerp(1.0F, RUBBLE_HEIGHT, (distance - radius) / CLIFF_WIDTH);
		} else {
			// concave rubble slope, steep near the cliff and spreading out
			float t = 1.0F - (distance - cliff) / (outer - cliff);
			profile = RUBBLE_HEIGHT * t * t;
		}

		// whether it stands at all is decided at its centre, so a butte is always whole
		float ground = this.groundAt(gridX, gridZ, centerX, centerZ, heightmap);
		if (ground == ABSENT) {
			return;
		}
		// flat top: a fixed height above the ground at the centre
		float riseBlocks = this.height * NoiseUtil.lerp(0.45F, 1.0F, random(this.seed, gridX, gridZ, 4));
		float top = ground + riseBlocks / this.levels.worldHeight;
		// a stepped mesa: a smaller tier standing on the broad top
		if (radius >= TIER_MIN_RADIUS && random(this.seed, gridX, gridZ, 7) < 0.5F) {
			float tierRadius = radius * NoiseUtil.lerp(0.35F, 0.6F, random(this.seed, gridX, gridZ, 8));
			float tierHeight = riseBlocks * NoiseUtil.lerp(0.2F, 0.45F, random(this.seed, gridX, gridZ, 9)) / this.levels.worldHeight;
			if (distance <= tierRadius) {
				top += tierHeight;
			} else if (distance <= tierRadius + TIER_CLIFF_WIDTH) {
				top += tierHeight * (1.0F - (distance - tierRadius) / TIER_CLIFF_WIDTH);
			}
		}
		top = Math.min(top, 0.98F);
		if (top <= cell.height) {
			return;
		}
		float target = cell.height + (top - cell.height) * profile;
		float height = NoiseUtil.lerp(cell.height, target, mask);
		if (height > cell.height) {
			cell.height = height;
			if (profile > RUBBLE_HEIGHT) {
				// keep the cliffs sheer; the filters would round them off
				cell.erosionMask = true;
			}
		}
	}

	// the ground at a butte's centre, or ABSENT if it isn't badlands or plateau away from rivers; remembered because
	// every cell around it asks
	private float groundAt(int gridX, int gridZ, float centerX, float centerZ, Heightmap heightmap) {
		Long2FloatOpenHashMap tops = this.tops.get();
		long key = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
		if (tops.containsKey(key)) {
			return tops.get(key);
		}
		if (tops.size() > 4096) {
			tops.clear();
		}
		Cell center = heightmap.sampleTerrain(centerX, centerZ);
		// in badlands and plateaus, or on flat land in a desert or savanna climate
		boolean terrain = terrainMask(center.terrain) > 0.0F && center.terrainRegionEdge > 0.05F;
		// the coast type reaches far inland over low ground, so count it once it's clear of the shore
		boolean lowland = center.terrain.isFlat() || (center.terrain == TerrainType.COAST && center.height > this.levels.water(8));
		boolean arid = lowland && (center.biomeType == BiomeType.DESERT || center.biomeType == BiomeType.SAVANNA);
		boolean stands = (terrain || arid) && center.riverDistance > 0.5F && !center.terrain.isSubmerged() && center.height > this.levels.water;
		float ground = stands ? center.height : ABSENT;
		tops.put(key, ground);
		return ground;
	}

	private static float terrainMask(Terrain terrain) {
		return terrain.includes(TerrainType.BADLANDS) || terrain.includes(TerrainType.PLATEAU) ? 1.0F : 0.0F;
	}

	// 0 to 1, fixed for each grid cell and purpose
	private static float random(int seed, int gridX, int gridZ, int purpose) {
		return (NoiseUtil.valCoord2D(seed + purpose * 1013, gridX, gridZ) + 1.0F) * 0.5F;
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new Buttes(this.seed, this.density, this.height, this.gridSize, this.warpX.mapAll(visitor), this.warpZ.mapAll(visitor), this.levels, this.tops);
	}
}
