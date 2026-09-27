package raccoonman.reterraforged.world.worldgen.cave;

import raccoonman.reterraforged.world.worldgen.landform.SiteCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

/**
 * Giant caverns: rare, huge chambers deep underground, with ragged walls, stone pillars standing from floor to roof,
 * and a lake across the lowest part of their floor. Each lies in its own cell of a coarse grid.
 */
final class GiantCaverns implements CaveFeatures.Feature {
	private static final float GRID = 900.0F;
	// the roof stays at least this far below the lowest ground over it
	private static final int ROOF = 25;
	private static final Cavern NONE = new Cavern(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	private final int seed;
	private final float frequency;
	private final Noise walls;
	private final Noise pillars;
	private final SiteCache<Cavern> caverns = SiteCache.make();

	record Cavern(float x, float y, float z, float radiusX, float radiusY, float radiusZ) {
	}

	GiantCaverns(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
		this.walls = Noises.perlin(seed + 1, 18, 2);
		this.pillars = Noises.worleyEdge(seed + 2, 24);
	}

	@Override
	public void carve(CaveCarving carving) {
		int gridX = NoiseUtil.floor(carving.minX / GRID);
		int gridZ = NoiseUtil.floor(carving.minZ / GRID);
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (CaveFeatures.random(this.seed, gx, gz, 0) >= this.frequency * 0.35F) {
					continue;
				}
				Cavern cavern = this.caverns.get(gx, gz, (cx, cz) -> this.find(cx, cz, carving));
				if (cavern == NONE) {
					continue;
				}
				float reach = Math.max(cavern.radiusX(), cavern.radiusZ()) * 1.3F;
				if (carving.reaches(cavern.x() - reach, cavern.z() - reach, cavern.x() + reach, cavern.z() + reach)) {
					this.hollow(cavern, carving);
				}
			}
		}
	}

	private void hollow(Cavern cavern, CaveCarving carving) {
		int lake = (int) (cavern.y() - cavern.radiusY() * 0.55F);
		int y0 = Math.max(carving.bottom + 2, (int) (cavern.y() - cavern.radiusY() * 1.3F));
		int y1 = (int) (cavern.y() + cavern.radiusY() * 1.3F);
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				int x = carving.minX + dx;
				int z = carving.minZ + dz;
				float nx = (x - cavern.x()) / cavern.radiusX();
				float nz = (z - cavern.z()) / cavern.radiusZ();
				float flat = nx * nx + nz * nz;
				if (flat > 1.7F) {
					continue;
				}
				// pillars from floor to roof, where the cells of the pillar noise meet
				boolean pillar = this.pillars.compute(x, z, 0) < 0.12F && flat < 0.7F;
				for (int y = y0; y <= y1; y++) {
					float ny = (y - cavern.y()) / cavern.radiusY();
					// ragged walls: the noise, sampled along a slanted plane, stands in for a 3D one
					float ragged = (this.walls.compute(x + y * 0.7F, z - y * 0.7F, 0) - 0.5F) * 0.7F;
					if (flat + ny * ny + ragged >= 1.0F) {
						continue;
					}
					if (pillar && y > lake) {
						continue;
					}
					carving.clear(x, y, z, lake);
				}
			}
		}
	}

	Cavern find(int gridX, int gridZ, CaveCarving carving) {
		float margin = 160.0F;
		float x = gridX * GRID + margin + CaveFeatures.random(this.seed, gridX, gridZ, 1) * (GRID - margin * 2.0F);
		float z = gridZ * GRID + margin + CaveFeatures.random(this.seed, gridX, gridZ, 2) * (GRID - margin * 2.0F);
		float radiusX = NoiseUtil.lerp(45.0F, 75.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3));
		float radiusZ = NoiseUtil.lerp(40.0F, 70.0F, CaveFeatures.random(this.seed, gridX, gridZ, 4));
		float radiusY = NoiseUtil.lerp(20.0F, 32.0F, CaveFeatures.random(this.seed, gridX, gridZ, 5));
		int lowest = Integer.MAX_VALUE;
		for (int i = 0; i < 9; i++) {
			float angle = i * NoiseUtil.PI2 / 8.0F;
			float r = i == 8 ? 0.0F : 1.2F;
			lowest = Math.min(lowest, carving.terrainHeight(x + NoiseUtil.cos(angle) * radiusX * r, z + NoiseUtil.sin(angle) * radiusZ * r));
		}
		float y = Math.min(lowest - ROOF - radiusY * 1.3F, carving.levels.waterY - 20);
		if (y - radiusY * 1.3F < carving.bottom + 6) {
			return NONE;
		}
		return new Cavern(x, y, z, radiusX, radiusY, radiusZ);
	}
}
