package raccoonman.reterraforged.world.worldgen.cave;

import raccoonman.reterraforged.world.worldgen.landform.SiteCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

/**
 * Underground rivers: long, winding tunnels well below the land, level along their length, with water running along
 * their floor. Each starts in its own cell of a grid and wanders off from there; one that would pass under the sea or
 * too near the surface stops short.
 */
final class UndergroundRivers implements CaveFeatures.Feature {
	private static final float GRID = 384.0F;
	private static final int NODES = 18;
	private static final float STEP = 22.0F;
	// the water lies at least this far below the lowest ground over the tunnel
	private static final int DEPTH = 22;
	// how deep the water is, and how much air there is over it
	private static final float WATER_DEPTH = 3.0F;
	private static final float HEADROOM = 4.0F;
	private static final Path NONE = new Path(new float[0], new float[0], new float[0], 0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	private final int seed;
	private final float frequency;
	private final SiteCache<Path> paths = SiteCache.make();

	// the tunnel's middle line, its water level, its half width, and its bounds
	record Path(float[] xs, float[] ys, float[] zs, int water, float radius, float minX, float minZ, float maxX, float maxZ) {
	}

	UndergroundRivers(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		int gridX = NoiseUtil.floor(carving.minX / GRID);
		int gridZ = NoiseUtil.floor(carving.minZ / GRID);
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (CaveFeatures.random(this.seed, gx, gz, 0) >= this.frequency * 0.5F) {
					continue;
				}
				Path path = this.paths.get(gx, gz, (px, pz) -> this.find(px, pz, carving));
				if (path == NONE || !carving.reaches(path.minX(), path.minZ(), path.maxX(), path.maxZ())) {
					continue;
				}
				float radius = path.radius();
				float centre = path.water() - WATER_DEPTH + (WATER_DEPTH + HEADROOM) * 0.5F;
				float half = (WATER_DEPTH + HEADROOM) * 0.5F;
				CaveFeatures.along(path.xs(), path.ys(), path.zs(), 2.0F, (x, y, z, index) -> carving.hollow(x, centre + 1.0F, z, radius, half, radius, path.water(), 6));
			}
		}
	}

	Path find(int gridX, int gridZ, CaveCarving carving) {
		float x = (gridX + CaveFeatures.random(this.seed, gridX, gridZ, 1)) * GRID;
		float z = (gridZ + CaveFeatures.random(this.seed, gridX, gridZ, 2)) * GRID;
		float heading = CaveFeatures.random(this.seed, gridX, gridZ, 3) * NoiseUtil.PI2;
		float[] xs = new float[NODES];
		float[] zs = new float[NODES];
		int lowest = Integer.MAX_VALUE;
		int count = 0;
		int sea = carving.levels.waterY;
		for (int i = 0; i < NODES; i++) {
			int ground = carving.terrainHeight(x, z);
			// not under the sea, lakes or low land by it
			if (ground < sea + 12) {
				break;
			}
			lowest = Math.min(lowest, ground);
			xs[i] = x;
			zs[i] = z;
			count++;
			heading += (CaveFeatures.random(this.seed, gridX, gridZ, 10 + i) - 0.5F) * 0.8F;
			x += NoiseUtil.cos(heading) * STEP;
			z += NoiseUtil.sin(heading) * STEP;
		}
		int water = lowest - DEPTH - (int) (CaveFeatures.random(this.seed, gridX, gridZ, 4) * 12.0F);
		if (count < 6 || water < carving.bottom + 12) {
			return NONE;
		}
		float[] keptX = new float[count];
		float[] keptZ = new float[count];
		float[] ys = new float[count];
		System.arraycopy(xs, 0, keptX, 0, count);
		System.arraycopy(zs, 0, keptZ, 0, count);
		float radius = NoiseUtil.lerp(3.5F, 5.5F, CaveFeatures.random(this.seed, gridX, gridZ, 5));
		float minX = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			ys[i] = water;
			minX = Math.min(minX, keptX[i] - radius);
			maxX = Math.max(maxX, keptX[i] + radius);
			minZ = Math.min(minZ, keptZ[i] - radius);
			maxZ = Math.max(maxZ, keptZ[i] + radius);
		}
		return new Path(keptX, ys, keptZ, water, radius, minX, minZ, maxX, maxZ);
	}
}
