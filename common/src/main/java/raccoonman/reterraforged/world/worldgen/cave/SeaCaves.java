package raccoonman.reterraforged.world.worldgen.cave;

import raccoonman.reterraforged.world.worldgen.landform.SiteCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

/**
 * Sea caves and arches: caves the waves have cut into sea cliffs at the waterline, their floor flooded by the sea; where
 * a headland is narrow enough, right through it, leaving an arch. Each stands in its own cell of a fine grid, wherever
 * that cell holds a cliff by the sea.
 */
final class SeaCaves implements CaveFeatures.Feature {
	private static final float GRID = 40.0F;
	// the cliff stands at least this high over the sea
	private static final int CLIFF = 7;
	// the sea is looked for this far from the cliff top, and the far side of a headland this far
	private static final int SEA_SEARCH = 14;
	private static final int THROUGH_SEARCH = 26;
	private static final Cave NONE = new Cave(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	private final int seed;
	private final float frequency;
	private final SiteCache<Cave> caves = SiteCache.make();

	// from out in the sea to its far end, and its half width
	record Cave(float fromX, float fromZ, float toX, float toZ, float radius) {
	}

	SeaCaves(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		int gridX = NoiseUtil.floor(carving.minX / GRID);
		int gridZ = NoiseUtil.floor(carving.minZ / GRID);
		int sea = carving.levels.waterY;
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (CaveFeatures.random(this.seed, gx, gz, 0) >= this.frequency * 0.6F) {
					continue;
				}
				Cave cave = this.caves.get(gx, gz, (cx, cz) -> this.find(cx, cz, carving));
				if (cave == NONE) {
					continue;
				}
				float reach = cave.radius() + 1.0F;
				if (!carving.reaches(Math.min(cave.fromX(), cave.toX()) - reach, Math.min(cave.fromZ(), cave.toZ()) - reach, Math.max(cave.fromX(), cave.toX()) + reach, Math.max(cave.fromZ(), cave.toZ()) + reach)) {
					continue;
				}
				// its floor under the sea and its roof a few blocks over it
				CaveFeatures.along(new float[] { cave.fromX(), cave.toX() }, new float[] { sea + 1.5F, sea + 1.0F }, new float[] { cave.fromZ(), cave.toZ() }, 1.0F,
					(x, y, z, index) -> carving.hollow(x, y, z, cave.radius(), 3.8F, cave.radius(), sea, 2));
			}
		}
	}

	Cave find(int gridX, int gridZ, CaveCarving carving) {
		float x = (gridX + CaveFeatures.random(this.seed, gridX, gridZ, 1)) * GRID;
		float z = (gridZ + CaveFeatures.random(this.seed, gridX, gridZ, 2)) * GRID;
		int sea = carving.levels.waterY;
		int ground = carving.terrainHeight(x, z);
		if (ground < sea + CLIFF || ground > sea + 60) {
			return NONE;
		}
		// the nearest sea
		float dirX = 0.0F;
		float dirZ = 0.0F;
		int nearest = Integer.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			float angle = i * NoiseUtil.PI2 / 8.0F;
			float cos = NoiseUtil.cos(angle);
			float sin = NoiseUtil.sin(angle);
			for (int d = 3; d <= SEA_SEARCH && d < nearest; d += 2) {
				if (carving.terrainHeight(x + cos * d, z + sin * d) < sea) {
					nearest = d;
					dirX = cos;
					dirZ = sin;
					break;
				}
			}
		}
		if (nearest == Integer.MAX_VALUE) {
			return NONE;
		}
		// in from the sea, and right through the headland if the sea lies close behind it
		float length = NoiseUtil.lerp(12.0F, 26.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3));
		for (int d = 4; d <= THROUGH_SEARCH; d += 2) {
			if (carving.terrainHeight(x - dirX * d, z - dirZ * d) < sea) {
				length = d + 6.0F;
				break;
			}
		}
		float radius = NoiseUtil.lerp(2.5F, 4.5F, CaveFeatures.random(this.seed, gridX, gridZ, 4));
		return new Cave(x + dirX * (nearest + 4), z + dirZ * (nearest + 4), x - dirX * length, z - dirZ * length, radius);
	}
}
