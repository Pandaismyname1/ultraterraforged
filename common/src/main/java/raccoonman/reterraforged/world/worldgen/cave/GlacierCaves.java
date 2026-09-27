package raccoonman.reterraforged.world.worldgen.cave;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.landform.SiteCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.terrain.TerrainType;

/**
 * Glacier caves: tunnels walled with blue and packed ice, leading from the floor of glacial valleys and cirques into
 * the mountainside. Each starts in its own cell of a grid, wherever that holds a glacial valley or cirque below a
 * slope.
 */
final class GlacierCaves implements CaveFeatures.Feature {
	private static final float GRID = 192.0F;
	private static final float RADIUS = 3.2F;
	private static final Cave NONE = new Cave(new float[0], new float[0], new float[0], 0.0F, 0.0F, 0.0F, 0.0F);
	private static final BlockState BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState();
	private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();

	private final int seed;
	private final float frequency;
	private final SiteCache<Cave> caves = SiteCache.make();

	record Cave(float[] xs, float[] ys, float[] zs, float minX, float minZ, float maxX, float maxZ) {
	}

	GlacierCaves(int seed, float frequency) {
		this.seed = seed;
		this.frequency = frequency;
	}

	@Override
	public void carve(CaveCarving carving) {
		int gridX = NoiseUtil.floor(carving.minX / GRID);
		int gridZ = NoiseUtil.floor(carving.minZ / GRID);
		CaveCarving.Lining ice = (x, y, z) -> Math.floorMod((x * 73856093) ^ (y * 19349663) ^ (z * 83492791), 3) == 0 ? BLUE_ICE : PACKED_ICE;
		for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
			for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
				if (CaveFeatures.random(this.seed, gx, gz, 0) >= this.frequency) {
					continue;
				}
				Cave cave = this.caves.get(gx, gz, (cx, cz) -> this.find(cx, cz, carving));
				if (cave == NONE || !carving.reaches(cave.minX(), cave.minZ(), cave.maxX(), cave.maxZ())) {
					continue;
				}
				CaveFeatures.along(cave.xs(), cave.ys(), cave.zs(), 1.5F, (x, y, z, index) -> carving.line(x, y, z, RADIUS, RADIUS, RADIUS, 1.6F, ice));
				CaveFeatures.along(cave.xs(), cave.ys(), cave.zs(), 1.5F, (x, y, z, index) -> carving.hollow(x, y, z, RADIUS, RADIUS, RADIUS, CaveCarving.DRY, index < 1.0F ? CaveCarving.OPEN : 3));
			}
		}
	}

	Cave find(int gridX, int gridZ, CaveCarving carving) {
		float x = (gridX + CaveFeatures.random(this.seed, gridX, gridZ, 1)) * GRID;
		float z = (gridZ + CaveFeatures.random(this.seed, gridX, gridZ, 2)) * GRID;
		Cell cell = carving.cell(x, z);
		if (cell.terrain != TerrainType.GLACIAL_VALLEY && cell.terrain != TerrainType.CIRQUE) {
			return NONE;
		}
		int floor = carving.levels.scale(cell.height);
		// into the steepest rise nearby
		float bestRise = 0.0F;
		float dirX = 0.0F;
		float dirZ = 0.0F;
		for (int i = 0; i < 8; i++) {
			float angle = i * NoiseUtil.PI2 / 8.0F;
			float cos = NoiseUtil.cos(angle);
			float sin = NoiseUtil.sin(angle);
			float rise = carving.terrainHeight(x + cos * 16.0F, z + sin * 16.0F) - floor;
			if (rise > bestRise) {
				bestRise = rise;
				dirX = cos;
				dirZ = sin;
			}
		}
		if (bestRise < 8.0F) {
			return NONE;
		}
		float length = NoiseUtil.lerp(20.0F, 40.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3));
		int nodes = 5;
		float[] xs = new float[nodes];
		float[] ys = new float[nodes];
		float[] zs = new float[nodes];
		float wander = (CaveFeatures.random(this.seed, gridX, gridZ, 4) - 0.5F) * 0.8F;
		for (int i = 0; i < nodes; i++) {
			float t = i / (float) (nodes - 1);
			float side = NoiseUtil.sin(t * NoiseUtil.PI2 * 0.5F) * wander * length * 0.3F;
			xs[i] = x + dirX * (t * length - 2.0F) - dirZ * side;
			zs[i] = z + dirZ * (t * length - 2.0F) + dirX * side;
			// rising gently into the mountain, its floor at the valley's
			ys[i] = floor + RADIUS - 0.5F + t * length * 0.1F;
		}
		float reach = RADIUS + 2.0F;
		float minX = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i < nodes; i++) {
			minX = Math.min(minX, xs[i] - reach);
			maxX = Math.max(maxX, xs[i] + reach);
			minZ = Math.min(minZ, zs[i] - reach);
			maxZ = Math.max(maxZ, zs[i] + reach);
		}
		return new Cave(xs, ys, zs, minX, minZ, maxX, maxZ);
	}
}
