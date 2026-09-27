package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import com.pandaismyname1.ultraterraforged.data.preset.settings.LandformSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.type.BiomeType;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/**
 * Salt flats: dead flat basins of pale salt crust on the low, gentle ground of hot deserts, where a lake once dried up.
 * Each lies in its own cell of a grid, levelled at the lowest ground around it, so it sits in a shallow basin, with its
 * edge blending back into the desert.
 *
 * @param edge wobbles the edge, so the flats aren't round
 */
public record SaltFlats(int seed, float coverage, Noise edge, Levels levels, SiteCache<Site> sites) implements Landform {
	private static final float GRID = 440.0F;
	private static final float MIN_RADIUS = 50.0F;
	private static final float MAX_RADIUS = 115.0F;
	// the flat blends back into the land around it over this many blocks
	private static final float BLEND = 36.0F;
	// the site's centre stays this far inside its grid cell, so the whole flat does too
	private static final float MARGIN = MAX_RADIUS * 1.2F + BLEND;
	// the ground over the flat may vary no more than this many blocks
	private static final float MAX_RELIEF = 16.0F;
	// and it lies at least this many blocks above the sea, away from the coast
	private static final int MIN_HEIGHT = 4;
	private static final Site ABSENT = new Site(0.0F, 0.0F, 0.0F, 0.0F);

	// where a salt flat lies, how big it is, and the height it's levelled at
	record Site(float x, float z, float radius, float level) {
	}

	public static SaltFlats make(int seed, LandformSettings.SaltFlats settings, Levels levels) {
		return new SaltFlats(seed, NoiseUtil.clamp(settings.coverage, 0.0F, 1.0F), Noises.perlin(seed + 1, 45, 2), levels, SiteCache.make());
	}

	@Override
	public void apply(Cell cell, float x, float z, Heightmap heightmap) {
		if (!Landform.isRollingLand(cell.terrain) || cell.terrain == TerrainType.DUNES || cell.riverDistance < 0.3F) {
			return;
		}
		int gridX = NoiseUtil.floor(x / GRID);
		int gridZ = NoiseUtil.floor(z / GRID);
		if (Landform.random(this.seed, gridX, gridZ, 0) >= this.coverage) {
			return;
		}
		Site site = this.sites.get(gridX, gridZ, (gx, gz) -> this.find(gx, gz, heightmap));
		if (site == ABSENT) {
			return;
		}
		float dx = x - site.x();
		float dz = z - site.z();
		float reach = site.radius() * 1.2F + BLEND;
		if (dx * dx + dz * dz > reach * reach) {
			return;
		}
		float distance = (float) Math.sqrt(dx * dx + dz * dz) + (this.edge.compute(x, z, 0) - 0.5F) * site.radius() * 0.4F;
		float blend = Landform.smoothstep(distance, site.radius(), site.radius() + BLEND);
		if (blend >= 1.0F) {
			return;
		}
		cell.height = NoiseUtil.lerp(site.level(), cell.height, blend);
		if (blend <= 0.0F) {
			cell.terrain = TerrainType.SALT_FLAT;
			// the filters would roughen the crust
			cell.erosionMask = true;
		}
	}

	private Site find(int gridX, int gridZ, Heightmap heightmap) {
		float free = GRID - MARGIN * 2.0F;
		float x = gridX * GRID + MARGIN + Landform.random(this.seed, gridX, gridZ, 1) * free;
		float z = gridZ * GRID + MARGIN + Landform.random(this.seed, gridX, gridZ, 2) * free;
		float radius = NoiseUtil.lerp(MIN_RADIUS, MAX_RADIUS, Landform.random(this.seed, gridX, gridZ, 3));
		Cell center = heightmap.sampleTerrain(x, z);
		// (by the sea too, where the climate marks the land as coast)
		if (center.biomeType != BiomeType.DESERT || !(Landform.isRollingLand(center.terrain) || center.terrain == TerrainType.COAST) || center.riverDistance < 0.5F) {
			return ABSENT;
		}
		// gentle ground: the flat is levelled at its lowest point, so it sinks into a shallow basin
		float lowest = center.height;
		float highest = center.height;
		for (float ring : new float[] { 0.5F, 1.0F }) {
			for (int i = 0; i < 8; i++) {
				double angle = (i + ring) * Math.PI / 4.0D;
				Cell ground = heightmap.sampleGround(x + (float) Math.cos(angle) * radius * ring, z + (float) Math.sin(angle) * radius * ring);
				if (!Landform.isRollingLand(ground.terrain)) {
					return ABSENT;
				}
				lowest = Math.min(lowest, ground.height);
				highest = Math.max(highest, ground.height);
			}
		}
		if ((highest - lowest) * this.levels.worldHeight > MAX_RELIEF || lowest < this.levels.water(MIN_HEIGHT)) {
			return ABSENT;
		}
		// the middle of a block, so the whole flat is one block level
		float level = (NoiseUtil.floor(lowest * this.levels.worldHeight) + 0.5F) / this.levels.worldHeight;
		return new Site(x, z, radius, level);
	}

	@Override
	public Landform mapNoise(Noise.Visitor visitor) {
		return new SaltFlats(this.seed, this.coverage, this.edge.mapAll(visitor), this.levels, this.sites);
	}
}
