package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.type.BiomeType;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainCategory;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Mountains shaped by old glaciers, in cold climates. The glaciers wore their valleys from a V into a U: broad flat
 * floors between steep walls, with the peaks and ridges above left as they were. High on the slopes under the peaks,
 * they scooped out cirques: round bowls with a steep back wall, holding a small lake that spills over the bowl's lip.
 *
 * The valleys are found from the lowest and highest ground around, measured on a coarse grid; ground between them is
 * lowered towards the valley floor, the more the nearer to it, so the floor widens and the walls steepen.
 */
public record GlacialValleys(int seed, float power, float cirques, Levels levels, ClimateGrid climate, SampleGrid floor, SampleGrid tops, SiteCache<Cirque> sites) implements Landform {
	private static final float GRID = 64.0F;
	// how far around a grid corner the lowest and highest ground are looked for
	private static final float[] REACH = { 56.0F, 112.0F };
	// valleys less deep than this, in blocks, are left alone
	private static final float MIN_RELIEF = 24.0F;
	// nor valleys whose floor lies less than this many blocks above the sea
	private static final float MIN_FLOOR = 16.0F;
	// the floor of a U-shaped valley: ground within this share of the way from the floor to the tops
	private static final float FLOOR = 0.12F;
	private static final float CIRQUE_GRID = 176.0F;
	private static final float MIN_CIRQUE_RADIUS = 20.0F;
	private static final float MAX_CIRQUE_RADIUS = 34.0F;
	// cirques lie at least this many blocks above the sea
	private static final float CIRQUE_HEIGHT = 70.0F;
	// the bowl's floor lies this many blocks below the ground at its middle, and its lake this many above the floor
	private static final float CIRQUE_DEPTH = 7.0F;
	private static final float TARN_DEPTH = 3.0F;
	private static final Cirque ABSENT = new Cirque(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	/**
	 * @param dirX the way the bowl opens, downhill
	 * @param floor the height of the bowl's floor
	 */
	record Cirque(float x, float z, float dirX, float dirZ, float radius, float floor) {
	}

	public static GlacialValleys make(int seed, LandformSettings.GlacialValleys settings, Levels levels) {
		// the stronger, the flatter the floors
		float power = 1.0F + Math.max(0.0F, settings.strength) * 1.2F;
		return new GlacialValleys(seed, power, NoiseUtil.clamp(settings.cirques, 0.0F, 1.0F), levels, ClimateGrid.of(128.0F, BiomeType.TUNDRA, BiomeType.TAIGA, BiomeType.COLD_STEPPE, BiomeType.ALPINE), SampleGrid.of(GRID), SampleGrid.of(GRID), SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (cell.terrain.getCategory() != TerrainCategory.HIGHLAND || cell.terrain.isVolcano() || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return;
		}
		// rivers keep the valleys they cut, and their water
		float mask = Landform.smoothstep(cell.riverDistance, 0.35F, 0.65F);
		if (mask <= 0.0F) {
			return;
		}
		mask *= Landform.smoothstep(this.climate.get(x, z, heightmap), 0.4F, 0.9F);
		if (mask <= 0.0F) {
			return;
		}
		float floor = this.floor.get(x, z, (sx, sz) -> this.lowest(sx, sz, heightmap));
		// only valleys well above the sea: by the coast they'd be worn down into the sea
		mask *= Landform.smoothstep((floor - this.levels.water) * this.levels.worldHeight, MIN_FLOOR, MIN_FLOOR * 2.0F);
		if (mask <= 0.0F) {
			return;
		}
		float tops = this.tops.get(x, z, (sx, sz) -> this.highest(sx, sz, heightmap));
		float relief = tops - floor;
		if (relief * this.levels.worldHeight > MIN_RELIEF && cell.height > floor) {
			float t = Math.min(1.0F, (cell.height - floor) / relief);
			float worn = floor + relief * (float) Math.pow(t, this.power);
			// fading in with deeper valleys
			mask *= Landform.smoothstep(relief * this.levels.worldHeight, MIN_RELIEF, MIN_RELIEF * 2.0F);
			float height = NoiseUtil.lerp(cell.height, worn, mask);
			// never down to the sea
			if (height < cell.height) {
				cell.height = Math.max(height, Math.min(cell.height, this.levels.water(3)));
				if (t < FLOOR && mask > 0.5F) {
					cell.terrain = TerrainType.GLACIAL_VALLEY;
				}
			}
		}
		if (this.cirques > 0.0F) {
			this.cirque(cell, x, z, heightmap);
		}
	}

	// the lowest ground around; the sea, if it's near, making it too low to wear down
	private float lowest(float x, float z, Heightmap heightmap) {
		float lowest = heightmap.sampleGround(x, z).height;
		for (float reach : REACH) {
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0D;
				lowest = Math.min(lowest, heightmap.sampleGround(x + (float) Math.cos(angle) * reach, z + (float) Math.sin(angle) * reach).height);
			}
		}
		return lowest;
	}

	private float highest(float x, float z, Heightmap heightmap) {
		float highest = heightmap.sampleGround(x, z).height;
		for (float reach : REACH) {
			for (int i = 0; i < 8; i++) {
				double angle = (i + 0.5D) * Math.PI / 4.0D;
				highest = Math.max(highest, heightmap.sampleGround(x + (float) Math.cos(angle) * reach, z + (float) Math.sin(angle) * reach).height);
			}
		}
		return highest;
	}

	private void cirque(Cell cell, float x, float z, Heightmap heightmap) {
		int gridX = NoiseUtil.floor(x / CIRQUE_GRID);
		int gridZ = NoiseUtil.floor(z / CIRQUE_GRID);
		if (Landform.random(this.seed, gridX, gridZ, 0) >= this.cirques) {
			return;
		}
		Cirque cirque = this.sites.get(gridX, gridZ, (gx, gz) -> this.findCirque(gx, gz, heightmap));
		if (cirque == ABSENT) {
			return;
		}
		float dx = x - cirque.x();
		float dz = z - cirque.z();
		float distance = (float) Math.sqrt(dx * dx + dz * dz) / cirque.radius();
		if (distance >= 1.0F) {
			return;
		}
		// a flat floor, then the back wall rising steeply to the ground around
		float wall = Landform.smoothstep(distance, 0.5F, 1.0F);
		wall *= wall;
		float bowl = NoiseUtil.lerp(cirque.floor(), cell.height, wall);
		// the whole bowl, so its lip isn't built up to hold the lake in: where the ground outside is lower than the
		// lake, it spills over, down the mountain
		cell.terrain = TerrainType.CIRQUE;
		cell.erosionMask = true;
		if (bowl < cell.height) {
			cell.height = bowl;
			// the lake on the floor
			float lake = cirque.floor() + TARN_DEPTH * this.levels.unit;
			if (distance < 0.6F && cell.height < lake) {
				cell.waterLevel = cell.waterLevel > 0.0F ? Math.min(cell.waterLevel, lake) : lake;
			}
		}
	}

	private Cirque findCirque(int gridX, int gridZ, Heightmap heightmap) {
		float margin = MAX_CIRQUE_RADIUS + 8.0F;
		float free = CIRQUE_GRID - margin * 2.0F;
		float x = gridX * CIRQUE_GRID + margin + Landform.random(this.seed, gridX, gridZ, 1) * free;
		float z = gridZ * CIRQUE_GRID + margin + Landform.random(this.seed, gridX, gridZ, 2) * free;
		Cell center = heightmap.sampleTerrain(x, z);
		if (center.terrain.getCategory() != TerrainCategory.HIGHLAND || center.terrain.isVolcano() || center.riverDistance < 0.5F || center.height < this.levels.water((int) CIRQUE_HEIGHT)) {
			return ABSENT;
		}
		if (this.climate.get(x, z, heightmap) < 0.9F) {
			return ABSENT;
		}
		float radius = NoiseUtil.lerp(MIN_CIRQUE_RADIUS, MAX_CIRQUE_RADIUS, Landform.random(this.seed, gridX, gridZ, 3));
		// on a slope below a peak: the ground rises well on one side and falls on the other
		float bestRise = 0.0F;
		float upX = 0.0F;
		float upZ = 0.0F;
		float lowest = Float.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0D;
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			float height = heightmap.sampleGround(x + cos * radius, z + sin * radius).height;
			if (height - center.height > bestRise) {
				bestRise = height - center.height;
				upX = cos;
				upZ = sin;
			}
			lowest = Math.min(lowest, height);
		}
		if (bestRise * this.levels.worldHeight < 10.0F || (center.height - lowest) * this.levels.worldHeight < 6.0F) {
			return ABSENT;
		}
		float floor = center.height - CIRQUE_DEPTH * this.levels.unit;
		return new Cirque(x, z, -upX, -upZ, radius, floor);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return this;
	}
}
